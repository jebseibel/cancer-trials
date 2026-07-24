package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Purchase;
import com.seibel.cancer.database.db.entity.PurchaseDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class PurchaseMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Purchase toModel(PurchaseDb item) {
        return modelMapper.map(item, Purchase.class);
    }

    public PurchaseDb toDb(Purchase item) {
        return modelMapper.map(item, PurchaseDb.class);
    }

    public List<Purchase> toModelList(List<PurchaseDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<PurchaseDb> toDbList(List<Purchase> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
