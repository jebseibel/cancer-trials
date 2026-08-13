package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.UserPatientDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Grants, read by the authorisation check on every patient-scoped request.
 *
 * <p><strong>A grant is out of force for two independent reasons</strong> - it was revoked
 * ({@code revokedAt} is set) or the row was soft-deleted ({@code active} is INACTIVE). The
 * {@code findActive*} methods below require <em>both</em> to be clear, and they are the only
 * finders authorisation should use. A finder that checks one and not the other grants access
 * it should not.
 */
@Repository
public interface UserPatientRepository extends JpaRepository<UserPatientDb, Long> {

    Optional<UserPatientDb> findByExtid(String extid);

    List<UserPatientDb> findByActive(ActiveEnum active);

    Page<UserPatientDb> findByActive(ActiveEnum active, Pageable pageable);

    boolean existsByExtid(String extid);

    default List<UserPatientDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }

    // --- Authorisation finders. Both conditions, always. ---

    List<UserPatientDb> findByUserIdAndRevokedAtIsNullAndActive(Long userId, ActiveEnum active);

    List<UserPatientDb> findByPatientIdAndRevokedAtIsNullAndActive(Long patientId, ActiveEnum active);

    Optional<UserPatientDb> findByUserIdAndPatientIdAndRevokedAtIsNullAndActive(
            Long userId, Long patientId, ActiveEnum active);

    /** Every patient this login may currently see. */
    default List<UserPatientDb> findActiveGrantsForUser(Long userId) {
        return findByUserIdAndRevokedAtIsNullAndActive(userId, ActiveEnum.ACTIVE);
    }

    /** Everyone who may currently see this patient - the "who can see my record" list. */
    default List<UserPatientDb> findActiveGrantsForPatient(Long patientId) {
        return findByPatientIdAndRevokedAtIsNullAndActive(patientId, ActiveEnum.ACTIVE);
    }

    /** The single grant linking one login to one patient, if it is in force. */
    default Optional<UserPatientDb> findActiveGrant(Long userId, Long patientId) {
        return findByUserIdAndPatientIdAndRevokedAtIsNullAndActive(userId, patientId, ActiveEnum.ACTIVE);
    }
}
