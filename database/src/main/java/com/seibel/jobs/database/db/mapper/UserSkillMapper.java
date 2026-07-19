package com.seibel.jobs.database.db.mapper;

import com.seibel.jobs.common.domain.UserSkill;
import com.seibel.jobs.database.db.entity.UserSkillDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class UserSkillMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public UserSkill toModel(UserSkillDb item) {
        return modelMapper.map(item, UserSkill.class);
    }

    public UserSkillDb toDb(UserSkill item) {
        return modelMapper.map(item, UserSkillDb.class);
    }

    public List<UserSkill> toModelList(List<UserSkillDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<UserSkillDb> toDbList(List<UserSkill> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
