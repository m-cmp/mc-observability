import logging

import tiktoken

logger = logging.getLogger(__name__)


def count_tokens(text: str, model_name: str = "gpt-4") -> int:
    try:
        if "gpt-4o" in model_name:
            encoding_name = "o200k_base"
        elif "gpt-4" in model_name or "gpt-3.5" in model_name:
            encoding_name = "cl100k_base"
        else:
            encoding_name = "cl100k_base"

        encoding = tiktoken.get_encoding(encoding_name)
        return len(encoding.encode(str(text)))
    except Exception as exc:
        logger.warning(
            "Error counting tokens: %s. Using character-based estimation.",
            exc,
        )
        return len(str(text)) // 4
