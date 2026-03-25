#!/usr/bin/env python3
"""
Extract OpenAPI spec from Python FastAPI insight module and convert to Swagger 2.0.
Must be run from python/insight/ directory with uv.
"""

import json
import sys
from pathlib import Path

import yaml
from main import app


def openapi3_to_swagger2(spec):
    """Convert OpenAPI 3.x spec dict to Swagger 2.0 spec dict."""
    swagger = {
        "swagger": "2.0",
        "info": spec.get("info", {}),
        "basePath": "/",
        "consumes": ["application/json"],
        "produces": ["application/json"],
    }

    paths = {}
    for path, methods in spec.get("paths", {}).items():
        converted_methods = {}
        for method, operation in methods.items():
            if method in ("get", "post", "put", "delete", "patch"):
                converted_methods[method] = convert_operation(operation)
        paths[path] = converted_methods
    swagger["paths"] = paths

    components = spec.get("components", {})
    schemas = components.get("schemas", {})
    if schemas:
        swagger["definitions"] = convert_schemas(schemas)

    return swagger


def convert_operation(op):
    result = {}

    for key in ("tags", "summary", "description", "operationId"):
        if key in op:
            result[key] = op[key]

    result["produces"] = ["application/json"]

    params = []
    for p in op.get("parameters", []):
        param = {
            "name": p.get("name"),
            "in": p.get("in"),
            "description": p.get("description", ""),
            "required": p.get("required", False),
        }
        schema = p.get("schema", {})

        # Handle anyOf (nullable types in OpenAPI 3.1)
        if "anyOf" in schema:
            for variant in schema["anyOf"]:
                if variant.get("type") != "null":
                    param["type"] = variant.get("type", "string")
                    if "format" in variant:
                        param["format"] = variant["format"]
                    if "enum" in variant:
                        param["enum"] = variant["enum"]
                    break
        else:
            param["type"] = schema.get("type", "string")
            if "format" in schema:
                param["format"] = schema["format"]
            if "enum" in schema:
                param["enum"] = schema["enum"]

        if "default" in schema:
            param["default"] = schema["default"]
        params.append(param)

    request_body = op.get("requestBody")
    if request_body:
        content = request_body.get("content", {})
        for content_type, media in content.items():
            body_param = {
                "in": "body",
                "name": "",
                "required": request_body.get("required", False),
            }
            if "schema" in media:
                body_param["schema"] = convert_ref(media["schema"])
            params.append(body_param)
            result["consumes"] = [content_type]
            break

    if params:
        result["parameters"] = params

    responses = {}
    for code, resp in op.get("responses", {}).items():
        converted = {"description": ""}
        resp_content = resp.get("content")
        if resp_content:
            for ct, media in resp_content.items():
                if "schema" in media:
                    converted["schema"] = convert_ref(media["schema"])
                break
        responses[code] = converted
    result["responses"] = responses

    return result


def convert_ref(schema):
    if not isinstance(schema, dict):
        return schema

    result = {}
    for k, v in schema.items():
        if k == "$ref" and isinstance(v, str):
            result[k] = v.replace("#/components/schemas/", "#/definitions/")
        elif k in ("items", "additionalProperties"):
            result[k] = convert_ref(v)
        elif k == "properties" and isinstance(v, dict):
            result[k] = {pk: convert_ref(pv) for pk, pv in v.items()}
        elif k in ("allOf", "oneOf", "anyOf") and isinstance(v, list):
            result[k] = [convert_ref(item) for item in v]
        else:
            result[k] = v

    return result


def convert_schemas(schemas):
    return {name: convert_ref(schema) for name, schema in schemas.items()}


def main():
    schema = app.openapi()
    swagger2 = openapi3_to_swagger2(schema)

    output_path = Path("../../swagger/python-swagger.yaml")
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with open(output_path, "w") as f:
        yaml.dump(
            swagger2,
            f,
            default_flow_style=False,
            allow_unicode=True,
            sort_keys=False,
            width=120,
        )

    print(f"Python Swagger 2.0 written to {output_path}")
    print(f"  Paths: {len(swagger2.get('paths', {}))}")
    print(f"  Definitions: {len(swagger2.get('definitions', {}))}")


if __name__ == "__main__":
    main()
