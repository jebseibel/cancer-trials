package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.Contact;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.ContactDb;
import com.seibel.jobhunting.database.db.mapper.ContactMapper;
import com.seibel.jobhunting.database.db.repository.ContactRepository;
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
public class ContactDbService extends BaseDbService {

    private final ContactRepository repository;
    private final ContactMapper mapper;

    public ContactDbService(ContactRepository repository, ContactMapper mapper) {
        super("ContactDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Contact create(@NonNull Contact item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            ContactDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            ContactDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public Contact create(Long companyId, Long jobPostingId, @NonNull String name, String role, String email, String phone, String notes) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            ContactDb record = new ContactDb();
            record.setExtid(extid);
            record.setCompanyId(companyId);
            record.setJobPostingId(jobPostingId);
            record.setName(name);
            record.setRole(role);
            record.setEmail(email);
            record.setPhone(phone);
            record.setNotes(notes);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            ContactDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Contact update(@NonNull String extid, Long companyId, Long jobPostingId, String name, String role, String email, String phone, String notes) {

        ContactDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (companyId != null) record.setCompanyId(companyId);
            if (jobPostingId != null) record.setJobPostingId(jobPostingId);
            if (name != null) record.setName(name);
            if (role != null) record.setRole(role);
            if (email != null) record.setEmail(email);
            if (phone != null) record.setPhone(phone);
            if (notes != null) record.setNotes(notes);
            record.setUpdatedAt(LocalDateTime.now());

            ContactDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        ContactDb record = repository.findByExtid(extid)
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

    public Contact findByExtid(@NonNull String extid) {
        ContactDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<Contact> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<Contact> findAll(Pageable pageable) {
        Page<ContactDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Contact> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<Contact> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<ContactDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Contact> findByCompanyId(@NonNull Long companyId) {
        return findAndLog(repository.findByCompanyId(companyId), String.format("companyId (%s)", companyId));
    }

    public List<Contact> findByJobPostingId(@NonNull Long jobPostingId) {
        return findAndLog(repository.findByJobPostingId(jobPostingId), String.format("jobPostingId (%s)", jobPostingId));
    }

    private List<Contact> findAndLog(List<ContactDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
