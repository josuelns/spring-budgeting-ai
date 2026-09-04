package dio.budgeting.domain;

import java.util.List;
import java.util.Optional;

/**
 * Port para persistência de contas fixas agendadas.
 * A implementação concreta fica na camada de infraestrutura (JPA).
 */
public interface BillPlanningRepository {

    BillPlanning save(BillPlanning billPlanning);

    List<BillPlanning> findByAccountId(Long accountId);

    /**
     * Busca uma conta PENDENTE por descrição (case-insensitive) para o usuário.
     * Usada pelo payBill para localizar o boleto a ser pago via comando de voz.
     */
    Optional<BillPlanning> findPendingByAccountIdAndDescription(Long accountId, String description);
}
