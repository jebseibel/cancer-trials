package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.TrialSource;
import com.seibel.cancer.database.db.entity.TrialSourceDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class TrialSourceMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public TrialSource toModel(TrialSourceDb item) {
        return modelMapper.map(item, TrialSource.class);
    }

    public TrialSourceDb toDb(TrialSource item) {
        return modelMapper.map(item, TrialSourceDb.class);
    }

    public List<TrialSource> toModelList(List<TrialSourceDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<TrialSourceDb> toDbList(List<TrialSource> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
