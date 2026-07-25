package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.database.db.entity.TrialDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class TrialMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Trial toModel(TrialDb item) {
        return modelMapper.map(item, Trial.class);
    }

    public TrialDb toDb(Trial item) {
        return modelMapper.map(item, TrialDb.class);
    }

    public List<Trial> toModelList(List<TrialDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<TrialDb> toDbList(List<Trial> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
