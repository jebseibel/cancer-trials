package com.seibel.jobhunting.database.db.mapper;

import com.seibel.jobhunting.common.domain.Skill;
import com.seibel.jobhunting.database.db.entity.SkillDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class SkillMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Skill toModel(SkillDb item) {
        return modelMapper.map(item, Skill.class);
    }

    public SkillDb toDb(Skill item) {
        return modelMapper.map(item, SkillDb.class);
    }

    public List<Skill> toModelList(List<SkillDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<SkillDb> toDbList(List<Skill> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
