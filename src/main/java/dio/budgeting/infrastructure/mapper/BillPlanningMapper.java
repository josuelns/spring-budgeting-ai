package dio.budgeting.infrastructure.mapper;

import dio.budgeting.application.output.BillPlanningDto;
import dio.budgeting.domain.BillPlanning;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper gerado em compile-time pelo MapStruct.
 * Converte o domain BillPlanning para o DTO BillPlanningDto.
 *
 * <p>status  → enum BillPlanningStatus → String via .name()
 * <p>dueDate → LocalDate              → String via .toString() (formato ISO: yyyy-MM-dd)
 */
@Mapper(componentModel = "spring")
public interface BillPlanningMapper {

    @Mapping(target = "status",
             expression = "java(billPlanning.getStatus().name())")
    @Mapping(target = "dueDate",
             expression = "java(billPlanning.getDueDate().toString())")
    BillPlanningDto toDto(BillPlanning billPlanning);

    List<BillPlanningDto> toDto(List<BillPlanning> billPlannings);
}
