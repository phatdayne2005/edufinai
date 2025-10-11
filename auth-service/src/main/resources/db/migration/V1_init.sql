CREATE TABLE users (
  user_id         BINARY(16)      NOT NULL,
  username        VARCHAR(50)     NOT NULL,
  password_hash   VARCHAR(100)    NOT NULL,
  email           VARCHAR(120)    NOT NULL,
  phone           VARCHAR(20)     NULL,
  role            ENUM('LEARNER','CREATOR','MOD','ADMIN') NOT NULL DEFAULT 'LEARNER',
  avatar_url      VARCHAR(255)    NULL,
  finance_profile JSON            NULL,
  goals           JSON            NULL,
  last_login      DATETIME        NULL,
  status          ENUM('ACTIVE','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT pk_users PRIMARY KEY (user_id),
  CONSTRAINT uq_users_username UNIQUE (username),
  CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
