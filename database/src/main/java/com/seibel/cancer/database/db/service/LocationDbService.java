package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.LocationDb;
import com.seibel.cancer.database.db.mapper.LocationMapper;
import com.seibel.cancer.database.db.repository.LocationRepository;
import com.seibel.cancer.common.exceptions.ServiceException;
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
public class LocationDbService extends BaseDbService {

    private final LocationRepository repository;
    private final LocationMapper mapper;

    public LocationDbService(LocationRepository repository, LocationMapper mapper) {
        super("LocationDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Location create(@NonNull Location item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            LocationDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            LocationDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public Location update(@NonNull String extid, Location item) {

        LocationDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (item.getTrialId() != null) record.setTrialId(item.getTrialId());
            if (item.getFacility() != null) record.setFacility(item.getFacility());
            if (item.getCity() != null) record.setCity(item.getCity());
            if (item.getState() != null) record.setState(item.getState());
            if (item.getZip() != null) record.setZip(item.getZip());
            if (item.getCountry() != null) record.setCountry(item.getCountry());
            if (item.getStatus() != null) record.setStatus(item.getStatus());
            if (item.getLatitude() != null) record.setLatitude(item.getLatitude());
            if (item.getLongitude() != null) record.setLongitude(item.getLongitude());
            record.setUpdatedAt(LocalDateTime.now());

            LocationDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        LocationDb record = repository.findByExtid(extid)
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

    public Location findByExtid(@NonNull String extid) {
        LocationDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<Location> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<Location> findAll(Pageable pageable) {
        Page<LocationDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Location> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<Location> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<LocationDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<Location> findAndLog(List<LocationDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
