package no.ssb.rawdata.converter.app.altinn3.schema;

import lombok.NonNull;
import lombok.Value;

@Value
public class Altinn3DataFormat {
    @NonNull
    private final String provider;
    @NonNull
    private final String id;
    @NonNull
    private final String version;

    @Override
    public String toString() {
        return String.join("-", provider, id, version).toLowerCase();
    }

    public boolean matches(String dataFormatProvider, String dataFormatId, String dataFormatVersion) {
        return this.provider.equalsIgnoreCase(dataFormatProvider) &&
          this.id.equals(dataFormatId) &&
          this.version.equals(dataFormatVersion);
    }
}
