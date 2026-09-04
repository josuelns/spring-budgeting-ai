package dio.budgeting.infrastructure.persistence.entity;

import dio.budgeting.domain.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade JPA da conta bancária.
 *
 * <p>Usa @Getter/@Setter explícitos (sem @Data) para evitar que o Lombok gere
 * toString/equals/hashCode incluindo proxies lazy do Hibernate, o que causaria
 * LazyInitializationException ou StackOverflowError em relacionamentos bidirecionais.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"bank", "transactions", "billPlannings"})
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /** Salário mensal informado pelo usuário — base para cálculo do comprometimento de renda. */
    @Column(name = "monthly_salary", precision = 19, scale = 2)
    private BigDecimal monthlySalary;

    @Column(name = "keycloak_user_id", unique = true, nullable = false)
    private String keycloakUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", nullable = false)
    private BankEntity bank;

    /**
     * Transações bancárias — sem cascade.
     * Persistidas explicitamente via BankTransactionRepository.
     */
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<BankTransactionEntity> transactions = new ArrayList<>();

    /**
     * Contas fixas agendadas — sem cascade.
     * Persistidas explicitamente via BillPlanningRepository.
     */
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<BillPlanningEntity> billPlannings = new ArrayList<>();

    // -------------------------------------------------------
    // Conversão Entity → Domain
    // -------------------------------------------------------

    /**
     * Converte para domínio. Deve ser chamado dentro de um contexto @Transactional,
     * pois inicializa o proxy lazy de BankEntity.
     */
    public Account toDomain() {
        return new Account(
                this.id,
                this.accountNumber,
                this.balance,
                this.monthlySalary,
                this.keycloakUserId,
                this.bank.toDomain()
        );
    }
}
