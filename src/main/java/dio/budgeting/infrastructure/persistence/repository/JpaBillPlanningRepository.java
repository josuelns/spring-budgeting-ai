package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.BillPlanning;
import dio.budgeting.domain.BillPlanningRepository;
import dio.budgeting.domain.BillPlanningStatus;
import dio.budgeting.infrastructure.persistence.entity.BillPlanningEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA que implementa o port BillPlanningRepository do domínio.
 * Persiste contas agendadas explicitamente — sem cascade na AccountEntity.
 */
@Repository
public class JpaBillPlanningRepository implements BillPlanningRepository {

    private final BillPlanningEntityRepository billPlanningEntityRepository;
    private final AccountEntityRepository accountEntityRepository;

    public JpaBillPlanningRepository(BillPlanningEntityRepository billPlanningEntityRepository,
                                     AccountEntityRepository accountEntityRepository) {
        this.billPlanningEntityRepository = billPlanningEntityRepository;
        this.accountEntityRepository = accountEntityRepository;
    }

    @Override
    public BillPlanning save(BillPlanning billPlanning) {
        // getReferenceById cria proxy sem query extra — suficiente para a FK
        var accountRef = accountEntityRepository.getReferenceById(billPlanning.getAccountId());
        var entity = BillPlanningEntity.from(billPlanning, accountRef);
        return billPlanningEntityRepository.save(entity).toDomain();
    }

    @Override
    public List<BillPlanning> findByAccountId(Long accountId) {
        return billPlanningEntityRepository.findByAccountId(accountId).stream()
                .map(BillPlanningEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<BillPlanning> findPendingByAccountIdAndDescription(Long accountId, String description) {
        return billPlanningEntityRepository
                .findPendingByAccountIdAndDescription(accountId, description, BillPlanningStatus.PENDING)
                .map(BillPlanningEntity::toDomain);
    }
}
