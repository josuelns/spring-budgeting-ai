package dio.budgeting.domain;

/**
 * Exceção de negócio lançada quando o saldo é insuficiente para saque/transferência.
 * Não estende RuntimeException de infraestrutura — é uma regra de domínio pura.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
