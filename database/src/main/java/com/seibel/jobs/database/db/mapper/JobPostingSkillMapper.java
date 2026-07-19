package com.seibel.jobs.database.db.mapper;

import com.seibel.jobs.common.domain.JobPostingSkill;
import com.seibel.jobs.database.db.entity.JobPostingSkillDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class JobPostingSkillMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public JobPostingSkill toModel(JobPostingSkillDb item) {
        return modelMapper.map(item, JobPostingSkill.class);
    }

    public JobPostingSkillDb toDb(JobPostingSkill item) {
        return modelMapper.map(item, JobPostingSkillDb.class);
    }

    public List<JobPostingSkill> toModelList(List<JobPostingSkillDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<JobPostingSkillDb> toDbList(List<JobPostingSkill> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
