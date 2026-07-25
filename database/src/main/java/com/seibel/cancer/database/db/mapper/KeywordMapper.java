package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Keyword;
import com.seibel.cancer.database.db.entity.KeywordDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class KeywordMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Keyword toModel(KeywordDb item) {
        return modelMapper.map(item, Keyword.class);
    }

    public KeywordDb toDb(Keyword item) {
        return modelMapper.map(item, KeywordDb.class);
    }

    public List<Keyword> toModelList(List<KeywordDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<KeywordDb> toDbList(List<Keyword> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
