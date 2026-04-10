package com.mcmp.o11ymanager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("swaggergen")
class SwaggerGeneratorTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ConnectionFactory connectionFactory;

    @Test
    void generateSwaggerYaml() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs")).andReturn();

        String json = result.getResponse().getContentAsString();

        ObjectMapper jsonMapper = new ObjectMapper();
        Map<String, Object> openapi3 =
                jsonMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});

        Map<String, Object> swagger2 = convertToSwagger2(openapi3);

        Path outputDir = Path.of("../../swagger");
        Files.createDirectories(outputDir);

        YAMLFactory yamlFactory =
                new YAMLFactory()
                        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        ObjectMapper yamlMapper = new ObjectMapper(yamlFactory);

        try (FileWriter writer = new FileWriter(outputDir.resolve("java-swagger.yaml").toFile())) {
            yamlMapper.writerWithDefaultPrettyPrinter().writeValue(writer, swagger2);
        }

        System.out.println("Swagger 2.0 YAML generated at swagger/java-swagger.yaml");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToSwagger2(Map<String, Object> openapi3) throws Exception {
        Map<String, Object> swagger2 = new LinkedHashMap<>();

        swagger2.put("swagger", "2.0");

        Map<String, Object> info = (Map<String, Object>) openapi3.getOrDefault("info", Map.of());
        swagger2.put("info", info);

        List<Map<String, Object>> servers =
                (List<Map<String, Object>>) openapi3.getOrDefault("servers", List.of());
        if (!servers.isEmpty()) {
            String url = (String) servers.get(0).getOrDefault("url", "localhost");
            url = url.replaceFirst("https?://", "");
            swagger2.put("host", url);
        }

        swagger2.put("basePath", "/");
        swagger2.put("consumes", List.of("application/json"));
        swagger2.put("produces", List.of("application/json"));

        Map<String, Object> paths = (Map<String, Object>) openapi3.getOrDefault("paths", Map.of());
        Map<String, Object> swagger2Paths = new LinkedHashMap<>();

        for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
            Map<String, Object> methods = (Map<String, Object>) pathEntry.getValue();
            Map<String, Object> swagger2Methods = new LinkedHashMap<>();

            for (Map.Entry<String, Object> methodEntry : methods.entrySet()) {
                Map<String, Object> operation = (Map<String, Object>) methodEntry.getValue();
                Map<String, Object> swagger2Op = convertOperation(operation);
                swagger2Methods.put(methodEntry.getKey(), swagger2Op);
            }
            swagger2Paths.put(pathEntry.getKey(), swagger2Methods);
        }
        swagger2.put("paths", swagger2Paths);

        Map<String, Object> components =
                (Map<String, Object>) openapi3.getOrDefault("components", Map.of());
        Map<String, Object> schemas =
                (Map<String, Object>) components.getOrDefault("schemas", Map.of());
        if (!schemas.isEmpty()) {
            swagger2.put("definitions", convertAllSchemaRefs(schemas));
        }

        return swagger2;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertOperation(Map<String, Object> operation) throws Exception {
        Map<String, Object> op = new LinkedHashMap<>();

        if (operation.containsKey("tags")) op.put("tags", operation.get("tags"));
        if (operation.containsKey("summary")) op.put("summary", operation.get("summary"));
        if (operation.containsKey("description"))
            op.put("description", operation.get("description"));
        if (operation.containsKey("operationId"))
            op.put("operationId", operation.get("operationId"));

        op.put("produces", List.of("application/json"));

        List<Map<String, Object>> params = new ArrayList<>();

        List<Map<String, Object>> oa3Params =
                (List<Map<String, Object>>) operation.getOrDefault("parameters", List.of());
        for (Map<String, Object> p : oa3Params) {
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("name", p.get("name"));
            param.put("in", p.get("in"));
            if (p.containsKey("description")) param.put("description", p.get("description"));
            param.put("required", p.getOrDefault("required", false));

            Map<String, Object> schema = (Map<String, Object>) p.getOrDefault("schema", Map.of());
            String type = (String) schema.getOrDefault("type", "string");
            param.put("type", type);
            if (schema.containsKey("format")) param.put("format", schema.get("format"));

            params.add(param);
        }

        Map<String, Object> requestBody = (Map<String, Object>) operation.get("requestBody");
        if (requestBody != null) {
            Map<String, Object> content =
                    (Map<String, Object>) requestBody.getOrDefault("content", Map.of());
            for (Map.Entry<String, Object> contentEntry : content.entrySet()) {
                Map<String, Object> mediaType = (Map<String, Object>) contentEntry.getValue();
                Map<String, Object> bodyParam = new LinkedHashMap<>();
                bodyParam.put("in", "body");
                bodyParam.put("name", "");
                bodyParam.put("required", requestBody.getOrDefault("required", false));
                if (mediaType.containsKey("schema")) {
                    bodyParam.put("schema", convertSchemaRef(mediaType.get("schema")));
                }
                params.add(bodyParam);
                op.put("consumes", List.of(contentEntry.getKey()));
                break;
            }
        }

        if (!params.isEmpty()) op.put("parameters", params);

        Map<String, Object> responses =
                (Map<String, Object>) operation.getOrDefault("responses", Map.of());
        Map<String, Object> swagger2Responses = new LinkedHashMap<>();
        for (Map.Entry<String, Object> respEntry : responses.entrySet()) {
            Map<String, Object> resp = (Map<String, Object>) respEntry.getValue();
            Map<String, Object> swagger2Resp = new LinkedHashMap<>();
            swagger2Resp.put("description", "");

            Map<String, Object> respContent = (Map<String, Object>) resp.get("content");
            if (respContent != null) {
                for (Map.Entry<String, Object> ct : respContent.entrySet()) {
                    Map<String, Object> mt = (Map<String, Object>) ct.getValue();
                    if (mt.containsKey("example")) {
                        swagger2Resp.put("examples", Map.of("application/json", mt.get("example")));
                    }
                    if (mt.containsKey("schema")) {
                        swagger2Resp.put("schema", convertSchemaRef(mt.get("schema")));
                    }
                    break;
                }
            }
            swagger2Responses.put(respEntry.getKey(), swagger2Resp);
        }
        op.put("responses", swagger2Responses);

        return op;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertAllSchemaRefs(Map<String, Object> schemas) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : schemas.entrySet()) {
            result.put(entry.getKey(), convertSchemaRef(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object convertSchemaRef(Object schema) {
        if (schema instanceof Map) {
            Map<String, Object> s = (Map<String, Object>) schema;
            Map<String, Object> result = new LinkedHashMap<>(s);
            if (result.containsKey("$ref")) {
                String ref = (String) result.get("$ref");
                result.put("$ref", ref.replace("#/components/schemas/", "#/definitions/"));
            }
            if (result.containsKey("items")) {
                result.put("items", convertSchemaRef(result.get("items")));
            }
            if (result.containsKey("properties")) {
                Map<String, Object> props = (Map<String, Object>) result.get("properties");
                Map<String, Object> newProps = new LinkedHashMap<>();
                for (Map.Entry<String, Object> e : props.entrySet()) {
                    newProps.put(e.getKey(), convertSchemaRef(e.getValue()));
                }
                result.put("properties", newProps);
            }
            if (result.containsKey("allOf")) {
                List<Object> allOf = (List<Object>) result.get("allOf");
                List<Object> newAllOf = new ArrayList<>();
                for (Object item : allOf) {
                    newAllOf.add(convertSchemaRef(item));
                }
                result.put("allOf", newAllOf);
            }
            return result;
        }
        return schema;
    }
}
