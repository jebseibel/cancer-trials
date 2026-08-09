package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.database.db.entity.PatientVariantDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class PatientVariantMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public PatientVariant toModel(PatientVariantDb item) {
        return modelMapper.map(item, PatientVariant.class);
    }

    public PatientVariantDb toDb(PatientVariant item) {
        return modelMapper.map(item, PatientVariantDb.class);
    }

    public List<PatientVariant> toModelList(List<PatientVariantDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<PatientVariantDb> toDbList(List<PatientVariant> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
