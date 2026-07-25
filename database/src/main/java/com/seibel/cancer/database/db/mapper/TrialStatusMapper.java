package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.TrialStatus;
import com.seibel.cancer.database.db.entity.TrialStatusDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class TrialStatusMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public TrialStatus toModel(TrialStatusDb item) {
        return modelMapper.map(item, TrialStatus.class);
    }

    public TrialStatusDb toDb(TrialStatus item) {
        return modelMapper.map(item, TrialStatusDb.class);
    }

    public List<TrialStatus> toModelList(List<TrialStatusDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<TrialStatusDb> toDbList(List<TrialStatus> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
