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
                                                   `MCI_ID` varchar(100) NOT NULL,
                                                   `VM_ID` varchar(100) DEFAULT NULL,
                                                   `MEASUREMENT` varchar(100) NOT NULL,
                                                   `EXECUTION_INTERVAL` varchar(100) NOT NULL,
                                                   `LAST_EXECUTION` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
                                                   `REGDATE` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
                                                   PRIMARY KEY (`SEQ`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `mc_o11y_insight_llm_api_key` (
  `SEQ` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `PROVIDER` varchar(20) NOT NULL,
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
