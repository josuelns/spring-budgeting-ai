package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.BankTransaction;
import dio.budgeting.domain.BankTransactionRepository;
import dio.budgeting.infrastructure.persistence.entity.BankTransactionEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Adaptador JPA que implementa o port BankTransactionRepository do domínio.
 * Persiste explicitamente cada transação bancária (sem cascade JPA),
 * garantindo controle total sobre o ciclo de vida das movimentações.
 */
@Repository
public class JpaBankTransactionRepository implements BankTransactionRepository {

    private final BankTransactionEntityRepository bankTransactionEntityRepository;
    private final AccountEntityRepository accountEntityRepository;

    public JpaBankTransactionRepository(BankTransactionEntityRepository bankTransactionEntityRepository,
                                        AccountEntityRepository accountEntityRepository) {
        this.bankTransactionEntityRepository = bankTransactionEntityRepository;
        this.accountEntityRepository = accountEntityRepository;
    }

    @Override
    public BankTransaction save(BankTransaction transaction, Long accountId) {
        // getReferenceById cria um proxy sem query extra — suficiente para a FK
        var accountRef = accountEntityRepository.getReferenceById(accountId);
        var entity = BankTransactionEntity.from(transaction, accountRef);
        return bankTransactionEntityRepository.save(entity).toDomain();
    }

    @Override
    public List<BankTransaction> findByAccountId(Long accountId) {
        return bankTransactionEntityRepository.findByAccountId(accountId).stream()
                .map(BankTransactionEntity::toDomain)
                .toList();
    }
}
