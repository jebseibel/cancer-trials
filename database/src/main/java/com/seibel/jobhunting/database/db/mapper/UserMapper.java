package com.seibel.jobhunting.database.db.mapper;

import com.seibel.jobhunting.common.domain.User;
import com.seibel.jobhunting.database.db.entity.UserDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class UserMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public User toModel(UserDb item) {
        return modelMapper.map(item, User.class);
    }

    public UserDb toDb(User item) {
        return modelMapper.map(item, UserDb.class);
    }

    public List<User> toModelList(List<UserDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<UserDb> toDbList(List<User> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
