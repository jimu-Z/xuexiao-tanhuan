-- Incremental migration. Existing talk_person values are retained unchanged.
ALTER TABLE talk_session MODIFY COLUMN talk_person VARCHAR(500) NOT NULL COMMENT '谈话人';
SET @participant_revision_ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'talk_session' AND COLUMN_NAME = 'revision') = 0, 'ALTER TABLE talk_session ADD COLUMN revision BIGINT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE participant_revision_stmt FROM @participant_revision_ddl;
EXECUTE participant_revision_stmt;
DEALLOCATE PREPARE participant_revision_stmt;
CREATE TABLE IF NOT EXISTS talk_session_participant (
  participant_id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  user_id BIGINT DEFAULT NULL,
  display_name VARCHAR(100) NOT NULL,
  custom_identity VARCHAR(100) DEFAULT NULL,
  position_index INT NOT NULL DEFAULT 0,
  PRIMARY KEY (participant_id),
  UNIQUE KEY uk_session_participant_user (session_id, user_id),
  KEY idx_participant_user (user_id, session_id),
  KEY idx_participant_session (session_id, position_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
