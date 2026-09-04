-- ============================================================
-- AI Personal Finance Management — Script de Inicialização
-- Executado automaticamente pelo MySQL na criação do container
-- ============================================================

-- Banco dedicado ao Keycloak (evita conflito com as tabelas da aplicação)
CREATE DATABASE IF NOT EXISTS keycloak_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Garante que as tabelas da aplicação vão para o banco correto
USE bank_assistant_db;

-- ----------------------------------------------------------
-- TABELAS (ordem respeitando as chaves estrangeiras)
-- ----------------------------------------------------------

CREATE TABLE IF NOT EXISTS banks
(
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    code VARCHAR(10)  NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_banks_code UNIQUE (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accounts
(
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    account_number   VARCHAR(20)    NOT NULL,
    balance          DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    monthly_salary   DECIMAL(19, 2)          DEFAULT NULL,
    keycloak_user_id VARCHAR(255)   NOT NULL,
    bank_id          BIGINT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_accounts_number      UNIQUE (account_number),
    CONSTRAINT uk_accounts_keycloak_id UNIQUE (keycloak_user_id),
    CONSTRAINT fk_accounts_bank        FOREIGN KEY (bank_id) REFERENCES banks (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bank_transactions
(
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    account_id       BIGINT         NOT NULL,
    transaction_type VARCHAR(20)    NOT NULL,
    amount           DECIMAL(19, 2) NOT NULL,
    created_at       DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bill_planning
(
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    account_id  BIGINT         NOT NULL,
    description VARCHAR(255)   NOT NULL,
    due_date    DATE           NOT NULL,
    amount      DECIMAL(19, 2) NOT NULL,
    status      VARCHAR(10)    NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (id),
    CONSTRAINT fk_bill_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- MASSA DE DADOS DE TESTE
-- ----------------------------------------------------------

-- Bancos
INSERT INTO banks (code, name)
VALUES ('001', 'Banco do Brasil'),
       ('341', 'Itaú Unibanco');

-- Contas bancárias
-- Conta 1: usuário principal de teste — salário R$ 5.000, saldo R$ 1.500
--          keycloak_user_id = 'user-test-uuid-123' (simula o "sub" do JWT do Keycloak)
-- Conta 2: conta secundária para testar transferências
INSERT INTO accounts (account_number, balance, monthly_salary, keycloak_user_id, bank_id)
VALUES ('0001-1', 1500.00, 5000.00, 'user-test-uuid-123', 1),
       ('0002-1', 500.00, NULL, 'user-test-uuid-456', 2);

-- Transações iniciais (crédito de abertura de conta)
INSERT INTO bank_transactions (account_id, transaction_type, amount, created_at)
VALUES (1, 'DEPOSIT', 2000.00, DATE_SUB(NOW(), INTERVAL 30 DAY)),
       (1, 'WITHDRAWAL', 500.00, DATE_SUB(NOW(), INTERVAL 15 DAY)),
       (2, 'DEPOSIT', 500.00, NOW());

-- Contas agendadas da conta 1 (para testar getBillPlanning, payBill e generateFinancialInsights)
INSERT INTO bill_planning (account_id, description, due_date, amount, status)
VALUES (1, 'Internet', DATE_FORMAT(NOW(), '%Y-%m-15'), 120.00, 'PENDING'),
       (1, 'Aluguel', DATE_FORMAT(NOW(), '%Y-%m-28'), 1200.00, 'PENDING'),
       (1, 'Streaming', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 MONTH), '%Y-%m-10'), 45.90, 'PAID');
