package com.seibel.cancer.database.converter;

import com.seibel.cancer.common.enums.ai.AiLifecycle;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AiLifecycleConverter implements AttributeConverter<AiLifecycle, String> {

    @Override
    public String convertToDatabaseColumn(AiLifecycle attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public AiLifecycle convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return AiLifecycle.valueOf(dbData);
    }
}
