from .base_prompt_service import BasePromptService


class LogPromptService(BasePromptService):
    """Prompt service specialized for log analysis."""

    def get_system_prompt_config(self) -> dict[str, str]:
        """Get log analysis system prompt configuration."""
        return self.config.get_log_system_prompt_config()

    def build_prompt(self, session_id: str, messages: str, msg_count: int = 0) -> list[dict[str, str]]:
        """Build prompt specifically for log analysis."""
        system_prompt = self._build_system_prompt(msg_count)
        return self._create_prompt_messages(system_prompt, messages)
