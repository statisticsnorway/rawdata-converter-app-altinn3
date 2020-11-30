package no.ssb.rawdata.converter.app.altinn3;

import avro.shaded.com.google.common.base.Objects;
import com.google.common.base.MoreObjects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ssb.avro.convert.xml.XmlToRecords;
import no.ssb.rawdata.api.RawdataMessage;
import no.ssb.rawdata.converter.app.altinn3.schema.Altinn3DataFormat;
import no.ssb.rawdata.converter.app.altinn3.schema.Altinn3SchemaAdapter;
import no.ssb.rawdata.converter.app.altinn3.schema.Altinn3Schemas;
import no.ssb.rawdata.converter.app.altinn3.schema.ConverterManifestSchemaAdapter;
import no.ssb.rawdata.converter.app.altinn3.schema.ManifestSchemaAdapter;
import no.ssb.rawdata.converter.app.altinn3.schema.SchemaProperties;
import no.ssb.rawdata.converter.core.convert.ConversionResult;
import no.ssb.rawdata.converter.core.convert.ConversionResult.ConversionResultBuilder;
import no.ssb.rawdata.converter.core.convert.RawdataConverter;
import no.ssb.rawdata.converter.core.convert.ValueInterceptorChain;
import no.ssb.rawdata.converter.core.exception.RawdataConverterException;
import no.ssb.rawdata.converter.core.schema.AggregateSchemaBuilder;
import no.ssb.rawdata.converter.core.schema.DcManifestSchemaAdapter;
import no.ssb.rawdata.converter.util.Jq;
import no.ssb.rawdata.converter.util.RawdataMessageAdapter;
import no.ssb.rawdata.converter.util.Xml;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static no.ssb.rawdata.converter.util.RawdataMessageAdapter.posAndIdOf;

@Slf4j
public class Altinn3RawdataConverter implements RawdataConverter {

    private static final String FIELDNAME_MANIFEST = "manifest";

    private final Altinn3RawdataConverterConfig converterConfig;
    private final ValueInterceptorChain valueInterceptorChain;

    private ManifestSchemaAdapter manifestSchemaAdapter;
    private DcManifestSchemaAdapter dcManifestSchemaAdapter;
    private ConverterManifestSchemaAdapter converterManifestSchemaAdapter;
    private Altinn3SchemaAdapter altinn3SchemaAdapter;
    private Schema targetAvroSchema;

    public Altinn3RawdataConverter(@NonNull Altinn3RawdataConverterConfig converterConfig, ValueInterceptorChain valueInterceptorChain) {
        this.converterConfig = converterConfig;
        this.valueInterceptorChain = valueInterceptorChain;
    }

    @Override
    public void init(Collection<RawdataMessage> sampleRawdataMessages) {
        log.info("Determine target avro schema from {}", sampleRawdataMessages);
        RawdataMessage sample = sampleRawdataMessages.stream()
          .findFirst()
          .orElseThrow(() ->
            new Altinn3RawdataConverterException("Unable to determine target avro schema since no sample rawdata messages were supplied. Make sure to configure `converter-settings.rawdata-samples`")
          );

        RawdataMessageAdapter msg = new RawdataMessageAdapter(sample);
        dcManifestSchemaAdapter = DcManifestSchemaAdapter.of(sample);
        converterManifestSchemaAdapter = new ConverterManifestSchemaAdapter();
        manifestSchemaAdapter = new ManifestSchemaAdapter(dcManifestSchemaAdapter, converterManifestSchemaAdapter);
        altinn3SchemaAdapter = Altinn3Schemas.getBySchemaDescriptor(converterConfig.getSchemaProps());

        String targetNamespace = "dapla.rawdata." + msg.getTopic().orElse("altinn3." + altinn3SchemaAdapter.getProps().getSchemaSlug());
        targetAvroSchema = new AggregateSchemaBuilder(targetNamespace)
          .schema(FIELDNAME_MANIFEST, manifestSchemaAdapter.getSchema())
          .schema(altinn3SchemaAdapter.getProps().getTargetItemName(), altinn3SchemaAdapter.getSchema())
          .build();
    }

