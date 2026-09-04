package dio.budgeting.domain;

import java.util.Optional;

/**
 * Port (contrato) para persistência de contas bancárias.
 * A implementação concreta fica na camada de infraestrutura (JPA).
 */
public interface AccountRepository {

    Optional<Account> findByKeycloakUserId(String keycloakUserId);

    /** Busca com Lock Pessimista — previne double-spending em saques simultâneos. */
    Optional<Account> findByKeycloakUserIdWithLock(String keycloakUserId);

    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Busca conta de destino de transferência com Lock Pessimista.
     * SEMPRE adquira locks em ordem crescente de ID para evitar deadlock.
     */
    Optional<Account> findByAccountNumberWithLock(String accountNumber);

    /** Persiste apenas os campos escalares da conta (balance). As transações são salvas via BankTransactionRepository. */
    Account save(Account account);
}
