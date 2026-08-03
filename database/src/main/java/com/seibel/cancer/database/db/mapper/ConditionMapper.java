package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Condition;
import com.seibel.cancer.database.db.entity.MedicalConditionDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class ConditionMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Condition toModel(MedicalConditionDb item) {
        return modelMapper.map(item, Condition.class);
    }

    public MedicalConditionDb toDb(Condition item) {
        return modelMapper.map(item, MedicalConditionDb.class);
    }

    public List<Condition> toModelList(List<MedicalConditionDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<MedicalConditionDb> toDbList(List<Condition> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
