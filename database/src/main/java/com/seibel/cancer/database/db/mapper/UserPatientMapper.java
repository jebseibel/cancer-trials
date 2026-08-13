package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.UserPatient;
import com.seibel.cancer.database.db.entity.UserPatientDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class UserPatientMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public UserPatient toModel(UserPatientDb item) {
        return modelMapper.map(item, UserPatient.class);
    }

    public UserPatientDb toDb(UserPatient item) {
        return modelMapper.map(item, UserPatientDb.class);
    }

    public List<UserPatient> toModelList(List<UserPatientDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<UserPatientDb> toDbList(List<UserPatient> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
