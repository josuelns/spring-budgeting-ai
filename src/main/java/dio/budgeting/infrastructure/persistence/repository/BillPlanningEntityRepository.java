package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.BillPlanningStatus;
import dio.budgeting.infrastructure.persistence.entity.BillPlanningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BillPlanningEntityRepository extends JpaRepository<BillPlanningEntity, Long> {

    /** Lista todas as contas agendadas (PENDING e PAID) de uma conta bancária. */
    @Query("SELECT b FROM BillPlanningEntity b WHERE b.account.id = :accountId ORDER BY b.dueDate")
    List<BillPlanningEntity> findByAccountId(@Param("accountId") Long accountId);

    /**
     * Busca a primeira conta PENDENTE pela descrição (case-insensitive).
     * Usada pelo payBill para localizar o boleto via comando de voz.
     */
    @Query("""
            SELECT b FROM BillPlanningEntity b
            WHERE b.account.id = :accountId
              AND LOWER(b.description) = LOWER(:description)
              AND b.status = :status
            ORDER BY b.dueDate
            LIMIT 1
            """)
    Optional<BillPlanningEntity> findPendingByAccountIdAndDescription(
            @Param("accountId") Long accountId,
            @Param("description") String description,
            @Param("status") BillPlanningStatus status);
}
