package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.LabResultComponent;
import com.seibel.cancer.database.db.entity.LabResultComponentDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class LabResultComponentMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public LabResultComponent toModel(LabResultComponentDb item) {
        return modelMapper.map(item, LabResultComponent.class);
    }

    public LabResultComponentDb toDb(LabResultComponent item) {
        return modelMapper.map(item, LabResultComponentDb.class);
    }

    public List<LabResultComponent> toModelList(List<LabResultComponentDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<LabResultComponentDb> toDbList(List<LabResultComponent> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
