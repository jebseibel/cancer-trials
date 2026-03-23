package com.seibel.basic.database.db.service;

import com.seibel.basic.common.domain.Purchase;
import com.seibel.basic.common.enums.ActiveEnum;
import com.seibel.basic.database.db.entity.PurchaseDb;
import com.seibel.basic.database.db.mapper.PurchaseMapper;
import com.seibel.basic.database.db.repository.PurchaseRepository;
import com.seibel.basic.common.exceptions.ServiceException;
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
public class PurchaseDbService extends BaseDbService {

    private final PurchaseRepository repository;
    private final PurchaseMapper mapper;

    public PurchaseDbService(PurchaseRepository repository, PurchaseMapper mapper) {
        super("PurchaseDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Purchase create(@NonNull Purchase item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            PurchaseDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            PurchaseDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public Purchase create(@NonNull String customer, @NonNull String items, @NonNull String status) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            PurchaseDb record = new PurchaseDb();
            record.setExtid(extid);
            record.setCustomer(customer);
            record.setItems(items);
            record.setStatus(status);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            PurchaseDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Purchase update(@NonNull String extid, String customer, String items, String status) {

        PurchaseDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (customer != null) record.setCustomer(customer);
            if (items != null) record.setItems(items);
            if (status != null) record.setStatus(status);
            record.setUpdatedAt(LocalDateTime.now());

            PurchaseDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        PurchaseDb record = repository.findByExtid(extid)
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

    public Purchase findByExtid(@NonNull String extid) {
        PurchaseDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<Purchase> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<Purchase> findAll(Pageable pageable) {
        Page<PurchaseDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Purchase> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<Purchase> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<PurchaseDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<Purchase> findAndLog(List<PurchaseDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
