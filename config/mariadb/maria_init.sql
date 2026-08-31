CREATE DATABASE IF NOT EXISTS `semaphore`;
CREATE USER IF NOT EXISTS 'semaphore'@'%' IDENTIFIED BY 'semaphorepass';
GRANT ALL PRIVILEGES ON `semaphore`.* TO 'semaphore'@'%';
FLUSH PRIVILEGES;

CREATE DATABASE IF NOT EXISTS mc_airflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON mc_airflow.* TO 'mc-agent'@'%';
FLUSH PRIVILEGES;

USE mc_observability;

CREATE TABLE `mc_o11y_insight_anomaly_setting` (
                                                   `SEQ` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
                                                   `NAMESPACE_ID` varchar(100) NOT NULL,
                                                   `INFRA_ID` varchar(100) NOT NULL,
                                                   `NODE_ID` varchar(100) DEFAULT NULL,
                                                   `MEASUREMENT` varchar(100) NOT NULL,
                                                   `EXECUTION_INTERVAL` varchar(100) NOT NULL,
                                                   `LAST_EXECUTION` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
                                                   `REGDATE` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
                                                   PRIMARY KEY (`SEQ`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `mc_o11y_insight_llm_connection` (
  `SEQ` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `NAME` varchar(100) NOT NULL,
  `PROVIDER` varchar(20) NOT NULL,
  `BASE_URL` text DEFAULT NULL,
  `API_KEY_ENCRYPTED` text DEFAULT NULL,
  `DEFAULT_MODEL` varchar(255) DEFAULT NULL,
  `IS_DEFAULT` boolean NOT NULL DEFAULT 0,
  `ENABLED` boolean NOT NULL DEFAULT 1,
  `REGDATE` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`SEQ`),
  UNIQUE KEY `uk_llm_connection_name` (`NAME`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;

CREATE TABLE `mc_o11y_insight_chat_session` (
                                                `SEQ` bigint(20) NOT NULL AUTO_INCREMENT,
                                                `USER_ID` varchar(100) NOT NULL DEFAULT '1',
                                                `SESSION_ID` varchar(100) NOT NULL,
                                                `CONNECTION_ID` bigint(20) unsigned DEFAULT NULL,
                                                `ANALYSIS_TYPE` varchar(20) NOT NULL DEFAULT 'log',
                                                `PROVIDER` varchar(20) NOT NULL,
                                                `MODEL_NAME` varchar(255) NOT NULL,
                                                `REGDATE` timestamp NOT NULL DEFAULT current_timestamp(),
                                                PRIMARY KEY (`SEQ`),
                                                KEY `idx_chat_session_connection_id` (`CONNECTION_ID`),
                                                CONSTRAINT `fk_chat_session_connection`
                                                  FOREIGN KEY (`CONNECTION_ID`)
                                                  REFERENCES `mc_o11y_insight_llm_connection` (`SEQ`)
                                                  ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;

CREATE TABLE `mc_o11y_insight_server_error_analysis` (
  `ID` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `TRACE_ID` varchar(64) DEFAULT NULL COMMENT '분석 범위의 대표 trace_id',
  `SESSION_ID` varchar(100) NOT NULL COMMENT '연결된 채팅 세션 ID',
  `STATUS` varchar(20) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING, SUCCEEDED, FAILED, PARTIAL',
  `SUMMARY` text DEFAULT NULL COMMENT '목록/상세 화면에 표시할 최종 요약',
  `REQUEST_JSON` json DEFAULT NULL COMMENT '검증된 RCA 요청 원문',
  `DETAIL_JSON` json DEFAULT NULL COMMENT '위험도, 근거 요약, trace/log 요약, 추천 조치, 오류 정보',
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`ID`),
  KEY `ix_server_error_trace_id` (`TRACE_ID`),
  KEY `idx_server_error_session_id` (`SESSION_ID`),
  KEY `idx_server_error_status` (`STATUS`),
  KEY `idx_server_error_updated_at` (`UPDATED_AT`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;

CREATE TABLE `mc_o11y_insight_rca_schedule` (
  `ID` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `NAME` varchar(100) NOT NULL COMMENT '사용자 식별용 이름',
  `ENABLED` boolean NOT NULL DEFAULT 1 COMMENT '향후 실행 활성화 여부',
  `INTERVAL_MINUTES` int(10) unsigned NOT NULL COMMENT '완료 후 다음 실행까지의 분 단위 간격',
  `REQUEST_JSON` json NOT NULL COMMENT '실행할 RCA 요청 본문',
  `STATUS` varchar(20) NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE, RUNNING, SUCCEEDED, PARTIAL, FAILED',
  `LAST_EXECUTION` timestamp NULL DEFAULT NULL COMMENT '마지막 실행 종료 UTC 시각',
  `NEXT_EXECUTION` timestamp NULL DEFAULT NULL COMMENT '다음 실행 예정 UTC 시각',
  `LAST_ANALYSIS_ID` bigint(20) unsigned DEFAULT NULL COMMENT '마지막 RCA 분석 레코드 ID',
  `LAST_ERROR` text DEFAULT NULL COMMENT '마지막 실행 실패 원인',
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`ID`),
  KEY `ix_rca_schedule_due` (`ENABLED`, `NEXT_EXECUTION`, `STATUS`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;
