package com.seibel.jobhunting.database.db.mapper;

import com.seibel.jobhunting.common.domain.JobPosting;
import com.seibel.jobhunting.database.db.entity.JobPostingDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class JobPostingMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public JobPosting toModel(JobPostingDb item) {
        return modelMapper.map(item, JobPosting.class);
    }

    public JobPostingDb toDb(JobPosting item) {
        return modelMapper.map(item, JobPostingDb.class);
    }

    public List<JobPosting> toModelList(List<JobPostingDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<JobPostingDb> toDbList(List<JobPosting> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
