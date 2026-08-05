package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.PatientMedication;
import com.seibel.cancer.database.db.entity.PatientMedicationDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class PatientMedicationMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public PatientMedication toModel(PatientMedicationDb item) {
        return modelMapper.map(item, PatientMedication.class);
    }

    public PatientMedicationDb toDb(PatientMedication item) {
        return modelMapper.map(item, PatientMedicationDb.class);
    }

    public List<PatientMedication> toModelList(List<PatientMedicationDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<PatientMedicationDb> toDbList(List<PatientMedication> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
