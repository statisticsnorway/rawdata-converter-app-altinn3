package no.ssb.rawdata.converter.app.altinn3;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;
import lombok.NonNull;
import no.ssb.rawdata.converter.app.altinn3.schema.SchemaProperties;

@ConfigurationProperties("rawdata.converter.altinn3")
@Data
public class Altinn3RawdataConverterConfig {

    private SchemaPropertiesConfig schemaProps;


    @ConfigurationProperties("schema-props")
    public static class SchemaPropertiesConfig extends SchemaProperties {}

}