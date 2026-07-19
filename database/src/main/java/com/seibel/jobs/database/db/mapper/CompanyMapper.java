package com.seibel.jobs.database.db.mapper;

import com.seibel.jobs.common.domain.Company;
import com.seibel.jobs.database.db.entity.CompanyDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class CompanyMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Company toModel(CompanyDb item) {
        return modelMapper.map(item, Company.class);
    }

    public CompanyDb toDb(Company item) {
        return modelMapper.map(item, CompanyDb.class);
    }

    public List<Company> toModelList(List<CompanyDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<CompanyDb> toDbList(List<Company> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
