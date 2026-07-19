package com.seibel.jobs.database.db.mapper;

import com.seibel.jobs.common.domain.Contact;
import com.seibel.jobs.database.db.entity.ContactDb;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class ContactMapper {

    private final ModelMapper modelMapper = new ModelMapper();

    public Contact toModel(ContactDb item) {
        return modelMapper.map(item, Contact.class);
    }

    public ContactDb toDb(Contact item) {
        return modelMapper.map(item, ContactDb.class);
    }

    public List<Contact> toModelList(List<ContactDb> items) {
        if (items == null) return null;
        return items.stream().map(this::toModel).toList();
    }

    public List<ContactDb> toDbList(List<Contact> items) {
        if (items == null) return null;
        return items.stream().map(this::toDb).toList();
    }
}
