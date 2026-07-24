package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.PurchaseDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<PurchaseDb, Long> {
    Optional<PurchaseDb> findByExtid(String extid);
    List<PurchaseDb> findByActive(ActiveEnum active);
    Page<PurchaseDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<PurchaseDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
