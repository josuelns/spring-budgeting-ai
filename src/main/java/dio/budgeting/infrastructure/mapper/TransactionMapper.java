package dio.budgeting.infrastructure.mapper;

import dio.budgeting.application.output.TransactionDto;
import dio.budgeting.domain.BankTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper gerado em compile-time pelo MapStruct.
 * Converte o domain record BankTransaction para o DTO de saída TransactionDto.
 *
 * <p>componentModel = "spring" → o bean gerado é gerenciado pelo Spring IoC
 * e pode ser injetado normalmente via @Autowired / construtor.
 *
 * <p>O campo transactionType é enum no domínio e String no DTO.
 * A expressão java() chama .name() para obter o nome literal do enum.
 */
@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "transactionType",
             expression = "java(transaction.transactionType().name())")
    TransactionDto toDto(BankTransaction transaction);

    List<TransactionDto> toDto(List<BankTransaction> transactions);
}
