package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.Account;
import dio.budgeting.domain.AccountRepository;
import dio.budgeting.infrastructure.persistence.entity.AccountEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador JPA que implementa o port AccountRepository do domínio.
 *
 * <p>O método save() usa UPDATE parcial (apenas balance) via JPQL para evitar
 * que o MERGE do Hibernate sobrescreva relacionamentos não carregados.
 */
@Repository
public class JpaAccountRepository implements AccountRepository {

    private final AccountEntityRepository accountEntityRepository;
    private final BankEntityRepository bankEntityRepository;

    public JpaAccountRepository(AccountEntityRepository accountEntityRepository,
                                BankEntityRepository bankEntityRepository) {
        this.accountEntityRepository = accountEntityRepository;
        this.bankEntityRepository = bankEntityRepository;
    }

    @Override
    public Optional<Account> findByKeycloakUserId(String keycloakUserId) {
        return accountEntityRepository.findByKeycloakUserId(keycloakUserId)
                .map(AccountEntity::toDomain);
    }

    @Override
    public Optional<Account> findByKeycloakUserIdWithLock(String keycloakUserId) {
        return accountEntityRepository.findByKeycloakUserIdWithLock(keycloakUserId)
                .map(AccountEntity::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return accountEntityRepository.findByAccountNumber(accountNumber)
                .map(AccountEntity::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumberWithLock(String accountNumber) {
        return accountEntityRepository.findByAccountNumberWithLock(accountNumber)
                .map(AccountEntity::toDomain);
    }

    /**
     * Atualiza apenas o saldo via JPQL para garantir uma operação atômica e segura.
     * Evita carregar novamente a entidade completa (SELECT + UPDATE) em operações com lock.
     */
    @Override
    @Transactional
    public Account save(Account account) {
        accountEntityRepository.updateBalance(account.getId(), account.getBalance());
        return account;
    }
}
