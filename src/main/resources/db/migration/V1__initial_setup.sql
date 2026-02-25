CREATE TABLE IF NOT EXISTS users
(
    user_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) UNIQUE                  NOT NULL,
    password   VARCHAR(255)                         NOT NULL,
    created_at DATETIME DEFAULT (CURRENT_TIMESTAMP) NOT NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS expenses
(
    expense_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT                                                         NOT NULL,
    description VARCHAR(255)                                                   NOT NULL,
    amount      DECIMAL(12, 2)                                                 NOT NULL,
    entry_date  DATE     DEFAULT (CURRENT_DATE)                                NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP                             NOT NULL,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT expenses_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_expenses_entry_date ON expenses (entry_date);