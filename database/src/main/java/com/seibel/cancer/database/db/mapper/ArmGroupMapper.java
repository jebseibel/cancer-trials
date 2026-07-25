package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.ArmGroup;
import com.seibel.cancer.database.db.entity.ArmGroupDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class ArmGroupMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public ArmGroup toModel(ArmGroupDb item) {
        return modelMapper.map(item, ArmGroup.class);
    }

    public ArmGroupDb toDb(ArmGroup item) {
        return modelMapper.map(item, ArmGroupDb.class);
    }

    public List<ArmGroup> toModelList(List<ArmGroupDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<ArmGroupDb> toDbList(List<ArmGroup> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
