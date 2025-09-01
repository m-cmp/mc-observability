package mcmp.mc.observability.o11ymanager.util;

import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.key;

import com.epages.restdocs.apispec.HeaderDescriptorWithType;
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ParameterDescriptorWithType;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

public final class ApiDocumentation {

    private static final String ENUM_TYPE = "ENUM";
    private static final String ENUM_KEY = "enumValues";

    public static HeaderDescriptorWithType header(
            String name, ParameterType type, String description) {
        return headerWithName(name).type(type.toSimpleType()).description(description);
    }

    public static ParameterDescriptorWithType parameter(
            String name, ParameterType type, String description) {
        return parameterWithName(name).type(type.toSimpleType()).description(description);
    }

    public static ParameterDescriptorWithType paramInteger(String name, String description) {
        return parameterWithName(name).type(SimpleType.INTEGER).description(description);
    }

    public static ParameterDescriptorWithType paramNumber(String name, String description) {
        return parameterWithName(name).type(SimpleType.NUMBER).description(description);
    }

    public static ParameterDescriptorWithType paramString(String name, String description) {
        return parameterWithName(name).type(SimpleType.STRING).description(description);
    }

    public static ParameterDescriptorWithType paramBoolean(String name, String description) {
        return parameterWithName(name).type(SimpleType.BOOLEAN).description(description);
    }

    public static FieldDescriptor field(String path, JsonFieldType type, String description) {
        return fieldWithPath(path).type(type).description(description);
    }

    public static FieldDescriptor fieldNumber(String path, String description) {
        return fieldWithPath(path).type(JsonFieldType.NUMBER).description(description);
    }

    public static FieldDescriptor fieldString(String path, String description) {
        return fieldWithPath(path).type(JsonFieldType.STRING).description(description);
    }

    public static FieldDescriptor fieldObject(String path, String description) {
        return fieldWithPath(path).type(JsonFieldType.OBJECT).description(description);
    }

    public static FieldDescriptor fieldBoolean(String path, String description) {
        return fieldWithPath(path).type(JsonFieldType.BOOLEAN).description(description);
    }

    public static FieldDescriptor fieldNull(String path, String description) {
        return fieldWithPath(path).type(JsonFieldType.NULL).description(description);
    }

    public static FieldDescriptor fieldArray(String path, String description) {
        return fieldWithPath(path).type(JsonFieldType.ARRAY).description(description);
    }

    public static FieldDescriptor fieldVaries(String path, String description) {
        return fieldWithPath(path).type(JsonFieldType.VARIES).description(description);
    }

    public static FieldDescriptor fieldEnum(
            String path, String description, Class<? extends Enum<?>> clazz) {
        return fieldWithPath(path)
                .type(ENUM_TYPE)
                .description(description)
                .attributes(key(ENUM_KEY).value(Arrays.asList(clazz.getEnumConstants())));
    }

    public static FieldDescriptor fieldSubsection(
            String path, JsonFieldType type, String description) {
        return subsectionWithPath(path).type(type).description(description);
    }

    public static FieldDescriptor fieldSubsection(String path, String description) {
        return subsectionWithPath(path).type(JsonFieldType.OBJECT).description(description);
    }

    public static ApiDocumentationBuilder builder() {
        return new ApiDocumentationBuilder();
    }

    public static class ApiDocumentationBuilder {

        private String tag;
        private String description;
        private String summary;
        private Schema requestSchema;
        private Schema responseSchema;
        private List<HeaderDescriptorWithType> requestHeaders = Collections.emptyList();
        private List<HeaderDescriptorWithType> responseHeaders = Collections.emptyList();
        private List<ParameterDescriptorWithType> pathParameters = Collections.emptyList();
        private List<ParameterDescriptorWithType> queryParameters = Collections.emptyList();
        private List<FieldDescriptor> requestFields = Collections.emptyList();
        private List<FieldDescriptor> responseFields = Collections.emptyList();

        ApiDocumentationBuilder() {}

        public ApiDocumentationBuilder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public ApiDocumentationBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ApiDocumentationBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public ApiDocumentationBuilder requestSchema(String requestSchema) {
            this.requestSchema = Schema.schema(requestSchema);
            return this;
        }

        public ApiDocumentationBuilder responseSchema(String responseSchema) {
            this.responseSchema = Schema.schema(responseSchema);
            return this;
        }

        public ApiDocumentationBuilder requestHeaders(HeaderDescriptorWithType... requestHeaders) {
            this.requestHeaders = Arrays.asList(requestHeaders);
            return this;
        }

        public ApiDocumentationBuilder responseHeaders(
                HeaderDescriptorWithType... responseHeaders) {
            this.responseHeaders = Arrays.asList(responseHeaders);
            return this;
        }

        public ApiDocumentationBuilder pathParameters(
                ParameterDescriptorWithType... pathParameters) {
            this.pathParameters = Arrays.asList(pathParameters);
            return this;
        }

        public ApiDocumentationBuilder queryParameters(
                ParameterDescriptorWithType... queryParameters) {
            this.queryParameters = Arrays.asList(queryParameters);
            return this;
        }

        public ApiDocumentationBuilder requestFields(FieldDescriptor... requestFields) {
            this.requestFields = Arrays.asList(requestFields);
            return this;
        }

        public ApiDocumentationBuilder responseFields(FieldDescriptor... responseFields) {
            this.responseFields = Arrays.asList(responseFields);
            return this;
        }

        public RestDocumentationResultHandler build() {
            return MockMvcRestDocumentationWrapper.document(
                    "{method-name}",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    resource(
                            ResourceSnippetParameters.builder()
                                    .tag(tag)
                                    .description(description)
                                    .summary(summary)
                                    .requestSchema(requestSchema)
                                    .responseSchema(responseSchema)
                                    .requestHeaders(requestHeaders)
                                    .responseHeaders(responseHeaders)
                                    .pathParameters(pathParameters)
                                    .queryParameters(queryParameters)
                                    .requestFields(requestFields)
                                    .responseFields(responseFields)
                                    .build()));
        }
    }

    public enum ParameterType {
        STRING(SimpleType.STRING),
        INTEGER(SimpleType.INTEGER),
        NUMBER(SimpleType.NUMBER),
        BOOLEAN(SimpleType.BOOLEAN);

        private final SimpleType simpleType;

        ParameterType(SimpleType simpleType) {
            this.simpleType = simpleType;
        }

        private SimpleType toSimpleType() {
            return simpleType;
        }
    }
}
