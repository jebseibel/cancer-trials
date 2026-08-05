package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.StagingRawFhirResource;
import com.seibel.cancer.database.db.entity.StagingRawFhirResourceDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class StagingRawFhirResourceMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public StagingRawFhirResource toModel(StagingRawFhirResourceDb item) {
        return modelMapper.map(item, StagingRawFhirResource.class);
    }

    public StagingRawFhirResourceDb toDb(StagingRawFhirResource item) {
        return modelMapper.map(item, StagingRawFhirResourceDb.class);
    }

    public List<StagingRawFhirResource> toModelList(List<StagingRawFhirResourceDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<StagingRawFhirResourceDb> toDbList(List<StagingRawFhirResource> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
