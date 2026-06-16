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

CREATE TABLE `mc_o11y_insight_llm_api_key` (
  `SEQ` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `PROVIDER` varchar(20) NOT NULL,
  `API_KEY` text DEFAULT NULL,
  `BASE_URL` text DEFAULT NULL,
  PRIMARY KEY (`SEQ`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;

CREATE TABLE `mc_o11y_insight_chat_session` (
                                                `SEQ` bigint(20) NOT NULL AUTO_INCREMENT,
                                                `USER_ID` varchar(100) NOT NULL DEFAULT '1',
                                                `SESSION_ID` varchar(100) NOT NULL,
                                                `PROVIDER` varchar(20) NOT NULL,
                                                `MODEL_NAME` varchar(20) NOT NULL,
                                                `REGDATE` timestamp NOT NULL DEFAULT current_timestamp(),
                                                PRIMARY KEY (`SEQ`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;

CREATE TABLE `mc_o11y_insight_server_error_analysis` (
  `ID` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `TRACE_ID` varchar(64) DEFAULT NULL COMMENT '대표 trace_id, NULL이면 unique dedup 미적용',
  `SESSION_ID` varchar(100) NOT NULL COMMENT '연결된 채팅 세션 ID',
  `STATUS` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, RUNNING, SUCCEEDED, FAILED, PARTIAL',
  `SUMMARY` text DEFAULT NULL COMMENT '목록/상세 화면에 표시할 최종 요약',
  `DETAIL_JSON` json DEFAULT NULL COMMENT '위험도, 근거 요약, trace/log 요약, 추천 조치, 오류 정보',
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_server_error_trace_id` (`TRACE_ID`),
  KEY `idx_server_error_session_id` (`SESSION_ID`),
  KEY `idx_server_error_status` (`STATUS`),
  KEY `idx_server_error_updated_at` (`UPDATED_AT`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;
