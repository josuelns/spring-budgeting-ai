package dio.budgeting.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade de domínio representando uma conta fixa agendada (boleto, aluguel, etc.).
 *
 * <p>Usa classe mutável (não record) pois o status muda de PENDING → PAID via payBill.
 * O accountId é armazenado como Long para desacoplar do objeto JPA AccountEntity.
 */
@Getter
@AllArgsConstructor
public class BillPlanning {

    private Long id;
    private Long accountId;
    private String description;
    private LocalDate dueDate;
    private BigDecimal amount;
    private BillPlanningStatus status;

    /**
     * Regra de negócio: marca a conta como paga.
     * O débito no saldo é responsabilidade do use case (via Account.withdraw).
     *
     * @throws IllegalStateException se a conta já foi paga anteriormente
     */
    public void pay() {
        if (this.status == BillPlanningStatus.PAID) {
            throw new IllegalStateException(
                    "A conta '" + this.description + "' já foi paga anteriormente.");
        }
        this.status = BillPlanningStatus.PAID;
    }
}
