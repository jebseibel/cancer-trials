package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Intervention;
import com.seibel.cancer.database.db.entity.InterventionDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class InterventionMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Intervention toModel(InterventionDb item) {
        return modelMapper.map(item, Intervention.class);
    }

    public InterventionDb toDb(Intervention item) {
        return modelMapper.map(item, InterventionDb.class);
    }

    public List<Intervention> toModelList(List<InterventionDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<InterventionDb> toDbList(List<Intervention> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
