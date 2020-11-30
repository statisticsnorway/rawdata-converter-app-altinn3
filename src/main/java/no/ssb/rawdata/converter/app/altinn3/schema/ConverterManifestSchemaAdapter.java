package no.ssb.rawdata.converter.app.altinn3.schema;

import lombok.Value;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

@Value
public class ConverterManifestSchemaAdapter {
    public static final String FIELDNAME_SCHEMAS = "schemas";

    private final Schema schema;

    public ConverterManifestSchemaAdapter() {
        schema = SchemaBuilder.record("converter")
          .fields()
          .name(FIELDNAME_SCHEMAS).type(Schema.createMap(SchemaBuilder.builder().stringType())).noDefault()
          .endRecord();
    }
}
