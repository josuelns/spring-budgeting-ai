package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.infrastructure.persistence.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface AccountEntityRepository extends JpaRepository<AccountEntity, Long> {

    /**
     * Busca a conta com banco associado via JOIN FETCH (evita N+1 query).
     * Usado em consultas normais onde não há contenção de escrita.
     */
    @Query("SELECT a FROM AccountEntity a JOIN FETCH a.bank WHERE a.keycloakUserId = :userId")
    Optional<AccountEntity> findByKeycloakUserId(@Param("userId") String userId);

    /**
     * Lock Pessimista (SELECT ... FOR UPDATE no MySQL).
     * Garante consistência ACID em saques/transferências simultâneas — anti double-spending.
     * Deve ser chamado dentro de uma transação @Transactional.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a JOIN FETCH a.bank WHERE a.keycloakUserId = :userId")
    Optional<AccountEntity> findByKeycloakUserIdWithLock(@Param("userId") String userId);

    @Query("SELECT a FROM AccountEntity a JOIN FETCH a.bank WHERE a.accountNumber = :accountNumber")
    Optional<AccountEntity> findByAccountNumber(@Param("accountNumber") String accountNumber);

    /**
     * Lock Pessimista na conta de destino em uma transferência.
     * Deve ser usado em conjunto com findByKeycloakUserIdWithLock, SEMPRE em ordem crescente de ID
     * para evitar deadlock entre duas transferências inversas simultâneas (A→B e B→A).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a JOIN FETCH a.bank WHERE a.accountNumber = :accountNumber")
    Optional<AccountEntity> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    /**
     * UPDATE parcial e atômico — somente o saldo é alterado.
     * Evita race condition com o @OneToMany de transações (sem cascade).
     */
    @Modifying
    @Query("UPDATE AccountEntity a SET a.balance = :balance WHERE a.id = :id")
    void updateBalance(@Param("id") Long id, @Param("balance") BigDecimal balance);
}
