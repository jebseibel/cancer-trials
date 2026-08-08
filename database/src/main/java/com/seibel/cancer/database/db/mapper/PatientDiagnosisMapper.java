package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.database.db.entity.PatientDiagnosisDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class PatientDiagnosisMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public PatientDiagnosis toModel(PatientDiagnosisDb item) {
        return modelMapper.map(item, PatientDiagnosis.class);
    }

    public PatientDiagnosisDb toDb(PatientDiagnosis item) {
        return modelMapper.map(item, PatientDiagnosisDb.class);
    }

    public List<PatientDiagnosis> toModelList(List<PatientDiagnosisDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<PatientDiagnosisDb> toDbList(List<PatientDiagnosis> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
