package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Sponsor;
import com.seibel.cancer.database.db.entity.SponsorDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class SponsorMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Sponsor toModel(SponsorDb item) {
        return modelMapper.map(item, Sponsor.class);
    }

    public SponsorDb toDb(Sponsor item) {
        return modelMapper.map(item, SponsorDb.class);
    }

    public List<Sponsor> toModelList(List<SponsorDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<SponsorDb> toDbList(List<Sponsor> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
