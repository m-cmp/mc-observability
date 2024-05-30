CREATE TABLE IF NOT EXISTS `m_cmp_agent_host` (
  `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '고유번호',
  `NAME` varchar(50) NOT NULL DEFAULT '' COMMENT '모니터링 호스트명',
  `UUID` char(36) NOT NULL COMMENT '호스트 고유 ID Agent 실행시 Host 구분값으로 사용 Agent 등록시 자체 발행 및 파일 기록',
  `OS` enum('LINUX','WINDOWS','MACOS','UNIX','SOLARIS') NOT NULL COMMENT '운영체제',
  `MONITORING_YN` enum('Y','N') NOT NULL DEFAULT 'Y' COMMENT '모니터링 ON/OFF',
  `STATE` enum('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT '모니터링 상태',
  `SYNC_YN` enum('Y','N') NOT NULL DEFAULT 'Y',
  `CREATE_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '등록일시',
  `UPDATE_AT` timestamp NULL DEFAULT NULL COMMENT '마지막 업데이트 시간',
  `EX` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '{}' COMMENT '추가정보(cmp agent에서는 처리 없음)',
  `DESCRIPTION` varchar(1024) DEFAULT NULL,
  `TELEGRAF_STATE` enum('RUNNING','STOPPED','FAILED') DEFAULT 'STOPPED',
  PRIMARY KEY (`SEQ`),
  UNIQUE KEY `UUID` (`UUID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='모니터링 호스트';

CREATE TABLE IF NOT EXISTS `m_cmp_agent_host_item` (
  `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '고유번호',
  `HOST_SEQ` int(10) unsigned NOT NULL COMMENT '호스트 고유번호',
  `STATE` enum('NONE','ADD','UPDATE','DELETE') NOT NULL DEFAULT 'ADD' COMMENT '상태',
  `MONITORING_YN` enum('Y','N') NOT NULL DEFAULT 'Y' COMMENT '모니터링 ON/OFF',
  `CREATE_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  `UPDATE_AT` timestamp NULL DEFAULT NULL COMMENT '마지막 업데이트 시간',
  `PLUGIN_SEQ` int(10) unsigned NOT NULL COMMENT '플러그인 SEQ',
  `PLUGIN_NAME` varchar(50) NOT NULL COMMENT '플러그인 명',
  `NAME` varchar(50) NOT NULL COMMENT '모니터링 명',
  `INTERVAL_SEC` mediumint(9) NOT NULL COMMENT '주기(초)',
  `SETTING` longtext NOT NULL COMMENT '상세 설정',
  PRIMARY KEY (`SEQ`) USING BTREE,
  KEY `FK_ITEM_SEQ_HOST_SEQ` (`HOST_SEQ`),
  CONSTRAINT `FK_ITEM_SEQ_HOST_SEQ` FOREIGN KEY (`HOST_SEQ`) REFERENCES `m_cmp_agent_host` (`SEQ`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='호스트에 등록된 모니터링 항목';

CREATE TABLE IF NOT EXISTS `m_cmp_agent_host_storage` (
  `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '고유번호',
  `HOST_SEQ` int(10) unsigned NOT NULL COMMENT '호스트 고유 번호',
  `NAME` varchar(100) NOT NULL DEFAULT '' COMMENT '저장소 명',
  `MONITORING_YN` enum('Y','N') NOT NULL DEFAULT 'Y' COMMENT '모니터링 YN',
  `PLUGIN_SEQ` int(10) unsigned NOT NULL DEFAULT 0,
  `PLUGIN_NAME` varchar(50) NOT NULL DEFAULT '',
  `SETTING` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '저장소 정보',
  `STATE` enum('NONE','ADD','UPDATE','DELETE') NOT NULL DEFAULT 'ADD',
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`SEQ`),
  KEY `FK_STORAGE_SEQ_HOST_SEQ` (`HOST_SEQ`),
  CONSTRAINT `FK_STORAGE_SEQ_HOST_SEQ` FOREIGN KEY (`HOST_SEQ`) REFERENCES `m_cmp_agent_host` (`SEQ`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='호스트에 등록된 저장소';

CREATE TABLE IF NOT EXISTS `m_cmp_agent_mining_db` (
  `URL` varchar(50) DEFAULT NULL,
  `DATABASE` varchar(50) DEFAULT NULL,
  `RETENTION_POLICY` varchar(50) DEFAULT NULL,
  `USERNAME` varchar(50) DEFAULT NULL,
  `PASSWORD` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `m_cmp_agent_mining_db` (`URL`, `DATABASE`, `RETENTION_POLICY`, `USERNAME`, `PASSWORD`) VALUES
	('http://localhost:8086', 'mc-mining', 'autogen', 'mc-agent', 'mc-agent');

CREATE TABLE IF NOT EXISTS `m_cmp_agent_plugin_def` (
  `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '고유번호',
  `NAME` varchar(50) NOT NULL COMMENT '플러그인 명',
  `PLUGIN_ID` varchar(50) NOT NULL COMMENT '플러그인 ID',
  `PLUGIN_TYPE` enum('INPUT','PROCESSOR','AGGREGATOR','OUTPUT') NOT NULL,
  `OS` set('LINUX','WINDOWS','MACOS') DEFAULT NULL COMMENT '운영체제',
  PRIMARY KEY (`SEQ`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COMMENT='모니터링 항목 정의';

INSERT INTO `m_cmp_agent_plugin_def` (`SEQ`, `NAME`, `PLUGIN_ID`, `PLUGIN_TYPE`, `OS`) VALUES
	(1, 'cpu', '[[inputs.cpu]]', 'INPUT', 'LINUX,WINDOWS,MACOS'),
	(2, 'disk', '[[inputs.disk]]', 'INPUT', 'LINUX,WINDOWS,MACOS'),
	(3, 'diskio', '[[inputs.diskio]]', 'INPUT', 'LINUX,WINDOWS,MACOS'),
	(4, 'mem', '[[inputs.mem]]', 'INPUT', 'LINUX,WINDOWS,MACOS'),
	(5, 'processes', '[[inputs.processes]]', 'INPUT', 'LINUX,WINDOWS,MACOS'),
	(6, 'swap', '[[inputs.swap]]', 'INPUT', 'LINUX,MACOS'),
	(7, 'tail', '[[inputs.tail]]', 'INPUT', 'LINUX,WINDOWS,MACOS'),
	(8, 'system', '[[inputs.system]]', 'INPUT', 'LINUX,WINDOWS,MACOS'),
	(9, 'influxdb', '[[outputs.influxdb]]', 'OUTPUT', 'LINUX,WINDOWS,MACOS');

CREATE TABLE IF NOT EXISTS `m_cmp_agent_summary_influx` (
  `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `URL` varchar(50) NOT NULL DEFAULT '',
  `DATABASE` varchar(50) NOT NULL DEFAULT '',
  `RETENTION_POLICY` varchar(50) NOT NULL DEFAULT '',
  `USERNAME` varchar(50) NOT NULL DEFAULT '',
  `PASSWORD` varchar(50) NOT NULL DEFAULT '',
  PRIMARY KEY (`SEQ`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;