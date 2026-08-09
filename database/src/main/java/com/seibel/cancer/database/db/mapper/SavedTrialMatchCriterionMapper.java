package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.SavedTrialMatchCriterion;
import com.seibel.cancer.database.db.entity.SavedTrialMatchCriterionDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class SavedTrialMatchCriterionMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public SavedTrialMatchCriterion toModel(SavedTrialMatchCriterionDb item) {
        return modelMapper.map(item, SavedTrialMatchCriterion.class);
    }

    public SavedTrialMatchCriterionDb toDb(SavedTrialMatchCriterion item) {
        return modelMapper.map(item, SavedTrialMatchCriterionDb.class);
    }

    public List<SavedTrialMatchCriterion> toModelList(List<SavedTrialMatchCriterionDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<SavedTrialMatchCriterionDb> toDbList(List<SavedTrialMatchCriterion> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
