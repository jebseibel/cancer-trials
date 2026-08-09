package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.database.db.entity.PatientPriorTreatmentDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class PatientPriorTreatmentMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public PatientPriorTreatment toModel(PatientPriorTreatmentDb item) {
        return modelMapper.map(item, PatientPriorTreatment.class);
    }

    public PatientPriorTreatmentDb toDb(PatientPriorTreatment item) {
        return modelMapper.map(item, PatientPriorTreatmentDb.class);
    }

    public List<PatientPriorTreatment> toModelList(List<PatientPriorTreatmentDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<PatientPriorTreatmentDb> toDbList(List<PatientPriorTreatment> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
