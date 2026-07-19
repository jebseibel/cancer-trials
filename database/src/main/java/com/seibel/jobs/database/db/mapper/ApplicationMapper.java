package com.seibel.jobs.database.db.mapper;

import com.seibel.jobs.common.domain.Application;
import com.seibel.jobs.database.db.entity.ApplicationDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class ApplicationMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Application toModel(ApplicationDb item) {
        return modelMapper.map(item, Application.class);
    }

    public ApplicationDb toDb(Application item) {
        return modelMapper.map(item, ApplicationDb.class);
    }

    public List<Application> toModelList(List<ApplicationDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<ApplicationDb> toDbList(List<Application> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
