package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.UcHealthOAuthToken;
import com.seibel.cancer.database.db.entity.UcHealthOAuthTokenDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class UcHealthOAuthTokenMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public UcHealthOAuthToken toModel(UcHealthOAuthTokenDb item) {
        return modelMapper.map(item, UcHealthOAuthToken.class);
    }

    public UcHealthOAuthTokenDb toDb(UcHealthOAuthToken item) {
        return modelMapper.map(item, UcHealthOAuthTokenDb.class);
    }

    public List<UcHealthOAuthToken> toModelList(List<UcHealthOAuthTokenDb> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toModel).toList();
    }

    public List<UcHealthOAuthTokenDb> toDbList(List<UcHealthOAuthToken> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toDb).toList();
    }
}
