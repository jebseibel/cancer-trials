package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.AiTrialAssessment;
import com.seibel.cancer.database.db.entity.AiTrialAssessmentDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class AiTrialAssessmentMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public AiTrialAssessment toModel(AiTrialAssessmentDb item) {
        return item == null ? null : modelMapper.map(item, AiTrialAssessment.class);
    }

    public AiTrialAssessmentDb toDb(AiTrialAssessment item) {
        return item == null ? null : modelMapper.map(item, AiTrialAssessmentDb.class);
    }

    public List<AiTrialAssessment> toModelList(List<AiTrialAssessmentDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }
}
