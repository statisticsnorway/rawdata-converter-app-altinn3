package no.ssb.rawdata.converter.app.altinn3.schema;

import com.google.common.base.Functions;
import no.ssb.rawdata.converter.app.altinn3.Altinn3RawdataConverter.Altinn3RawdataConverterException;
import no.ssb.rawdata.converter.util.WordUtil;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.ssb.rawdata.converter.util.AvroSchemaUtil.readAvroSchema;

public class Altinn3Schemas {

    private static final Map<String, Altinn3SchemaAdapter> SCHEMAS;

    static {
        SCHEMAS = Set.of(
          Altinn3SchemaAdapter.builder()
            .name(normalizedNameOf("RA-0678_M"))
            .schema(readAvroSchema("schema/ra-0678_m_v1.2_20150506.avsc"))
            .rootXmlName("RA_0678_M") // TODO: Deduce this from the schema instead?
            .build()
        )
        .stream().collect(Collectors.toMap(s ->
          s.getName(), Functions.identity()));
    }

    public static Altinn3SchemaAdapter get(String schemaName) {
        return Optional.ofNullable(SCHEMAS.get(normalizedNameOf(schemaName)))
          .orElseThrow(() ->
            new Altinn3SchemaNotFoundException("Unknown altinn3 schema: " + schemaName + " (" + normalizedNameOf(schemaName) + ")"));
    }

    private static String normalizedNameOf(String s) {
        return WordUtil.toCamelCase(s);
    }

    public static class Altinn3SchemaNotFoundException extends Altinn3RawdataConverterException {
        public Altinn3SchemaNotFoundException(String msg) {
            super(msg);
        }
    }

}
