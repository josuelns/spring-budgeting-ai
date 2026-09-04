package dio.budgeting.infrastructure.persistence.entity;

import dio.budgeting.domain.BankTransaction;
import dio.budgeting.domain.BankTransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "account")
public class BankTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private BankTransactionType transactionType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // -------------------------------------------------------
    // Conversões Domain ↔ Entity
    // -------------------------------------------------------

    public BankTransaction toDomain() {
        return new BankTransaction(this.id, this.transactionType, this.amount, this.createdAt);
    }

    public static BankTransactionEntity from(BankTransaction transaction, AccountEntity account) {
        return new BankTransactionEntity(
                transaction.id(),
                account,
                transaction.transactionType(),
                transaction.amount(),
                transaction.createdAt()
        );
    }
}
