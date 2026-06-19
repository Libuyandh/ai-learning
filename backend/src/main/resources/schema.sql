CREATE TABLE IF NOT EXISTS learning_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  title VARCHAR(255) NOT NULL,
  input_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  question_count INT NOT NULL DEFAULT 0,
  correct_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS source_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  content LONGTEXT NULL,
  source_url VARCHAR(1024) NULL,
  file_url VARCHAR(1024) NULL,
  parsed_text LONGTEXT NULL,
  created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  stem TEXT NOT NULL,
  options_json JSON NOT NULL,
  correct_answer VARCHAR(255) NOT NULL,
  explanation TEXT NOT NULL,
  knowledge_point VARCHAR(255) NOT NULL,
  difficulty VARCHAR(32) NOT NULL,
  source_url VARCHAR(1024) NOT NULL,
  evidence_text TEXT NOT NULL,
  confidence DECIMAL(4,3) NOT NULL,
  sort_order INT NOT NULL,
  created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS answer_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  user_answer VARCHAR(255) NOT NULL,
  correct TINYINT NOT NULL,
  explanation TEXT NOT NULL,
  answered_at DATETIME NOT NULL,
  UNIQUE KEY uk_session_question (session_id, question_id)
);

CREATE TABLE IF NOT EXISTS learning_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  summary TEXT NOT NULL,
  mastery TEXT NOT NULL,
  score INT NOT NULL,
  accuracy DECIMAL(5,2) NOT NULL,
  weak_points_json JSON NOT NULL,
  suggestions_json JSON NOT NULL,
  wrong_questions_json JSON NOT NULL,
  created_at DATETIME NOT NULL
);
