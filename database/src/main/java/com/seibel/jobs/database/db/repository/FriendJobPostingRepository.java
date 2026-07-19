package com.seibel.jobs.database.db.repository;

import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.database.db.entity.FriendJobPostingDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendJobPostingRepository extends JpaRepository<FriendJobPostingDb, Long> {
    Optional<FriendJobPostingDb> findByExtid(String extid);
    List<FriendJobPostingDb> findByFriendId(Long friendId);
    List<FriendJobPostingDb> findByJobPostingId(Long jobPostingId);
    List<FriendJobPostingDb> findByActive(ActiveEnum active);
    Page<FriendJobPostingDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<FriendJobPostingDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
