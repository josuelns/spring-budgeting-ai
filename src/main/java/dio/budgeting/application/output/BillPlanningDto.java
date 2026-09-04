package dio.budgeting.application.output;

import java.math.BigDecimal;

/**
 * DTO de saída para exposição segura do planejamento de contas fixas.
 * O dueDate é convertido para String para evitar problemas de serialização ISO-8601.
 * Gerado pelo MapStruct a partir do domain BillPlanning.
 */
public record BillPlanningDto(
        Long id,
        String description,
        String dueDate,
        BigDecimal amount,
        String status) {
}
