package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.UserPatientDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test-database")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserPatientRepositoryTest {

    private static final Long USER_ID = 4001L;
    private static final Long PATIENT_ID = 5001L;

    @Autowired
    private UserPatientRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnGrant_whenExists() {
        UserPatientDb saved = repository.save(DomainBuilderDatabase.getUserPatientDb());

        Optional<UserPatientDb> found = repository.findByExtid(saved.getExtid());

        assertTrue(found.isPresent());
        assertEquals(saved.getUserId(), found.get().getUserId());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotFound() {
        assertTrue(repository.findByExtid(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    void save_shouldPersistGrant() {
        UserPatientDb item = DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID,
                AccessLevel.EDIT_RECORD, null);

        UserPatientDb saved = repository.save(item);

        assertNotNull(saved.getId());
        assertEquals(USER_ID, saved.getUserId());
        assertEquals(PATIENT_ID, saved.getPatientId());
        assertEquals(AccessLevel.EDIT_RECORD, saved.getAccessLevel());
        assertNotNull(saved.getGrantedAt());
    }

    /** Stored as the enum name, so a level inserted mid-enum cannot re-map existing grants. */
    @Test
    void save_shouldRoundTripEveryAccessLevel() {
        for (AccessLevel level : AccessLevel.values()) {
            UserPatientDb saved = repository.save(
                    DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID, level, null));

            assertEquals(level, repository.findByExtid(saved.getExtid()).orElseThrow().getAccessLevel());
        }
    }

    @Test
    void findActiveGrant_shouldReturnGrant_whenInForce() {
        repository.save(DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID));

        Optional<UserPatientDb> found = repository.findActiveGrant(USER_ID, PATIENT_ID);

        assertTrue(found.isPresent());
    }

    @Test
    void findActiveGrant_shouldReturnEmpty_whenNoGrantExists() {
        assertTrue(repository.findActiveGrant(USER_ID, PATIENT_ID).isEmpty());
    }

    /** The whole point of revocation: the row survives, the access does not. */
    @Test
    void findActiveGrant_shouldReturnEmpty_whenRevoked() {
        UserPatientDb revoked = DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID);
        revoked.setRevokedAt(LocalDateTime.now());
        repository.save(revoked);

        assertTrue(repository.findActiveGrant(USER_ID, PATIENT_ID).isEmpty());
        // The row itself is still there - revocation is not a delete.
        assertEquals(1, repository.findAll().size());
    }

    /** A soft-deleted grant must not authorise either, independently of revokedAt. */
    @Test
    void findActiveGrant_shouldReturnEmpty_whenSoftDeleted() {
        UserPatientDb deleted = DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID);
        deleted.setActive(ActiveEnum.INACTIVE);
        deleted.setDeletedAt(LocalDateTime.now());
        repository.save(deleted);

        assertTrue(repository.findActiveGrant(USER_ID, PATIENT_ID).isEmpty());
    }

    @Test
    void findActiveGrant_shouldNotMatchADifferentUser() {
        repository.save(DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID));

        assertTrue(repository.findActiveGrant(USER_ID + 1, PATIENT_ID).isEmpty());
    }

    @Test
    void findActiveGrant_shouldNotMatchADifferentPatient() {
        repository.save(DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID));

        assertTrue(repository.findActiveGrant(USER_ID, PATIENT_ID + 1).isEmpty());
    }

    @Test
    void findActiveGrantsForUser_shouldReturnOnlyThatUsersLiveGrants() {
        repository.save(DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID));
        repository.save(DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID + 1));
        repository.save(DomainBuilderDatabase.getUserPatientDb(USER_ID + 99, PATIENT_ID));

        UserPatientDb revoked = DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID + 2);
        revoked.setRevokedAt(LocalDateTime.now());
        repository.save(revoked);

        List<UserPatientDb> found = repository.findActiveGrantsForUser(USER_ID);

        assertEquals(2, found.size());
        assertTrue(found.stream().allMatch(g -> g.getUserId().equals(USER_ID)));
        assertTrue(found.stream().allMatch(g -> g.getRevokedAt() == null));
    }

    @Test
    void findActiveGrantsForPatient_shouldReturnEveryoneWhoCanSeeIt() {
        repository.save(DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID));
        repository.save(DomainBuilderDatabase.getUserPatientDb(USER_ID + 1, PATIENT_ID));

        UserPatientDb revoked = DomainBuilderDatabase.getUserPatientDb(USER_ID + 2, PATIENT_ID);
        revoked.setRevokedAt(LocalDateTime.now());
        repository.save(revoked);

        List<UserPatientDb> found = repository.findActiveGrantsForPatient(PATIENT_ID);

        assertEquals(2, found.size());
    }

    /** Re-granting after a revoke is legitimate, so two rows for one pair must be storable. */
    @Test
    void save_shouldAllowARegrantAfterRevocation() {
        UserPatientDb first = DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID);
        first.setRevokedAt(LocalDateTime.now());
        repository.save(first);

        repository.save(DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID));

        assertEquals(2, repository.findAll().size());
        assertTrue(repository.findActiveGrant(USER_ID, PATIENT_ID).isPresent());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        UserPatientDb saved = repository.save(DomainBuilderDatabase.getUserPatientDb());

        assertTrue(repository.existsByExtid(saved.getExtid()));
    }

    @Test
    void existsByExtid_shouldReturnFalse_whenNotFound() {
        assertFalse(repository.existsByExtid(UUID.randomUUID().toString()));
    }

    @Test
    void findAllActive_shouldReturnOnlyActive() {
        repository.save(DomainBuilderDatabase.getUserPatientDb());

        UserPatientDb inactive = DomainBuilderDatabase.getUserPatientDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        assertEquals(1, repository.findAllActive().size());
    }
}
