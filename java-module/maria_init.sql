-- --------------------------------------------------------
-- 호스트:                          192.168.110.211
-- 서버 버전:                        10.7.6-MariaDB-1:10.7.6+maria~ubu2004 - mariadb.org binary distribution
-- 서버 OS:                        debian-linux-gnu
-- HeidiSQL 버전:                  12.3.0.6589
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


CREATE DATABASE IF NOT EXISTS mc_airflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON mc_airflow.* TO 'mc-agent'@'%';
FLUSH PRIVILEGES;

USE mc_observability;


-- 테이블 mc_observability.mc_o11y_agent_plugin_def 구조 내보내기
CREATE TABLE IF NOT EXISTS `mc_o11y_agent_plugin_def` (
    `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `NAME` varchar(50) NOT NULL,
    `PLUGIN_ID` varchar(50) NOT NULL,
    `PLUGIN_TYPE` enum('INPUT','PROCESSOR','AGGREGATOR','OUTPUT') NOT NULL,
    PRIMARY KEY (`SEQ`)
    ) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4;

-- 테이블 데이터 mc_observability.mc_o11y_agent_plugin_def:~10 rows (대략적) 내보내기
INSERT INTO `mc_o11y_agent_plugin_def` (`SEQ`, `NAME`, `PLUGIN_ID`, `PLUGIN_TYPE`) VALUES
                                                                                       (1, 'cpu', '[[inputs.cpu]]', 'INPUT'),
                                                                                       (2, 'disk', '[[inputs.disk]]', 'INPUT'),
                                                                                       (3, 'diskio', '[[inputs.diskio]]', 'INPUT'),
                                                                                       (4, 'mem', '[[inputs.mem]]', 'INPUT'),
                                                                                       (5, 'processes', '[[inputs.processes]]', 'INPUT'),
                                                                                       (6, 'swap', '[[inputs.swap]]', 'INPUT'),
                                                                                       (7, 'tail', '[[inputs.tail]]', 'INPUT'),
                                                                                       (8, 'system', '[[inputs.system]]', 'INPUT'),
                                                                                       (9, 'influxdb', '[[outputs.influxdb]]', 'OUTPUT'),
                                                                                       (10, 'opensearch', '[[outputs.opensearch]]', 'OUTPUT');

-- 테이블 mc_observability.mc_o11y_agent_summary_influx 구조 내보내기
CREATE TABLE IF NOT EXISTS `mc_o11y_agent_summary_influx` (
    `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `URL` varchar(50) NOT NULL DEFAULT '',
    `DATABASE` varchar(50) NOT NULL DEFAULT '',
    `RETENTION_POLICY` varchar(50) NOT NULL DEFAULT '',
    `USERNAME` varchar(50) NOT NULL DEFAULT '',
    `PASSWORD` varchar(50) NOT NULL DEFAULT '',
    PRIMARY KEY (`SEQ`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 테이블 데이터 mc_observability.mc_o11y_agent_summary_influx:~0 rows (대략적) 내보내기

-- 테이블 mc_observability.mc_o11y_agent_summary_opensearch 구조 내보내기
CREATE TABLE IF NOT EXISTS `mc_o11y_agent_summary_opensearch` (
    `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `URL` varchar(50) NOT NULL DEFAULT '',
    `INDEX_NAME` varchar(50) NOT NULL DEFAULT '',
    `USERNAME` varchar(50) NOT NULL DEFAULT '',
    `PASSWORD` varchar(50) NOT NULL DEFAULT '',
    PRIMARY KEY (`SEQ`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 테이블 데이터 mc_observability.mc_o11y_agent_summary_opensearch:~0 rows (대략적) 내보내기

-- 테이블 mc_observability.mc_o11y_agent_target 구조 내보내기
CREATE TABLE IF NOT EXISTS `mc_o11y_agent_target` (
    `NS_ID` varchar(50) NOT NULL,
    `MCI_ID` varchar(50) NOT NULL,
    `ID` varchar(50) NOT NULL,
    `NAME` varchar(50) DEFAULT '',
    `ALIAS_NAME` varchar(50) DEFAULT NULL,
    `DESCRIPTION` varchar(50) DEFAULT NULL,
    `STATE` enum('ACTIVE','INACTIVE') DEFAULT 'INACTIVE',
    PRIMARY KEY (`NS_ID`,`MCI_ID`,`ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 테이블 데이터 mc_observability.mc_o11y_agent_target:~0 rows (대략적) 내보내기

-- 테이블 mc_observability.mc_o11y_agent_target_monitoring_config 구조 내보내기
CREATE TABLE IF NOT EXISTS `mc_o11y_agent_target_monitoring_config` (
    `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `NS_ID` varchar(50) NOT NULL,
    `MCI_ID` varchar(50) NOT NULL,
    `TARGET_ID` varchar(50) NOT NULL,
    `NAME` varchar(50) NOT NULL,
    `STATE` enum('NONE','ADD','UPDATE','DELETE') NOT NULL DEFAULT 'ADD',
    `PLUGIN_SEQ` int(10) unsigned NOT NULL DEFAULT 0,
    `PLUGIN_NAME` varchar(50) NOT NULL,
    `PLUGIN_TYPE` enum('INPUT','PROCESSOR','AGGREGATOR','OUTPUT') NOT NULL,
    `PLUGIN_CONFIG` longtext NOT NULL DEFAULT '',
    PRIMARY KEY (`SEQ`),
    KEY `NS_ID_MCI_ID_TARGET_ID` (`NS_ID`,`MCI_ID`,`TARGET_ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 테이블 데이터 mc_observability.mc_o11y_agent_target_monitoring_config:~1 rows (대략적) 내보내기

-- 테이블 mc_observability.mc_o11y_mining_db 구조 내보내기
CREATE TABLE IF NOT EXISTS `mc_o11y_mining_db` (
    `URL` varchar(50) NOT NULL DEFAULT '',
    `DATABASE` varchar(50) NOT NULL DEFAULT '',
    `RETENTION_POLICY` varchar(50) NOT NULL DEFAULT '',
    `USERNAME` varchar(50) NOT NULL DEFAULT '',
    `PASSWORD` varchar(50) NOT NULL DEFAULT ''
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 테이블 데이터 mc_observability.mc_o11y_mining_db:~0 rows (대략적) 내보내기
INSERT INTO `mc_o11y_mining_db` (`URL`, `DATABASE`, `RETENTION_POLICY`, `USERNAME`, `PASSWORD`) VALUES
    ('http://mc-observability-influx:8086', 'downsampling', 'autogen', 'mc-agent', 'mc-agent');

-- 테이블 mc_observability.mc_o11y_trigger_alert_email 구조 내보내기
CREATE TABLE IF NOT EXISTS `mc_o11y_trigger_alert_email` (
    `seq` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL,
    `email` varchar(100) NOT NULL,
    `policy_seq` int(10) unsigned NOT NULL,
    PRIMARY KEY (`seq`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 테이블 데이터 mc_observability.mc_o11y_trigger_alert_email:~0 rows (대략적) 내보내기

-- 테이블 mc_observability.mc_o11y_trigger_alert_slack 구조 내보내기
CREATE TABLE IF NOT EXISTS `mc_o11y_trigger_alert_slack` (
    `seq` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `policy_seq` int(10) unsigned NOT NULL,
    `name` varchar(100) NOT NULL,
    `token` varchar(100) NOT NULL,
    `channel` varchar(100) NOT NULL,
    PRIMARY KEY (`seq`),
    UNIQUE KEY `mc_o11y_trigger_alert_slack_unique` (`policy_seq`,`name`,`token`,`channel`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 테이블 데이터 mc_observability.mc_o11y_trigger_alert_slack:~0 rows (대략적) 내보내기

-- 테이블 mc_observability.mc_o11y_trigger_history 구조 내보내기
CREATE TABLE IF NOT EXISTS `mc_o11y_trigger_history` (
    `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '고유번호',
    `POLICY_SEQ` int(10) unsigned NOT NULL COMMENT '트리거 정책 고유번호',
    `TARGET_SEQ` int(10) unsigned NOT NULL COMMENT '트리거 대상 고유번호',
    `NAME` varchar(50) DEFAULT '' COMMENT '트리거 대상 호스트명',
    `NS_ID` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '추가정보(cmp agent에서는 처리 없음)',
    `TARGET_ID` varchar(50) NOT NULL COMMENT '호스트 고유 ID Agent 실행시 Host 구분값으로 사용 Agent 등록시 자체 발행 및 파일 기록',
    `METRIC` varchar(50) NOT NULL DEFAULT '' COMMENT '메트릭 이름',
    `DATA` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '' COMMENT '트리거 상세 내용',
    `LEVEL` varchar(50) NOT NULL DEFAULT '' COMMENT '트리거 레벨',
    `CREATE_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '등록일시',
    `OCCUR_TIME` timestamp NULL DEFAULT NULL COMMENT '발생일시',
    PRIMARY KEY (`SEQ`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='트리거  히스토리';

-- 테이블 데이터 mc_observability.mc_o11y_trigger_history:~0 rows (대략적) 내보내기

-- 테이블 mc_observability.mc_o11y_trigger_policy 구조 내보내기
CREATE TABLE IF NOT EXISTS `mc_o11y_trigger_policy` (
    `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '고유번호',
    `NAME` varchar(50) NOT NULL DEFAULT '' COMMENT '트리거 정책 이름',
    `DESCRIPTION` varchar(1024) DEFAULT NULL COMMENT '설명',
    `METRIC` varchar(50) NOT NULL DEFAULT '' COMMENT '메트릭 이름',
    `THRESHOLD` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '' COMMENT '트리거 상세 정책',
    `TICK_SCRIPT` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '{}' COMMENT 'Kapacitor 스크립트',
    `CREATE_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '등록일시',
    `UPDATE_AT` timestamp NULL DEFAULT NULL COMMENT '마지막 업데이트 시간',
    `FIELD` varchar(100) NOT NULL,
    `STATISTICS` varchar(100) NOT NULL,
    `STATUS` enum('ENABLED','DISABLED') NOT NULL DEFAULT 'ENABLED',
    `GROUP_FIELDS` varchar(100) DEFAULT NULL,
    `AGENT_MANAGER_IP` varchar(100) NOT NULL,
    PRIMARY KEY (`SEQ`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='트리거 정책';

-- 테이블 데이터 mc_observability.mc_o11y_trigger_policy:~0 rows (대략적) 내보내기

-- 테이블 mc_observability.mc_o11y_trigger_target 구조 내보내기
CREATE TABLE IF NOT EXISTS `mc_o11y_trigger_target` (
    `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '고유번호',
    `POLICY_SEQ` int(10) unsigned NOT NULL COMMENT '정책 고유번호',
    `CREATE_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '등록일시',
    `UPDATE_AT` timestamp NULL DEFAULT NULL COMMENT '마지막 업데이트 시간',
    `NS_ID` varchar(50) NOT NULL,
    `TARGET_ID` varchar(50) NOT NULL,
    PRIMARY KEY (`SEQ`),
    UNIQUE KEY `mc_o11y_trigger_target_unique` (`POLICY_SEQ`,`NS_ID`,`TARGET_ID`),
    KEY `m_cmp_trigger_target_m_cmp_trigger_policy_FK` (`POLICY_SEQ`),
    CONSTRAINT `m_cmp_trigger_target_m_cmp_trigger_policy_FK` FOREIGN KEY (`POLICY_SEQ`) REFERENCES `mc_o11y_trigger_policy` (`SEQ`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='트리거  대상';

-- 테이블 데이터 mc_observability.mc_o11y_trigger_target:~0 rows (대략적) 내보내기

-- 테이블 mc_observability.mc_o11y_trigger_target_storage 구조 내보내기
CREATE TABLE IF NOT EXISTS `mc_o11y_trigger_target_storage` (
    `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '고유번호',
    `TARGET_SEQ` int(10) unsigned NOT NULL COMMENT '트리거 대상 고유번호',
    `URL` varchar(50) NOT NULL DEFAULT '' COMMENT '저장소 URL',
    `DATABASE` varchar(50) NOT NULL DEFAULT '' COMMENT '저장소 데이터베이스명',
    `RETENTION_POLICY` varchar(50) NOT NULL DEFAULT '' COMMENT '저장소 Retention Policy',
    `CREATE_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '등록일시',
    PRIMARY KEY (`SEQ`),
    UNIQUE KEY `m_cmp_trigger_target_storage_unique` (`TARGET_SEQ`,`URL`,`DATABASE`,`RETENTION_POLICY`),
    CONSTRAINT `m_cmp_trigger_target_storage_m_cmp_trigger_target_FK` FOREIGN KEY (`TARGET_SEQ`) REFERENCES `mc_o11y_trigger_target` (`SEQ`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='트리거 대상 저장소';

-- 테이블 데이터 mc_observability.mc_o11y_trigger_target_storage:~0 rows (대략적) 내보내기


CREATE TABLE `mc_o11y_insight_anomaly_setting` (
  `SEQ` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `NAMESPACE_ID` varchar(100) NOT NULL,
  `TARGET_ID` varchar(100) NOT NULL,
  `TARGET_TYPE` varchar(100) NOT NULL,
  `MEASUREMENT` varchar(100) NOT NULL,
  `EXECUTION_INTERVAL` varchar(100) NOT NULL,
  `LAST_EXECUTION` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `REGDATE` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`SEQ`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


CREATE TABLE `mc_o11y_insight_openai_api_key` (
  `SEQ` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `API_KEY` text NOT NULL,
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


/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
