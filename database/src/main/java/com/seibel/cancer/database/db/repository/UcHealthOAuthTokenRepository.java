package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.UcHealthOAuthTokenDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UcHealthOAuthTokenRepository extends JpaRepository<UcHealthOAuthTokenDb, Long> {
    Optional<UcHealthOAuthTokenDb> findByExtid(String extid);
    List<UcHealthOAuthTokenDb> findByActive(ActiveEnum active);
    Page<UcHealthOAuthTokenDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    Optional<UcHealthOAuthTokenDb> findFirstByActiveOrderByCreatedAtDesc(ActiveEnum active);

    default List<UcHealthOAuthTokenDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
