package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.AiPrompt;
import com.seibel.cancer.database.db.entity.ai.AiPromptDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class AiPromptMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public AiPrompt toModel(AiPromptDb item) {
        AiPrompt model = modelMapper.map(item, AiPrompt.class);
        if (item.getEnvelope() != null) {
            model.setEnvelopeExtid(item.getEnvelope().getExtid());
            model.setEnvelopeName(item.getEnvelope().getName());
        }
        return model;
    }

    public AiPromptDb toDb(AiPrompt item) {
        return modelMapper.map(item, AiPromptDb.class);
    }

    public List<AiPrompt> toModelList(List<AiPromptDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<AiPromptDb> toDbList(List<AiPrompt> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
