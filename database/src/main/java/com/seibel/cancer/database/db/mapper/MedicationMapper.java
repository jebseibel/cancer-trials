package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Medication;
import com.seibel.cancer.database.db.entity.MedicationDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class MedicationMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Medication toModel(MedicationDb item) {
        return modelMapper.map(item, Medication.class);
    }

    public MedicationDb toDb(Medication item) {
        return modelMapper.map(item, MedicationDb.class);
    }

    public List<Medication> toModelList(List<MedicationDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<MedicationDb> toDbList(List<Medication> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
