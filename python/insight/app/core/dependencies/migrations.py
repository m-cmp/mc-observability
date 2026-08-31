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
    with engine.begin() as conn:
        _ensure_llm_connection_schema(conn)
        _ensure_min_varchar_length(conn, "mc_o11y_insight_chat_session", "MODEL_NAME", 255)
        _ensure_rca_analysis_identity(conn)
        _ensure_rca_schedule_schema(conn)


def _ensure_rca_schedule_schema(conn) -> None:
    conn.execute(
        text(
            "CREATE TABLE IF NOT EXISTS `mc_o11y_insight_rca_schedule` ("
            "`ID` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,"
            "`NAME` VARCHAR(100) NOT NULL,"
            "`ENABLED` BOOLEAN NOT NULL DEFAULT 1,"
            "`INTERVAL_MINUTES` INT(10) UNSIGNED NOT NULL,"
            "`REQUEST_JSON` JSON NOT NULL,"
            "`STATUS` VARCHAR(20) NOT NULL DEFAULT 'IDLE',"
            "`LAST_EXECUTION` TIMESTAMP NULL DEFAULT NULL,"
            "`NEXT_EXECUTION` TIMESTAMP NULL DEFAULT NULL,"
            "`LAST_ANALYSIS_ID` BIGINT(20) UNSIGNED NULL DEFAULT NULL,"
            "`LAST_ERROR` TEXT NULL,"
            "`CREATED_AT` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),"
            "`UPDATED_AT` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),"
            "PRIMARY KEY (`ID`),"
            "KEY `ix_rca_schedule_due` (`ENABLED`, `NEXT_EXECUTION`, `STATUS`)"
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        )
    )
    _ensure_column(
        conn,
        "mc_o11y_insight_rca_schedule",
        "TRIGGER_TYPE",
        "`TRIGGER_TYPE` VARCHAR(32) NULL DEFAULT NULL AFTER `INTERVAL_MINUTES`",
    )


def _ensure_llm_connection_schema(conn) -> None:
    conn.execute(
        text(
            "CREATE TABLE IF NOT EXISTS `mc_o11y_insight_llm_connection` ("
            "`SEQ` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,"
            "`NAME` VARCHAR(100) NOT NULL,"
            "`PROVIDER` VARCHAR(20) NOT NULL,"
            "`BASE_URL` TEXT NULL,"
            "`API_KEY_ENCRYPTED` TEXT NULL,"
            "`DEFAULT_MODEL` VARCHAR(255) NULL,"
            "`IS_DEFAULT` BOOLEAN NOT NULL DEFAULT 0,"
            "`ENABLED` BOOLEAN NOT NULL DEFAULT 1,"
            "`REGDATE` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),"
            "PRIMARY KEY (`SEQ`),"
            "UNIQUE KEY `uk_llm_connection_name` (`NAME`)"
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        )
    )
    _ensure_column(
        conn,
        "mc_o11y_insight_llm_connection",
        "CONTEXT_LENGTH",
        "`CONTEXT_LENGTH` INT(10) UNSIGNED NULL",
    )
    _ensure_column(
        conn,
        "mc_o11y_insight_chat_session",
        "CONNECTION_ID",
        "`CONNECTION_ID` BIGINT(20) UNSIGNED NULL",
    )
    _ensure_column(
        conn,
        "mc_o11y_insight_chat_session",
        "ANALYSIS_TYPE",
        "`ANALYSIS_TYPE` VARCHAR(20) NOT NULL DEFAULT 'log'",
    )
    _ensure_index(
        conn,
        "mc_o11y_insight_chat_session",
        "idx_chat_session_connection_id",
        ["CONNECTION_ID"],
    )
    _ensure_foreign_key(
        conn,
        "mc_o11y_insight_chat_session",
        "fk_chat_session_connection",
        "CONNECTION_ID",
        "mc_o11y_insight_llm_connection",
        "SEQ",
    )
    conn.execute(
        text(
            "UPDATE `mc_o11y_insight_chat_session` SET `ANALYSIS_TYPE` = 'rca' "
            "WHERE `SESSION_ID` LIKE 'server_error_%' AND `CONNECTION_ID` IS NULL"
        )
    )


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


def _ensure_rca_analysis_identity(conn) -> None:
    table = "mc_o11y_insight_server_error_analysis"
    _ensure_column(conn, table, "REQUEST_JSON", "`REQUEST_JSON` JSON NULL")
    _drop_index_if_exists(conn, table, "uk_server_error_trace_id")
    _ensure_index(conn, table, "ix_server_error_trace_id", ["TRACE_ID"])


def _ensure_column(conn, table: str, column: str, column_ddl: str) -> bool:
    row = conn.execute(
        text(
            "SELECT 1 FROM information_schema.columns "
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :t AND COLUMN_NAME = :c"
        ),
        {"t": table, "c": column},
    ).first()
    if row is not None:
        return False
    conn.execute(text(f"ALTER TABLE `{table}` ADD COLUMN {column_ddl}"))
    logger.info("DB migration: added %s.%s", table, column)
    return True


def _drop_index_if_exists(conn, table: str, index_name: str) -> None:
    if not _index_exists(conn, table, index_name):
        return
    conn.execute(text(f"ALTER TABLE `{table}` DROP INDEX `{index_name}`"))
    logger.info("DB migration: dropped index %s.%s", table, index_name)


def _ensure_index(conn, table: str, index_name: str, columns: list[str], *, unique: bool = False) -> None:
    if _index_exists(conn, table, index_name):
        return
    kind = "UNIQUE INDEX" if unique else "INDEX"
    column_sql = ", ".join(f"`{column}`" for column in columns)
    conn.execute(text(f"ALTER TABLE `{table}` ADD {kind} `{index_name}` ({column_sql})"))
    logger.info("DB migration: added %s index %s.%s", "unique" if unique else "plain", table, index_name)


def _ensure_foreign_key(
    conn,
    table: str,
    constraint_name: str,
    column: str,
    referenced_table: str,
    referenced_column: str,
) -> None:
    row = conn.execute(
        text(
            "SELECT 1 FROM information_schema.table_constraints "
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :t "
            "AND CONSTRAINT_NAME = :c AND CONSTRAINT_TYPE = 'FOREIGN KEY'"
        ),
        {"t": table, "c": constraint_name},
    ).first()
    if row is not None:
        return
    conn.execute(
        text(
            f"ALTER TABLE `{table}` ADD CONSTRAINT `{constraint_name}` "
            f"FOREIGN KEY (`{column}`) REFERENCES `{referenced_table}` (`{referenced_column}`) "
            "ON DELETE RESTRICT"
        )
    )
    logger.info("DB migration: added foreign key %s.%s", table, constraint_name)


def _index_exists(conn, table: str, index_name: str) -> bool:
    return (
        conn.execute(
            text(
                "SELECT 1 FROM information_schema.statistics "
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :t AND INDEX_NAME = :i LIMIT 1"
            ),
            {"t": table, "i": index_name},
        ).first()
        is not None
    )
