package dio.budgeting.infrastructure.persistence.entity;

import dio.budgeting.domain.Bank;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "banks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    private String name;

    // -------------------------------------------------------
    // Conversões Domain ↔ Entity (sem dependência de framework)
    // -------------------------------------------------------

    public Bank toDomain() {
        return new Bank(this.id, this.code, this.name);
    }

    public static BankEntity from(Bank bank) {
        return new BankEntity(bank.getId(), bank.getCode(), bank.getName());
    }
}
