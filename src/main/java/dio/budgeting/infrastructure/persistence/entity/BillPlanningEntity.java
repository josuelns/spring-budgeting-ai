package dio.budgeting.infrastructure.persistence.entity;

import dio.budgeting.domain.BillPlanning;
import dio.budgeting.domain.BillPlanningStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bill_planning")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "account")
public class BillPlanningEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(nullable = false)
    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BillPlanningStatus status;

    // -------------------------------------------------------
    // Conversões Domain ↔ Entity
    // -------------------------------------------------------

    public BillPlanning toDomain() {
        return new BillPlanning(
                this.id,
                this.account.getId(),
                this.description,
                this.dueDate,
                this.amount,
                this.status
        );
    }

    public static BillPlanningEntity from(BillPlanning bill, AccountEntity account) {
        return new BillPlanningEntity(
                bill.getId(),
                account,
                bill.getDescription(),
                bill.getDueDate(),
                bill.getAmount(),
                bill.getStatus()
        );
    }
}
