-- Incremental and repeatable. Back up the database before applying in production.
CREATE TABLE IF NOT EXISTS talk_student_photo (
  photo_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL UNIQUE,
  file_key VARCHAR(80) NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_student_photo_student FOREIGN KEY (student_id) REFERENCES talk_student(student_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
