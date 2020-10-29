package no.ssb.rawdata.converter.app.altinn3.schema;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.avro.Schema;

@Value
@Builder
public class Altinn3SchemaAdapter {

    @NonNull
    private final String name;

    @NonNull
    private final Schema schema;

    @NonNull
    private final String rootXmlName;

}
