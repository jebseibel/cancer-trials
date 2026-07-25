package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Condition;
import com.seibel.cancer.database.db.entity.ConditionDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class ConditionMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Condition toModel(ConditionDb item) {
        return modelMapper.map(item, Condition.class);
    }

    public ConditionDb toDb(Condition item) {
        return modelMapper.map(item, ConditionDb.class);
    }

    public List<Condition> toModelList(List<ConditionDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<ConditionDb> toDbList(List<Condition> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
