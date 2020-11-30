package no.ssb.rawdata.converter.app.altinn3.schema;

import com.google.common.base.CharMatcher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// TODO: Make immutable, use @Value instead of @Data?
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SchemaProperties {

    /**
     * e.g. RA-0678_M
     */
    private String dataType;

    /**
     * e.g. SERES
     */
    private String dataFormatProvider;

    /**
     * e.g. 4664
     */
    private String dataFormatId;

    /**
     * e.g. 38916
     */
    private String dataFormatVersion;

    /**
     * Name of the XML root element of the source document.
     *
     * e.g. RA0678_M
     */
    private String rootElementName;

    /**
     * The name of the data element to populate in the resulting targetAvroSchema.
     *
     * e.g. ra0678m
     */
    private String targetItemName;

    /**
     * lower cased dataType without any special characters such as - or _
     *
     * e.g. ra0678m
     */
    public String getSchemaSlug() {
        return CharMatcher.javaLetterOrDigit().retainFrom(dataType.toLowerCase());
    }

    /**
     * e.g. ra0678m-seres-4664-38916
     */
    public String getVersionedSchemaName() {
        return String.join("-",  getSchemaSlug(), dataFormatProvider, dataFormatId, dataFormatVersion).toLowerCase();
    }

    public boolean matches(String provider, String id, String version) {
        return this.dataFormatProvider.equalsIgnoreCase(provider) &&
          this.dataFormatId.equals(id) &&
          this.dataFormatVersion.equals(version);
    }

}
