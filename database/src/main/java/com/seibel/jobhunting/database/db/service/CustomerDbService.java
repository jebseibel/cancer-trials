package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.Customer;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.CustomerDb;
import com.seibel.jobhunting.database.db.mapper.CustomerMapper;
import com.seibel.jobhunting.database.db.repository.CustomerRepository;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CustomerDbService extends BaseDbService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public CustomerDbService(CustomerRepository repository, CustomerMapper mapper) {
        super("CustomerDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Customer create(@NonNull Customer item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            CustomerDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            CustomerDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public Customer create(@NonNull String code, @NonNull String name, @NonNull String contactName, @NonNull String description, @NonNull String contactEmail, @NonNull String contactPhone) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            CustomerDb record = new CustomerDb();
            record.setExtid(extid);
            record.setCode(code);
            record.setName(name);
            record.setContactName(contactName);
            record.setDescription(description);
            record.setContactEmail(contactEmail);
            record.setContactPhone(contactPhone);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            CustomerDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Customer update(@NonNull String extid, String code, String name, String contactName, String description, String contactEmail, String contactPhone) {

        CustomerDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (code != null) record.setCode(code);
            if (name != null) record.setName(name);
            if (contactName != null) record.setContactName(contactName);
            if (description != null) record.setDescription(description);
            if (contactEmail != null) record.setContactEmail(contactEmail);
            if (contactPhone != null) record.setContactPhone(contactPhone);
            record.setUpdatedAt(LocalDateTime.now());

            CustomerDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        CustomerDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            record.setDeletedAt(LocalDateTime.now());
            record.setActive(ActiveEnum.INACTIVE);

            repository.save(record);
            log.info(getDeletedMessage(extid));
            return true;

        } catch (Exception e) {
            handleException("delete", extid, e);
            return false; // unreachable
        }
    }

    public Customer findByExtid(@NonNull String extid) {
        CustomerDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<Customer> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<Customer> findAll(Pageable pageable) {
        Page<CustomerDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Customer> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<Customer> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<CustomerDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<Customer> findAndLog(List<CustomerDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
