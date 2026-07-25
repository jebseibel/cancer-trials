package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.OverallOfficial;
import com.seibel.cancer.database.db.entity.OverallOfficialDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class OverallOfficialMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public OverallOfficial toModel(OverallOfficialDb item) {
        return modelMapper.map(item, OverallOfficial.class);
    }

    public OverallOfficialDb toDb(OverallOfficial item) {
        return modelMapper.map(item, OverallOfficialDb.class);
    }

    public List<OverallOfficial> toModelList(List<OverallOfficialDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<OverallOfficialDb> toDbList(List<OverallOfficial> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
