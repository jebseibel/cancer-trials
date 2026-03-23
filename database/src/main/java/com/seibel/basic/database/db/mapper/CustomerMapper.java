package com.seibel.basic.database.db.mapper;

import com.seibel.basic.common.domain.Customer;
import com.seibel.basic.database.db.entity.CustomerDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class CustomerMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Customer toModel(CustomerDb item) {
        return modelMapper.map(item, Customer.class);
    }

    public CustomerDb toDb(Customer item) {
        return modelMapper.map(item, CustomerDb.class);
    }

    public List<Customer> toModelList(List<CustomerDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<CustomerDb> toDbList(List<Customer> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
