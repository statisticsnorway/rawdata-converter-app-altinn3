package no.ssb.rawdata.converter.app.altinn3.schema;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Value;
import no.ssb.rawdata.converter.util.AvroSchemaUtil;
import org.apache.avro.Schema;

@Value
@Builder(toBuilder = true)
public class Altinn3SchemaAdapter {

    public static Altinn3SchemaAdapter of(SchemaProperties.SchemaPropertiesBuilder propertiesBuilder) {
        return Altinn3SchemaAdapter.of(propertiesBuilder.build());
    }

    public static Altinn3SchemaAdapter of(SchemaProperties props) {
        return new Altinn3SchemaAdapter(
          props,
          AvroSchemaUtil.readAvroSchema("schema/" + props.getVersionedSchemaName() + ".avsc")
        );
    }

    @NonNull
    private final SchemaProperties props;

    @NonNull
    private final Schema schema;

}
