package no.ssb.rawdata.converter.app.altinn3.schema;

import lombok.Value;
import no.ssb.rawdata.converter.core.schema.AggregateSchemaBuilder;
import no.ssb.rawdata.converter.core.schema.DcManifestSchemaAdapter;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;

@Value
public class ManifestSchemaAdapter {
    private final Schema schema;

    public static final String FIELDNAME_COLLECTOR = "collector";
    public static final String FIELDNAME_CONVERTER = "converter";

    public ManifestSchemaAdapter(DcManifestSchemaAdapter dcManifestSchemaAdapter, ConverterManifestSchemaAdapter converterManifestSchemaAdapter) {
        this.schema = new AggregateSchemaBuilder("dapla.rawdata.manifest")
            .schema(FIELDNAME_COLLECTOR, dcManifestSchemaAdapter.getDcManifestSchema())
            .schema(FIELDNAME_CONVERTER, converterManifestSchemaAdapter.getSchema())
            .build();
    }

    public GenericRecord newRecord(GenericRecord collector, GenericRecord converter) {
        return new GenericRecordBuilder(schema)
          .set(FIELDNAME_COLLECTOR, collector)
          .set(FIELDNAME_CONVERTER, converter)
          .build();
    }

}
