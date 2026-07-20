package com.seibel.jobhunting.database.db.mapper;

import com.seibel.jobhunting.common.domain.FriendSkill;
import com.seibel.jobhunting.database.db.entity.FriendSkillDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class FriendSkillMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public FriendSkill toModel(FriendSkillDb item) {
        return modelMapper.map(item, FriendSkill.class);
    }

    public FriendSkillDb toDb(FriendSkill item) {
        return modelMapper.map(item, FriendSkillDb.class);
    }

    public List<FriendSkill> toModelList(List<FriendSkillDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<FriendSkillDb> toDbList(List<FriendSkill> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
