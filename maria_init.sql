
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
    ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='모니터링 호스트';

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
    ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='호스트에 등록된 모니터링 항목';

CREATE TABLE IF NOT EXISTS `m_cmp_agent_plugin_def` (
    `SEQ` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '고유번호',
    `NAME` varchar(50) NOT NULL COMMENT '플러그인 명',
    `PLUGIN_ID` varchar(50) NOT NULL COMMENT '플러그인 ID',
    `CATEGORY` set('APPLICATIONS','BUILD_N_DEPLOY','CLOUD','CONTAINERS','DATA_STORES','IOT','LOGGING','MESSAGING','NETWORKING','SERVERS','SYSTEMS','WEB') NOT NULL COMMENT '카테고리',
    `OS` set('LINUX','WINDOWS','MACOS') NOT NULL COMMENT '운영체제',
    PRIMARY KEY (`SEQ`)
    ) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='모니터링 항목 정의';

INSERT INTO `m_cmp_agent_plugin_def` (`SEQ`, `NAME`, `PLUGIN_ID`, `CATEGORY`, `OS`) VALUES
    (1, 'cpu', '[[inputs.cpu]]', 'SYSTEMS', 'LINUX,WINDOWS,MACOS'),
    (2, 'disk', '[[inputs.disk]]', 'SYSTEMS', 'LINUX,WINDOWS,MACOS'),
    (3, 'diskio', '[[inputs.diskio]]', 'SYSTEMS', 'LINUX,WINDOWS,MACOS'),
    (4, 'mem', '[[inputs.mem]]', 'SYSTEMS', 'LINUX,WINDOWS,MACOS'),
    (5, 'processes', '[[inputs.processes]]', 'SYSTEMS', 'LINUX,WINDOWS,MACOS'),
    (6, 'swap', '[[inputs.swap]]', 'SYSTEMS', 'LINUX,MACOS'),
    (7, 'syslog', '[[inputs.syslog]]', 'LOGGING,SYSTEMS', 'LINUX,WINDOWS,MACOS'),
    (8, 'system', '[[inputs.system]]', 'SYSTEMS', 'LINUX,WINDOWS,MACOS');