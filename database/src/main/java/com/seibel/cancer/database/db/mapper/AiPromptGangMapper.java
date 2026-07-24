package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.domain.AiPromptGang;
import com.seibel.cancer.database.db.entity.ai.AiPromptGangDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class AiPromptGangMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public AiPromptGang toModel(AiPromptGangDb item) {
        return modelMapper.map(item, AiPromptGang.class);
    }

    public AiPromptGangDb toDb(AiPromptGang item) {
        return modelMapper.map(item, AiPromptGangDb.class);
    }

    public List<AiPromptGang> toModelList(List<AiPromptGangDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<AiPromptGangDb> toDbList(List<AiPromptGang> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
