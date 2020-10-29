package no.ssb.rawdata.converter.app.altinn3;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties("rawdata.converter.app.altinn3")
@Data
public class Altinn3RawdataConverterConfig {

    /**
     * <p>Some config param</p>
     *
     * <p>Defaults to "default value"</p>
     *
     * TODO: Remove this
     */
    private String someParam = "default value";

}