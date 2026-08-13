package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Patient;
import com.seibel.cancer.database.db.entity.PatientDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class PatientMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Patient toModel(PatientDb item) {
        return modelMapper.map(item, Patient.class);
    }

    public PatientDb toDb(Patient item) {
        return modelMapper.map(item, PatientDb.class);
    }

    public List<Patient> toModelList(List<PatientDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<PatientDb> toDbList(List<Patient> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
