package com.seibel.jobhunting.database.db.mapper;

import com.seibel.jobhunting.common.domain.FriendCompany;
import com.seibel.jobhunting.database.db.entity.FriendCompanyDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class FriendCompanyMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public FriendCompany toModel(FriendCompanyDb item) {
        return modelMapper.map(item, FriendCompany.class);
    }

    public FriendCompanyDb toDb(FriendCompany item) {
        return modelMapper.map(item, FriendCompanyDb.class);
    }

    public List<FriendCompany> toModelList(List<FriendCompanyDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<FriendCompanyDb> toDbList(List<FriendCompany> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
