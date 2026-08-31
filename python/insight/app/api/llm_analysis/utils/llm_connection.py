import os

import httpx
from cryptography.fernet import Fernet, InvalidToken
from fastapi import HTTPException, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.api.llm_analysis.repo.repo import LogAnalysisRepository
from app.api.llm_analysis.request.req import is_official_openai_base_url
from app.api.llm_analysis.response.res import LLMConnection


class LLMConnectionService:
    def __init__(self, db: Session, credential_key: str | None = None, http_client=None):
        self.repo = LogAnalysisRepository(db=db)
        key = credential_key or os.getenv("LLM_CREDENTIAL_KEY")
        try:
            self.cipher = Fernet(key.encode() if isinstance(key, str) else key) if key else None
        except (TypeError, ValueError) as exc:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="LLM_CREDENTIAL_KEY must be a valid Fernet key",
            ) from exc
        self.http_client = http_client

    def create_connection(
        self,
        *,
        name: str,
        provider,
        base_url: str | None,
        api_key: str | None,
        default_model: str | None,
        context_length: int | None = None,
        enabled: bool,
        is_default: bool,
    ):
        if is_default and not default_model:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="default_model is required for the default connection",
            )
        if is_default and not enabled:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="The default LLM connection must be enabled",
            )
        try:
            connection = self.repo.create_connection(
                {
                    "NAME": name,
                    "PROVIDER": getattr(provider, "value", provider),
                    "BASE_URL": base_url,
                    "API_KEY_ENCRYPTED": self._encrypt_api_key(api_key),
                    "DEFAULT_MODEL": default_model,
                    "CONTEXT_LENGTH": context_length,
                    "ENABLED": enabled,
                }
            )
        except IntegrityError as exc:
            self.repo.db.rollback()
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="An LLM connection with this name already exists",
            ) from exc
        if is_default:
            connection = self.repo.set_default_connection(connection.SEQ, default_model)
        return self.map_connection_to_res(connection)

    def get_connections(self):
        return [self.map_connection_to_res(connection) for connection in self.repo.get_all_connections()]

    def get_connection(self, connection_id: int):
        return self.map_connection_to_res(self._get_connection(connection_id))

    def update_connection(self, connection_id: int, body):
        connection = self._get_connection(connection_id)
        fields = body.model_fields_set
        values = body.model_dump()
        provider = values["provider"] or connection.PROVIDER
        base_url = values["base_url"] if "base_url" in fields else connection.BASE_URL
        endpoint_changed = provider != connection.PROVIDER or base_url != connection.BASE_URL
        encrypted_key = connection.API_KEY_ENCRYPTED
        if "api_key" in fields:
            encrypted_key = self._encrypt_api_key(values["api_key"])
        elif endpoint_changed:
            encrypted_key = None
        if provider == "ollama" and not base_url:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="base_url is required for ollama provider",
            )
        if provider == "openai" and is_official_openai_base_url(base_url) and not encrypted_key:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="api_key is required for the default OpenAI endpoint",
            )

        field_map = {
            "name": "NAME",
            "provider": "PROVIDER",
            "base_url": "BASE_URL",
            "default_model": "DEFAULT_MODEL",
            "context_length": "CONTEXT_LENGTH",
            "enabled": "ENABLED",
        }
        updates = {
            column: getattr(values[field], "value", values[field])
            for field, column in field_map.items()
            if field in fields
        }
        if "api_key" in fields or endpoint_changed:
            updates["API_KEY_ENCRYPTED"] = encrypted_key
        if connection.IS_DEFAULT and updates.get("ENABLED") is False:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="The default LLM connection cannot be disabled",
            )
        if connection.IS_DEFAULT and "DEFAULT_MODEL" in updates and not updates["DEFAULT_MODEL"]:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="The default LLM connection must have a default model",
            )
        try:
            updated = self.repo.update_connection(connection, updates)
        except IntegrityError as exc:
            self.repo.db.rollback()
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="An LLM connection with this name already exists",
            ) from exc
        return self.map_connection_to_res(updated)

    def delete_connection(self, connection_id: int):
        connection = self._get_connection(connection_id)
        if connection.IS_DEFAULT:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="The default LLM connection cannot be deleted",
            )
        if self.repo.count_sessions_by_connection(connection_id):
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="The LLM connection is used by existing sessions",
            )
        self.repo.delete_connection(connection)

    def get_models(self, connection_id: int):
        connection = self._get_connection(connection_id)
        if not connection.ENABLED:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="LLM connection is disabled")
        headers = {}
        if connection.PROVIDER == "ollama":
            url = f"{connection.BASE_URL.rstrip('/')}/api/tags"
        else:
            api_key = self.decrypt_api_key(connection)
            if api_key:
                headers["Authorization"] = f"Bearer {api_key}"
            base_url = connection.BASE_URL or "https://api.openai.com/v1"
            url = f"{base_url.rstrip('/')}/models"
        try:
            if self.http_client:
                response = self.http_client.get(url, headers=headers)
            else:
                response = httpx.get(url, headers=headers, timeout=10.0)
            response.raise_for_status()
            payload = response.json()
        except (httpx.HTTPError, ValueError) as exc:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail=f"Failed to fetch models from LLM connection: {exc}",
            ) from exc
        key = "models" if connection.PROVIDER == "ollama" else "data"
        if not isinstance(payload, dict) or not isinstance(payload.get(key), list):
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="Invalid model response from LLM connection",
            )
        items = payload[key]
        if connection.PROVIDER == "ollama":
            return [
                item.get("name") or item.get("model")
                for item in items
                if isinstance(item, dict) and (item.get("name") or item.get("model"))
            ]
        return [item["id"] for item in items if isinstance(item, dict) and item.get("id")]

    def set_default_connection(self, connection_id: int, model_name: str):
        connection = self._get_connection(connection_id)
        if not connection.ENABLED:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="LLM connection is disabled")
        return self.map_connection_to_res(self.repo.set_default_connection(connection_id, model_name))

    def decrypt_api_key(self, connection) -> str | None:
        if not connection.API_KEY_ENCRYPTED:
            return None
        if not self.cipher:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="LLM_CREDENTIAL_KEY is not configured",
            )
        try:
            return self.cipher.decrypt(connection.API_KEY_ENCRYPTED.encode()).decode()
        except InvalidToken as exc:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="The LLM API key cannot be decrypted with LLM_CREDENTIAL_KEY",
            ) from exc

    def _encrypt_api_key(self, api_key: str | None) -> str | None:
        if not api_key:
            return None
        if not self.cipher:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="LLM_CREDENTIAL_KEY is not configured",
            )
        return self.cipher.encrypt(api_key.encode()).decode()

    def _get_connection(self, connection_id: int):
        connection = self.repo.get_connection_by_id(connection_id)
        if not connection:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="LLM connection not found")
        return connection

    @staticmethod
    def map_connection_to_res(connection):
        return LLMConnection(
            id=connection.SEQ,
            name=connection.NAME,
            provider=connection.PROVIDER,
            base_url=connection.BASE_URL,
            api_key_configured=bool(connection.API_KEY_ENCRYPTED),
            default_model=connection.DEFAULT_MODEL,
            context_length=connection.CONTEXT_LENGTH,
            is_default=connection.IS_DEFAULT,
            enabled=connection.ENABLED,
            regdate=connection.REGDATE,
        )
