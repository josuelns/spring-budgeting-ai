package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.infrastructure.persistence.entity.BankTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankTransactionEntityRepository extends JpaRepository<BankTransactionEntity, Long> {

    List<BankTransactionEntity> findByAccountId(Long accountId);
}
