package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.domain.AiPromptEnvelope;
import com.seibel.cancer.database.db.entity.ai.AiPromptEnvelopeDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class AiPromptEnvelopeMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public AiPromptEnvelope toModel(AiPromptEnvelopeDb item) {
        AiPromptEnvelope model = modelMapper.map(item, AiPromptEnvelope.class);
        if (item.getGang() != null) {
            model.setGangExtid(item.getGang().getExtid());
            model.setGangName(item.getGang().getName());
        }
        return model;
    }

    public AiPromptEnvelopeDb toDb(AiPromptEnvelope item) {
        return modelMapper.map(item, AiPromptEnvelopeDb.class);
    }

    public List<AiPromptEnvelope> toModelList(List<AiPromptEnvelopeDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<AiPromptEnvelopeDb> toDbList(List<AiPromptEnvelope> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
