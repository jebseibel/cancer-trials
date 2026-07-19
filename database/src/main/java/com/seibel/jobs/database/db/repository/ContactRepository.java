package com.seibel.jobs.database.db.repository;

import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.database.db.entity.ContactDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<ContactDb, Long> {
    Optional<ContactDb> findByExtid(String extid);
    List<ContactDb> findByCompanyId(Long companyId);
    List<ContactDb> findByJobPostingId(Long jobPostingId);
    List<ContactDb> findByActive(ActiveEnum active);
    Page<ContactDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<ContactDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