    @Override
    public Schema targetAvroSchema() {
        if (targetAvroSchema == null) {
            throw new IllegalStateException("targetAvroSchema is null. Make sure RawdataConverter#init() was invoked in advance.");
        }

        return targetAvroSchema;
    }

    @Override
    public boolean isConvertible(RawdataMessage rawdataMessage) {
        try {
            byte[] data = altinn3DataOf(rawdataMessage);
            String xml = new String(data);

            validateThatRawdataIsProperlyFormatted(xml);
            validateSchemaCompatibility(xml);

            return true;
        }
        catch (IncompatibleRawdataMessageException e) {
            log.info("Skipping RawdataMessage at {} - Cause: {}", posAndIdOf(rawdataMessage), e.getMessage());
        }
        catch (NoAltinn3DataFoundException e) {
            log.warn("Skipping RawdataMessage at {} - Cause: {}", posAndIdOf(rawdataMessage), e.getMessage());
        }
        catch (Exception e) {
            log.warn("Skipping RawdataMessage at " + posAndIdOf(rawdataMessage), e);
        }

        return false;
    }

    void validateThatRawdataIsProperlyFormatted(String xml) throws IncompatibleRawdataMessageException {
        if (! xml.startsWith("<?xml version=\"1.0\"?>")) {
            throw new IncompatibleRawdataMessageException("Invalid rawdata contents: " + xml);
        }
    }

    void validateSchemaCompatibility(String xml) throws IncompatibleRawdataMessageException {
        Altinn3DataFormat msgDataFormat = altinn3DataFormatOf(xml);
        SchemaProperties schema = converterConfig.getSchemaProps();

        if (! msgDataFormat.matches(schema.getDataFormatProvider(), schema.getDataFormatId(), schema.getDataFormatVersion())) {
            throw new IncompatibleRawdataMessageException(
              String.format("RawdataMessage is not schema compatible with %s - msg.provider=%s, msg.id=%s, msg.version=%s",
                converterConfig.getSchemaProps().getVersionedSchemaName(), msgDataFormat.getProvider(), msgDataFormat.getId(), msgDataFormat.getVersion()
              ));
        }
    }

    @Override
    public ConversionResult convert(RawdataMessage rawdataMessage) {
        ConversionResultBuilder resultBuilder = ConversionResult.builder(targetAvroSchema, rawdataMessage);

        addManifest(rawdataMessage, resultBuilder);
        convertAltinn3Data(rawdataMessage, resultBuilder);

        return resultBuilder.build();
    }

    DcManifestSchemaAdapter dcManifestSchemaAdapter() {
        if (dcManifestSchemaAdapter == null) {
            throw new IllegalStateException("dcManifestSchemaAdapter is null. Make sure RawdataConverter#init() was invoked in advance.");
        }

        return dcManifestSchemaAdapter;
    }

    void addManifest(RawdataMessage rawdataMessage, ConversionResultBuilder resultBuilder) {
        GenericRecord manifest = new GenericRecordBuilder(manifestSchemaAdapter.getSchema())
          .set(ManifestSchemaAdapter.FIELDNAME_COLLECTOR, dcManifestSchemaAdapter().newRecord(rawdataMessage, valueInterceptorChain))
          .set(ManifestSchemaAdapter.FIELDNAME_CONVERTER, converterManifestData())
          .build();

        resultBuilder.withRecord(FIELDNAME_MANIFEST, manifest);
    }

    GenericRecord converterManifestData() {
        Map<String, String> schemaInfo = Map.of(
          altinn3SchemaAdapter.getProps().getTargetItemName(),
          altinn3SchemaAdapter.getProps().getVersionedSchemaName()
        );

        return new GenericRecordBuilder(converterManifestSchemaAdapter.getSchema())
          .set(ConverterManifestSchemaAdapter.FIELDNAME_SCHEMAS, schemaInfo)
          .build();
    }

