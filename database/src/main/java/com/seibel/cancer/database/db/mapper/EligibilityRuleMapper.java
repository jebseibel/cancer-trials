package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.EligibilityRule;
import com.seibel.cancer.database.db.entity.EligibilityRuleDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class EligibilityRuleMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public EligibilityRule toModel(EligibilityRuleDb item) {
        return modelMapper.map(item, EligibilityRule.class);
    }

    public EligibilityRuleDb toDb(EligibilityRule item) {
        return modelMapper.map(item, EligibilityRuleDb.class);
    }

    public List<EligibilityRule> toModelList(List<EligibilityRuleDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<EligibilityRuleDb> toDbList(List<EligibilityRule> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
