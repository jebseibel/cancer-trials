package com.seibel.jobs.database.db.mapper;

import com.seibel.jobs.common.domain.FriendJobPosting;
import com.seibel.jobs.database.db.entity.FriendJobPostingDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class FriendJobPostingMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public FriendJobPosting toModel(FriendJobPostingDb item) {
        return modelMapper.map(item, FriendJobPosting.class);
    }

    public FriendJobPostingDb toDb(FriendJobPosting item) {
        return modelMapper.map(item, FriendJobPostingDb.class);
    }

    public List<FriendJobPosting> toModelList(List<FriendJobPostingDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<FriendJobPostingDb> toDbList(List<FriendJobPosting> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
