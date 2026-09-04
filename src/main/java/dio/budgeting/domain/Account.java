package dio.budgeting.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agregado raiz do contexto bancário.
 * Encapsula as regras de negócio de movimentação financeira.
 * Cada método mutador retorna a BankTransaction gerada para que
 * a camada de infraestrutura persista sem depender de cascade JPA.
 */
@Getter
@AllArgsConstructor
public class Account {

    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private BigDecimal monthlySalary; // salário mensal para cálculo de insights financeiros
    private String keycloakUserId;
    private Bank bank;

    // -------------------------------------------------------
    // Regras de negócio — DEPÓSITO
    // -------------------------------------------------------

    public BankTransaction deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        return new BankTransaction(null, BankTransactionType.DEPOSIT, amount, LocalDateTime.now());
    }

    // -------------------------------------------------------
    // Regras de negócio — SAQUE
    // -------------------------------------------------------

    public BankTransaction withdraw(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo insuficiente. Saldo atual: R$ " + this.balance +
                    ", valor solicitado: R$ " + amount);
        }
        this.balance = this.balance.subtract(amount);
        return new BankTransaction(null, BankTransactionType.WITHDRAWAL, amount, LocalDateTime.now());
    }

    // -------------------------------------------------------
    // Regras de negócio — TRANSFERÊNCIA (saída)
    // -------------------------------------------------------

    public BankTransaction transferOut(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo insuficiente para transferência. Saldo atual: R$ " + this.balance);
        }
        this.balance = this.balance.subtract(amount);
        return new BankTransaction(null, BankTransactionType.TRANSFER_OUT, amount, LocalDateTime.now());
    }

    // -------------------------------------------------------
    // Regras de negócio — TRANSFERÊNCIA (entrada)
    // -------------------------------------------------------

    public BankTransaction transferIn(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        return new BankTransaction(null, BankTransactionType.TRANSFER_IN, amount, LocalDateTime.now());
    }
}
