package com.seibel.jobs.database.converter;

import com.seibel.jobs.common.enums.ActiveEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ActiveEnumConverter implements AttributeConverter<ActiveEnum, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ActiveEnum attribute) {
        return attribute != null ? attribute.value : null;
    }

    @Override
    public ActiveEnum convertToEntityAttribute(Integer dbData) {
        if (dbData == null) return null;
        for (ActiveEnum status : ActiveEnum.values()) {
            if (status.value == dbData) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown database value for ActiveEnum: " + dbData);
    }
}
