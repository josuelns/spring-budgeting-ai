package dio.budgeting.application.output;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de saída para exposição segura do extrato bancário.
 * Gerado pelo MapStruct a partir do domain BankTransaction.
 */
public record TransactionDto(
        Long id,
        String transactionType,
        BigDecimal amount,
        LocalDateTime createdAt) {
}
