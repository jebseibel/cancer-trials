package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Outcome;
import com.seibel.cancer.database.db.entity.OutcomeDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class OutcomeMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Outcome toModel(OutcomeDb item) {
        return modelMapper.map(item, Outcome.class);
    }

    public OutcomeDb toDb(Outcome item) {
        return modelMapper.map(item, OutcomeDb.class);
    }

    public List<Outcome> toModelList(List<OutcomeDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<OutcomeDb> toDbList(List<Outcome> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