    byte[] altinn3DataOf(RawdataMessage rawdataMessage) {
        RawdataMessageAdapter msg = new RawdataMessageAdapter(rawdataMessage);
        RawdataMessageAdapter.ItemMetadata metadata = msg.getAllItemMetadata().values().stream()
          .filter(m -> m.getContentKey().startsWith("file-") && m.isPrintable())
          .findFirst().orElseThrow(() -> new NoAltinn3DataFoundException("Unable to find altinn3 schema payload in RawdataMessage"));

        return rawdataMessage.get(metadata.getContentKey());
    }

    Altinn3DataFormat altinn3DataFormatOf(String xml) {
        Map<String, Object> xmlMap = Xml.toGenericMap(xml);
        String dataFormatProvider = (String) xmlMap.getOrDefault("dataFormatProvider", "unknown-provider");
        String dataFormatId = (String) xmlMap.getOrDefault("dataFormatId", "unknown-id");
        String dataFormatVersion = (String) xmlMap.getOrDefault("dataFormatVersion", "unknown-version");

        return new Altinn3DataFormat(dataFormatProvider, dataFormatId, dataFormatVersion);
    }

    Map<String, String> entryDataMapOf(RawdataMessage rawdataMessage) {
        RawdataMessageAdapter msg = new RawdataMessageAdapter(rawdataMessage);
        String entryJson = msg.findItemAsString("entry").orElse(null);
        if (entryJson == null) {
            return Map.of();
        }

        String archiveReference = Jq.queryOne(".data[0].instanceGuid", entryJson, String.class).orElse("");
        String archiveTimeStamp = Jq.queryOne(".status.archived", entryJson, String.class).orElse("");
        String organisationNumber = Jq.queryOne(".instanceOwner.organisationNumber", entryJson, String.class).orElse(null);
        String personNumber = Jq.queryOne(".instanceOwner.personNumber", entryJson, String.class).orElse(null);
        String reportee = Optional.ofNullable(organisationNumber)
          .orElseGet(() -> Optional.ofNullable(personNumber)
            .orElse(""));

        return Map.of(
          "archiveReference", archiveReference,
          "archiveTimeStamp", archiveTimeStamp,
          "reportee", reportee
        );
    }

    void convertAltinn3Data(RawdataMessage rawdataMessage, ConversionResultBuilder resultBuilder) {
        byte[] data = altinn3DataOf(rawdataMessage);
        Altinn3DataFormat dataFormat = altinn3DataFormatOf(new String(data));
        try (XmlToRecords records = new XmlToRecords(new ByteArrayInputStream(data), altinn3SchemaAdapter.getProps().getRootElementName(), altinn3SchemaAdapter.getSchema())) {
            records.forEach(record -> {
                record.put("_dataFormatProvider", dataFormat.getProvider());
                record.put("_dataFormatId", dataFormat.getId());
                record.put("_dataFormatVersion", dataFormat.getVersion());
                record.put("others", entryDataMapOf(rawdataMessage));
                resultBuilder.withRecord(altinn3SchemaAdapter.getProps().getTargetItemName(), record);
            });
        }
        catch (Exception e) {
            throw new Altinn3RawdataConverterException("Error converting altinn3 document data at " + posAndIdOf(rawdataMessage), e);
        }
    }

    public static class Altinn3RawdataConverterException extends RawdataConverterException {
        public Altinn3RawdataConverterException(String msg) {
            super(msg);
        }
        public Altinn3RawdataConverterException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class IncompatibleRawdataMessageException extends Exception {
        public IncompatibleRawdataMessageException(String msg) {
            super(msg);
        }
    }

    public static class NoAltinn3DataFoundException extends Altinn3RawdataConverterException {
        public NoAltinn3DataFoundException(String msg) {
            super(msg);
        }
    }

}