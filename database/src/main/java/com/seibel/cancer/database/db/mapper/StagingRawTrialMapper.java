package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.StagingRawTrial;
import com.seibel.cancer.database.db.entity.StagingRawTrialDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class StagingRawTrialMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public StagingRawTrial toModel(StagingRawTrialDb item) {
        return modelMapper.map(item, StagingRawTrial.class);
    }

    public StagingRawTrialDb toDb(StagingRawTrial item) {
        return modelMapper.map(item, StagingRawTrialDb.class);
    }

    public List<StagingRawTrial> toModelList(List<StagingRawTrialDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<StagingRawTrialDb> toDbList(List<StagingRawTrial> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
