package com.seibel.jobs.database.db.mapper;

import com.seibel.jobs.common.domain.Friend;
import com.seibel.jobs.database.db.entity.FriendDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class FriendMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Friend toModel(FriendDb item) {
        return modelMapper.map(item, Friend.class);
    }

    public FriendDb toDb(Friend item) {
        return modelMapper.map(item, FriendDb.class);
    }

    public List<Friend> toModelList(List<FriendDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<FriendDb> toDbList(List<Friend> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
