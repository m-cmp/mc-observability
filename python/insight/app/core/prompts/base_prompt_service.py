from abc import ABC, abstractmethod
from datetime import UTC, datetime
from typing import List, Dict
from config.ConfigManager import ConfigManager


class BasePromptService(ABC):
    """Base abstract class for prompt services."""

    def __init__(self, config_manager: ConfigManager = None):
        self.config = config_manager or ConfigManager()

    @abstractmethod
    def get_system_prompt_config(self) -> Dict[str, str]:
        """Get system prompt configuration for the specific analysis type."""
        pass

    @abstractmethod
    def build_prompt(self, session_id: str, messages: str, msg_count: int = 0) -> List[Dict[str, str]]:
        """Build the complete prompt for the analysis."""
        pass

    @staticmethod
    def _get_current_time() -> str:
        """Get current time in UTC format."""
        return datetime.now(UTC).strftime("%Y-%m-%dT%H:%M:%SZ")

    def _build_system_prompt(self, msg_count: int = 0) -> str:
        """Build system prompt based on message count."""
        current_time = self._get_current_time()
        prompt_config = self.get_system_prompt_config()

        if msg_count == 0:
            system_prompt_template = prompt_config.get("system_prompt_first", "")
        else:
            system_prompt_template = prompt_config.get("system_prompt_default", "")

        return system_prompt_template.format(current_time=current_time)

    @staticmethod
    def _create_prompt_messages(system_prompt: str, user_message: str) -> List[Dict[str, str]]:
        """Create the final prompt message structure."""
        return [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_message}
        ]
