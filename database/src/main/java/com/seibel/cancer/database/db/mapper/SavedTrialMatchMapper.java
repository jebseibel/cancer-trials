package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.SavedTrialMatch;
import com.seibel.cancer.database.db.entity.SavedTrialMatchDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class SavedTrialMatchMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public SavedTrialMatch toModel(SavedTrialMatchDb item) {
        return modelMapper.map(item, SavedTrialMatch.class);
    }

    public SavedTrialMatchDb toDb(SavedTrialMatch item) {
        return modelMapper.map(item, SavedTrialMatchDb.class);
    }

    public List<SavedTrialMatch> toModelList(List<SavedTrialMatchDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<SavedTrialMatchDb> toDbList(List<SavedTrialMatch> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
