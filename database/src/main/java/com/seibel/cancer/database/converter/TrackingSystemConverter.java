package com.seibel.cancer.database.converter;

import com.seibel.cancer.common.enums.TrackingSystem;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TrackingSystemConverter implements AttributeConverter<TrackingSystem, String> {

    @Override
    public String convertToDatabaseColumn(TrackingSystem attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public TrackingSystem convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return TrackingSystem.valueOf(dbData);
    }
}
