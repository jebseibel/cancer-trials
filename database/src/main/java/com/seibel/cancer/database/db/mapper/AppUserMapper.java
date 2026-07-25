package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.AppUser;
import com.seibel.cancer.database.db.entity.AppUserDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class AppUserMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public AppUser toModel(AppUserDb item) {
        return modelMapper.map(item, AppUser.class);
    }

    public AppUserDb toDb(AppUser item) {
        return modelMapper.map(item, AppUserDb.class);
    }

    public List<AppUser> toModelList(List<AppUserDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<AppUserDb> toDbList(List<AppUser> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
