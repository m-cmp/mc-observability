import logging

from sqlalchemy import text

from app.core.dependencies.db import engine

logger = logging.getLogger(__name__)


def run_startup_migrations() -> None:
    """Apply idempotent schema reconciliations at application startup.

    The image ships without a dedicated migration tool, so lightweight, idempotent
    DDL fixes live here and run on every boot (baked into the built image). Each fix
    must be safe to run repeatedly and on an already-correct schema.
    """
    try:
        with engine.begin() as conn:
            # LLM identifiers such as "deepseek-v4-pro:cloud" exceed the original
            # MODEL_NAME VARCHAR(20); widen to match the ORM model (String(100)).
            _ensure_min_varchar_length(conn, "mc_o11y_insight_chat_session", "MODEL_NAME", 100)
    except Exception:
        logger.exception("Startup DB migration failed")


def _ensure_min_varchar_length(conn, table: str, column: str, min_length: int) -> None:
    """Widen a VARCHAR column to at least min_length if it is currently smaller."""
    row = conn.execute(
        text(
            "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns "
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :t AND COLUMN_NAME = :c"
        ),
        {"t": table, "c": column},
    ).first()
    if row is None:
        return  # table/column not present yet; nothing to reconcile
    current_length = row[0]
    if current_length is not None and current_length >= min_length:
        return
    conn.execute(text(f"ALTER TABLE `{table}` MODIFY COLUMN `{column}` VARCHAR({min_length}) NOT NULL"))
    logger.info(
        "DB migration: widened %s.%s to VARCHAR(%d) (was VARCHAR(%s))",
        table,
        column,
        min_length,
        current_length,
    )
