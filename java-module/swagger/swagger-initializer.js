window.onload = function () {
    //<editor-fold desc="Changeable Configuration Block">

    // the following lines will be replaced by docker/configurator, when it runs in a docker-container
    window.ui = SwaggerUIBundle({
        urls: [
            {url: "./swagger.yaml", name: "MC-Observability Swagger (OpenAPI 2.0)"},
            {url: "./MC-Observability Swagger(OpenAPI 2.0).yaml", name: "Manager Swagger (OpenAPI 2.0)"},
            {url: "./MC-Observability Swagger(OpenAPI 3.0).yaml", name: "Manager Swagger (OpenAPI 3.0)"}
        ],
        dom_id: '#swagger-ui',
        deepLinking: true,
        presets: [
            SwaggerUIBundle.presets.apis,
            SwaggerUIStandalonePreset
        ],
        plugins: [
            SwaggerUIBundle.plugins.DownloadUrl
        ],
        layout: "StandaloneLayout"
    });

    //</editor-fold>
};
