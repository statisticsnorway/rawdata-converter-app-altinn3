package no.ssb.rawdata.converter.app.altinn3;

import lombok.extern.slf4j.Slf4j;
import no.ssb.avro.convert.xml.XmlToRecords;
import no.ssb.rawdata.api.RawdataMessage;
import no.ssb.rawdata.converter.app.altinn3.schema.Altinn3SchemaAdapter;
import no.ssb.rawdata.converter.app.altinn3.schema.Altinn3Schemas;
import no.ssb.rawdata.converter.core.convert.ConversionResult;
import no.ssb.rawdata.converter.core.convert.ConversionResult.ConversionResultBuilder;
import no.ssb.rawdata.converter.core.convert.RawdataConverter;
import no.ssb.rawdata.converter.core.convert.ValueInterceptorChain;
import no.ssb.rawdata.converter.core.exception.RawdataConverterException;
import no.ssb.rawdata.converter.core.schema.AggregateSchemaBuilder;
import no.ssb.rawdata.converter.core.schema.DcManifestSchemaAdapter;
import no.ssb.rawdata.converter.util.Jq;
import no.ssb.rawdata.converter.util.RawdataMessageAdapter;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecordBuilder;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import static no.ssb.rawdata.converter.util.RawdataMessageAdapter.posAndIdOf;

@Slf4j
public class Altinn3RawdataConverter implements RawdataConverter {

    private static final String RAWDATA_ITEMNAME_ENTRY = "entry";
    private static final String FIELDNAME_DC_MANIFEST = "dcManifest";
    private static final String FIELDNAME_CSV_DATA = "data";

    private final Altinn3RawdataConverterConfig converterConfig;
    private final ValueInterceptorChain valueInterceptorChain;

    private DcManifestSchemaAdapter dcManifestSchemaAdapter;
    private Altinn3SchemaAdapter altinn3SchemaAdapter;
    private Schema targetAvroSchema;

    public Altinn3RawdataConverter(Altinn3RawdataConverterConfig converterConfig, ValueInterceptorChain valueInterceptorChain) {
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


        String entryJson = msg.getItemAsString("entry");
        String altinn3SchemaName = Jq.queryOne(".data[0].dataType", entryJson, String.class).orElseThrow(() ->
          new Altinn3RawdataConverterException("Unable to determine altinn3 schema name from sample rawdata message")
        );

        altinn3SchemaAdapter = Altinn3Schemas.get(altinn3SchemaName);

        String targetNamespace = "dapla.rawdata." + msg.getTopic().orElse("altinn3." + altinn3SchemaAdapter.getName());
        targetAvroSchema = new AggregateSchemaBuilder(targetNamespace)
          .schema(FIELDNAME_DC_MANIFEST, dcManifestSchemaAdapter.getDcManifestSchema())
          .schema(altinn3SchemaAdapter.getName(), altinn3SchemaAdapter.getSchema())
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
            byte[] data = getAltinn3Data(rawdataMessage);
            String dataAsString = new String(data);
            boolean isConvertible = dataAsString.startsWith("<?xml version=\"1.0\"?>");
            if (! isConvertible) {
                log.warn("Skipping RawdataMessage at {} - Invalid altinn3 data: \n{}", posAndIdOf(rawdataMessage), dataAsString);
            }

            return isConvertible;
        }
        catch (Exception e) {
            log.warn("Skipping RawdataMessage at {} - Cause: {}", posAndIdOf(rawdataMessage), e.getMessage());
            return false;
        }
    }

    @Override
    public ConversionResult convert(RawdataMessage rawdataMessage) {
        ConversionResultBuilder resultBuilder = ConversionResult.builder(new GenericRecordBuilder(targetAvroSchema));

        addDcManifest(rawdataMessage, resultBuilder);
        convertAltinn3Data(rawdataMessage, resultBuilder);

        return resultBuilder.build();
    }

    DcManifestSchemaAdapter dcManifestSchemaAdapter() {
        if (dcManifestSchemaAdapter == null) {
            throw new IllegalStateException("dcManifestSchemaAdapter is null. Make sure RawdataConverter#init() was invoked in advance.");
        }

        return dcManifestSchemaAdapter;
    }

    void addDcManifest(RawdataMessage rawdataMessage, ConversionResultBuilder resultBuilder) {
        resultBuilder.withRecord(FIELDNAME_DC_MANIFEST, dcManifestSchemaAdapter().newRecord(rawdataMessage));
    }

    byte[] getAltinn3Data(RawdataMessage rawdataMessage) {
        RawdataMessageAdapter msg = new RawdataMessageAdapter(rawdataMessage);
        RawdataMessageAdapter.ItemMetadata metadata = msg.getAllItemMetadata().values().stream()
          .filter(m -> m.getContentKey().startsWith("file-") && m.isPrintable())
          .findFirst().orElseThrow(() -> new Altinn3RawdataConverterException("Unable to find altinn3 schema payload in RawdataMessage"));

        return rawdataMessage.get(metadata.getContentKey());
    }

    void convertAltinn3Data(RawdataMessage rawdataMessage, ConversionResultBuilder resultBuilder) {
        byte[] data = getAltinn3Data(rawdataMessage);
        try (XmlToRecords records = new XmlToRecords(new ByteArrayInputStream(data), altinn3SchemaAdapter.getRootXmlName(), altinn3SchemaAdapter.getSchema())) {
            records.forEach(record ->
              resultBuilder.withRecord(altinn3SchemaAdapter.getName(), record)
            );
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

}