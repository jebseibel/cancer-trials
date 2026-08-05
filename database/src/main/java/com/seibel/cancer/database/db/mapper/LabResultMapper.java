package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.LabResult;
import com.seibel.cancer.database.db.entity.LabResultDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class LabResultMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public LabResult toModel(LabResultDb item) {
        return modelMapper.map(item, LabResult.class);
    }

    public LabResultDb toDb(LabResult item) {
        return modelMapper.map(item, LabResultDb.class);
    }

    public List<LabResult> toModelList(List<LabResultDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<LabResultDb> toDbList(List<LabResult> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
