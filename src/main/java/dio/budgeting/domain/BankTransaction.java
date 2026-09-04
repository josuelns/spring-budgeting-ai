package dio.budgeting.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Value object imutável representando uma movimentação bancária.
 * Usa record pois não possui comportamento — apenas carrega dados.
 */
public record BankTransaction(
        Long id,
        BankTransactionType transactionType,
        BigDecimal amount,
        LocalDateTime createdAt) {
}
