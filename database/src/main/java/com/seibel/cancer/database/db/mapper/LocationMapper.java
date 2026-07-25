package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.database.db.entity.LocationDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class LocationMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Location toModel(LocationDb item) {
        return modelMapper.map(item, Location.class);
    }

    public LocationDb toDb(Location item) {
        return modelMapper.map(item, LocationDb.class);
    }

    public List<Location> toModelList(List<LocationDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<LocationDb> toDbList(List<Location> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
