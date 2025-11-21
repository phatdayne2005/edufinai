-- Flyway migration V1: init schema for finance-service (MySQL)

-- Table: goal
CREATE TABLE IF NOT EXISTS goal (
    goal_id     BINARY(16) PRIMARY KEY,
    user_id     BINARY(16)     NOT NULL,
    title       VARCHAR(255)   NOT NULL,
    amount      DECIMAL(19,2)  NOT NULL,
    start_at    DATETIME       NOT NULL,
    end_at      DATETIME       NOT NULL,
    status      ENUM('ACTIVE','COMPLETED','FAILED') NOT NULL,
    updated_at  DATETIME       NOT NULL,
    new_status  ENUM('ACTIVE','COMPLETED','FAILED') NOT NULL
);

-- Table: expense (renamed to transactions in V2)
CREATE TABLE IF NOT EXISTS expense (
    expense_id     BINARY(16) PRIMARY KEY,
    user_id        BINARY(16)     NOT NULL,
    type           ENUM('INCOME','EXPENSE') NOT NULL,
    amount         DECIMAL(19,2)  NOT NULL,
    category       VARCHAR(100)   NOT NULL,
    note           TEXT,
    linked_account VARCHAR(100),
    goal_id        BINARY(16),
    status         ENUM('ACTIVE','DELETED') NOT NULL,
    created_at     DATETIME       NOT NULL,
    updated_at     DATETIME       NOT NULL,
    CONSTRAINT fk_expense_goal FOREIGN KEY (goal_id) REFERENCES goal(goal_id)
);
