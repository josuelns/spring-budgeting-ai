package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.infrastructure.persistence.entity.BankEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankEntityRepository extends JpaRepository<BankEntity, Long> {
}
