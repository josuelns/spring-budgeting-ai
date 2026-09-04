package dio.budgeting.domain;

import java.util.List;

/**
 * Port para persistência de transações bancárias.
 * Separado de AccountRepository para evitar cascade JPA e
 * garantir controle explícito sobre cada transação gerada.
 */
public interface BankTransactionRepository {

    BankTransaction save(BankTransaction transaction, Long accountId);

    List<BankTransaction> findByAccountId(Long accountId);
}
