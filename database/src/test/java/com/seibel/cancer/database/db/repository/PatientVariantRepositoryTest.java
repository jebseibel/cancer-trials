package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.PatientVariantDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

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
class PatientVariantRepositoryTest {

    @Autowired
    private PatientVariantRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnPatientVariant_whenExists() {
        PatientVariantDb saved = repository.save(DomainBuilderDatabase.getPatientVariantDb());

        Optional<PatientVariantDb> found = repository.findByExtid(saved.getExtid());

        assertTrue(found.isPresent());
        assertEquals(saved.getExtid(), found.get().getExtid());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotFound() {
        Optional<PatientVariantDb> found = repository.findByExtid(UUID.randomUUID().toString());

        assertTrue(found.isEmpty());
    }

    @Test
    void findByAppUserId_shouldReturnOnlyActiveRows() {
        repository.save(DomainBuilderDatabase.getPatientVariantDb(4242L, "DETECTED"));

        PatientVariantDb deleted = DomainBuilderDatabase.getPatientVariantDb(4242L, "NOT_DETECTED");
        deleted.setActive(ActiveEnum.INACTIVE);
        repository.save(deleted);

        List<PatientVariantDb> found = repository.findByAppUserId(4242L);

        // The soft-delete filter is the point: PatientDiagnosisRepository shipped without it
        // and the Diagnosis page edited a deleted row.
        assertEquals(1, found.size());
        assertEquals("DETECTED", found.get(0).getPik3caStatus());
    }

    @Test
    void findByPatientDiagnosisId_shouldReturnMatchingRows() {
        PatientVariantDb item = DomainBuilderDatabase.getPatientVariantDb();
        item.setPatientDiagnosisId(9999L);
        repository.save(item);

        List<PatientVariantDb> found = repository.findByPatientDiagnosisId(9999L);

        assertEquals(1, found.size());
        assertEquals(9999L, found.get(0).getPatientDiagnosisId());
    }

    @Test
    void findByActive_shouldReturnOnlyActive() {
        repository.save(DomainBuilderDatabase.getPatientVariantDb());

        PatientVariantDb inactive = DomainBuilderDatabase.getPatientVariantDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        List<PatientVariantDb> active = repository.findByActive(ActiveEnum.ACTIVE);
        List<PatientVariantDb> notActive = repository.findByActive(ActiveEnum.INACTIVE);

        assertEquals(1, active.size());
        assertEquals(1, notActive.size());
    }

    @Test
    void findByActive_shouldPage() {
        repository.save(DomainBuilderDatabase.getPatientVariantDb());
        repository.save(DomainBuilderDatabase.getPatientVariantDb());
        repository.save(DomainBuilderDatabase.getPatientVariantDb());

        Page<PatientVariantDb> page = repository.findByActive(ActiveEnum.ACTIVE, PageRequest.of(0, 2));

        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        PatientVariantDb saved = repository.save(DomainBuilderDatabase.getPatientVariantDb());

        assertTrue(repository.existsByExtid(saved.getExtid()));
    }

    @Test
    void existsByExtid_shouldReturnFalse_whenNotFound() {
        assertFalse(repository.existsByExtid(UUID.randomUUID().toString()));
    }

    @Test
    void save_shouldPersistPatientVariant() {
        PatientVariantDb saved = repository.save(DomainBuilderDatabase.getPatientVariantDb());

        assertNotNull(saved.getId());
        assertNotNull(saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllPatientVariants() {
        repository.save(DomainBuilderDatabase.getPatientVariantDb());
        repository.save(DomainBuilderDatabase.getPatientVariantDb());

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void deleteById_shouldRemovePatientVariant() {
        PatientVariantDb saved = repository.save(DomainBuilderDatabase.getPatientVariantDb());

        repository.deleteById(saved.getId());

        assertTrue(repository.findById(saved.getId()).isEmpty());
    }
}
