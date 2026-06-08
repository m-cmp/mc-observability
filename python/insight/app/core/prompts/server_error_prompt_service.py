from .base_prompt_service import BasePromptService


class ServerErrorPromptService(BasePromptService):
    """Prompt service specialized for HTTP 5xx server error analysis."""

    def get_system_prompt_config(self) -> dict[str, str]:
        """Get HTTP 5xx analysis system prompt configuration."""
        return self.config.get_server_error_system_prompt_config()

    def build_prompt(self, session_id: str, messages: str, msg_count: int = 0) -> list[dict[str, str]]:
        """Build prompt specifically for HTTP 5xx analysis."""
        system_prompt = self._build_system_prompt(msg_count)
        return self._create_prompt_messages(system_prompt, messages)
