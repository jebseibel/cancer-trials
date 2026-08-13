package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.PatientPriorTreatmentDb;
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
class PatientPriorTreatmentRepositoryTest {

    @Autowired
    private PatientPriorTreatmentRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnPatientPriorTreatment_whenExists() {
        PatientPriorTreatmentDb saved = repository.save(DomainBuilderDatabase.getPatientPriorTreatmentDb());

        Optional<PatientPriorTreatmentDb> found = repository.findByExtid(saved.getExtid());

        assertTrue(found.isPresent());
        assertEquals(saved.getExtid(), found.get().getExtid());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotFound() {
        Optional<PatientPriorTreatmentDb> found = repository.findByExtid(UUID.randomUUID().toString());

        assertTrue(found.isEmpty());
    }

    @Test
    void findByPatientId_shouldReturnOnlyActiveRows() {
        repository.save(DomainBuilderDatabase.getPatientPriorTreatmentDb(4242L, "CURRENT"));

        PatientPriorTreatmentDb deleted = DomainBuilderDatabase.getPatientPriorTreatmentDb(4242L, "PROGRESSED");
        deleted.setActive(ActiveEnum.INACTIVE);
        repository.save(deleted);

        List<PatientPriorTreatmentDb> found = repository.findByPatientId(4242L);

        // The soft-delete filter is the point: PatientDiagnosisRepository shipped without it
        // and the Diagnosis page edited a deleted row.
        assertEquals(1, found.size());
        assertEquals("CURRENT", found.get(0).getCdk46Status());
    }

    @Test
    void findByPatientDiagnosisId_shouldReturnMatchingRows() {
        PatientPriorTreatmentDb item = DomainBuilderDatabase.getPatientPriorTreatmentDb();
        item.setPatientDiagnosisId(9999L);
        repository.save(item);

        List<PatientPriorTreatmentDb> found = repository.findByPatientDiagnosisId(9999L);

        assertEquals(1, found.size());
        assertEquals(9999L, found.get(0).getPatientDiagnosisId());
    }

    @Test
    void findByActive_shouldReturnOnlyActive() {
        repository.save(DomainBuilderDatabase.getPatientPriorTreatmentDb());

        PatientPriorTreatmentDb inactive = DomainBuilderDatabase.getPatientPriorTreatmentDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        List<PatientPriorTreatmentDb> active = repository.findByActive(ActiveEnum.ACTIVE);
        List<PatientPriorTreatmentDb> notActive = repository.findByActive(ActiveEnum.INACTIVE);

        assertEquals(1, active.size());
        assertEquals(1, notActive.size());
    }

    @Test
    void findByActive_shouldPage() {
        repository.save(DomainBuilderDatabase.getPatientPriorTreatmentDb());
        repository.save(DomainBuilderDatabase.getPatientPriorTreatmentDb());
        repository.save(DomainBuilderDatabase.getPatientPriorTreatmentDb());

        Page<PatientPriorTreatmentDb> page =
                repository.findByActive(ActiveEnum.ACTIVE, PageRequest.of(0, 2));

        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        PatientPriorTreatmentDb saved = repository.save(DomainBuilderDatabase.getPatientPriorTreatmentDb());

        assertTrue(repository.existsByExtid(saved.getExtid()));
    }

    @Test
    void existsByExtid_shouldReturnFalse_whenNotFound() {
        assertFalse(repository.existsByExtid(UUID.randomUUID().toString()));
    }

    @Test
    void save_shouldPersistPatientPriorTreatment() {
        PatientPriorTreatmentDb saved = repository.save(DomainBuilderDatabase.getPatientPriorTreatmentDb());

        assertNotNull(saved.getId());
        assertNotNull(saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllPatientPriorTreatments() {
        repository.save(DomainBuilderDatabase.getPatientPriorTreatmentDb());
        repository.save(DomainBuilderDatabase.getPatientPriorTreatmentDb());

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void deleteById_shouldRemovePatientPriorTreatment() {
        PatientPriorTreatmentDb saved = repository.save(DomainBuilderDatabase.getPatientPriorTreatmentDb());

        repository.deleteById(saved.getId());

        assertTrue(repository.findById(saved.getId()).isEmpty());
    }
}
