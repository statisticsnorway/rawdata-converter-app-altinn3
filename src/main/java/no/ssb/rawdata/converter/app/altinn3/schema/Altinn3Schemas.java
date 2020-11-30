package no.ssb.rawdata.converter.app.altinn3.schema;

import no.ssb.rawdata.converter.app.altinn3.Altinn3RawdataConverter.Altinn3RawdataConverterException;

import java.util.Set;

public class Altinn3Schemas {

    private static final Set<Altinn3SchemaAdapter> SCHEMAS;

    static {
        SCHEMAS = Set.of(
          Altinn3SchemaAdapter.of(SchemaProperties.builder()
            .dataType("RA-0678_M")
            .dataFormatProvider("SERES")
            .dataFormatId("4664")
            .dataFormatVersion("38916")
            .rootElementName("RA0678_M")
            .targetItemName("ra0678m")
          ),
          Altinn3SchemaAdapter.of(SchemaProperties.builder()
            .dataType("RA-0678_M")
            .dataFormatProvider("SERES")
            .dataFormatId("4664")
            .dataFormatVersion("45918")
            .rootElementName("RA0678_M")
            .targetItemName("ra0678m")
          ),
          Altinn3SchemaAdapter.of(SchemaProperties.builder()
            .dataType("RA-0800")
            .dataFormatProvider("SERES")
            .dataFormatId("3983")
            .dataFormatVersion("40674")
            .rootElementName("RAvedlegg_M")
            .targetItemName("ra0800")
          )
        );
    }

    public static Altinn3SchemaAdapter getBySchemaDescriptor(SchemaProperties props) {
        Altinn3SchemaAdapter schemaAdapter = SCHEMAS.stream()
          .filter(schema -> schema.getProps().getVersionedSchemaName().equals(props.getVersionedSchemaName()))
          .findFirst()
          .orElseThrow(() ->
            new SchemaNotFoundException("No schema found for " + props.getVersionedSchemaName()));
        return merge(schemaAdapter, props);
    }

    private static Altinn3SchemaAdapter merge(Altinn3SchemaAdapter schemaAdapter, SchemaProperties overrides) {
        SchemaProperties.SchemaPropertiesBuilder propsBuilder = schemaAdapter.getProps().toBuilder();

        if (overrides.getTargetItemName() != null) {
            propsBuilder.targetItemName(overrides.getTargetItemName());
        }
        if (overrides.getRootElementName() != null) {
            propsBuilder.rootElementName(overrides.getRootElementName());
        }

        return schemaAdapter.toBuilder()
          .props(propsBuilder.build())
          .build();
    }


    public static class SchemaNotFoundException extends Altinn3RawdataConverterException {
        public SchemaNotFoundException(String msg) {
            super(msg);
        }
    }

}
