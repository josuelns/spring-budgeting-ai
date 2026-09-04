package dio.budgeting.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Entidade de domínio puro representando uma instituição bancária.
 * Nenhuma anotação de infraestrutura — Clean Architecture.
 */
@Getter
@AllArgsConstructor
public class Bank {
    private Long id;
    private String code;   // ex: "001"
    private String name;   // ex: "Banco do Brasil"
}
