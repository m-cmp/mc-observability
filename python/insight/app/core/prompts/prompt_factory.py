from config.ConfigManager import ConfigManager

from .alert_prompt_service import AlertPromptService
from .base_prompt_service import BasePromptService
from .log_prompt_service import LogPromptService


class PromptFactory:
    """Factory for creating appropriate prompt services based on analysis type."""

    @staticmethod
    def create_prompt_service(analysis_type: str, config_manager: ConfigManager = None) -> BasePromptService:
        """
        Create a prompt service instance based on analysis type.

        Args:
            analysis_type: Type of analysis ('log' or 'alert')
            config_manager: Optional configuration manager instance

        Returns:
            Appropriate prompt service instance

        Raises:
            ValueError: If analysis type is not supported
        """
        analysis_type = analysis_type.lower()

        service_map = {"log": LogPromptService, "alert": AlertPromptService}

        service_class = service_map.get(analysis_type)
        if not service_class:
            supported_types = list(service_map.keys())
            raise ValueError(f"Unsupported analysis type: {analysis_type}. Supported types: {supported_types}")

        return service_class(config_manager)
