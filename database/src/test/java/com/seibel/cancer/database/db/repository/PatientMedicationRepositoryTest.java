package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.PatientMedicationDb;
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
class PatientMedicationRepositoryTest {

    @Autowired
    private PatientMedicationRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void save_shouldPersistPatientMedication() {
        PatientMedicationDb item = DomainBuilderDatabase.getPatientMedicationDb();

        PatientMedicationDb saved = repository.save(item);

        assertNotNull(saved.getId());
        assertEquals(item.getFhirResourceId(), saved.getFhirResourceId());
        assertEquals(item.getMedicationName(), saved.getMedicationName());
    }

    @Test
    void findByExtid_shouldReturnPatientMedication_whenExists() {
        PatientMedicationDb saved = repository.save(DomainBuilderDatabase.getPatientMedicationDb());

        Optional<PatientMedicationDb> found = repository.findByExtid(saved.getExtid());

        assertTrue(found.isPresent());
        assertEquals(saved.getFhirResourceId(), found.get().getFhirResourceId());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        Optional<PatientMedicationDb> found = repository.findByExtid(UUID.randomUUID().toString());

        assertTrue(found.isEmpty());
    }

    @Test
    void findByFhirResourceId_shouldReturnPatientMedication_whenExists() {
        PatientMedicationDb saved = repository.save(
                DomainBuilderDatabase.getPatientMedicationDb("MedReq-12345", "Tamoxifen"));

        Optional<PatientMedicationDb> found = repository.findByFhirResourceId("MedReq-12345");

        assertTrue(found.isPresent());
        assertEquals(saved.getExtid(), found.get().getExtid());
        assertEquals("Tamoxifen", found.get().getMedicationName());
    }

    @Test
    void findByFhirResourceId_shouldReturnEmpty_whenNotExists() {
        Optional<PatientMedicationDb> found = repository.findByFhirResourceId("MedReq-does-not-exist");

        assertTrue(found.isEmpty());
    }

    @Test
    void findByStatus_shouldReturnMatchingPatientMedications() {
        PatientMedicationDb active = DomainBuilderDatabase.getPatientMedicationDb();
        active.setStatus("active");
        repository.save(active);

        PatientMedicationDb stopped = DomainBuilderDatabase.getPatientMedicationDb();
        stopped.setStatus("stopped");
        repository.save(stopped);

        List<PatientMedicationDb> found = repository.findByStatus("active");

        assertEquals(1, found.size());
        assertEquals("active", found.get(0).getStatus());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        PatientMedicationDb saved = repository.save(DomainBuilderDatabase.getPatientMedicationDb());

        assertTrue(repository.existsByExtid(saved.getExtid()));
    }

    @Test
    void existsByExtid_shouldReturnFalse_whenNotExists() {
        assertFalse(repository.existsByExtid(UUID.randomUUID().toString()));
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        repository.save(DomainBuilderDatabase.getPatientMedicationDb());

        PatientMedicationDb inactive = DomainBuilderDatabase.getPatientMedicationDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        Page<PatientMedicationDb> found = repository.findByActive(ActiveEnum.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, found.getTotalElements());
        assertEquals(ActiveEnum.ACTIVE, found.getContent().get(0).getActive());
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        repository.save(DomainBuilderDatabase.getPatientMedicationDb());

        PatientMedicationDb inactive = DomainBuilderDatabase.getPatientMedicationDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        Page<PatientMedicationDb> found = repository.findByActive(ActiveEnum.INACTIVE, PageRequest.of(0, 10));

        assertEquals(1, found.getTotalElements());
        assertEquals(ActiveEnum.INACTIVE, found.getContent().get(0).getActive());
    }

    @Test
    void findAll_shouldReturnAllPatientMedications() {
        repository.save(DomainBuilderDatabase.getPatientMedicationDb());
        repository.save(DomainBuilderDatabase.getPatientMedicationDb());

        List<PatientMedicationDb> all = repository.findAll();

        assertEquals(2, all.size());
    }

    @Test
    void deleteById_shouldRemovePatientMedication() {
        PatientMedicationDb saved = repository.save(DomainBuilderDatabase.getPatientMedicationDb());

        repository.deleteById(saved.getId());

        assertTrue(repository.findByExtid(saved.getExtid()).isEmpty());
    }
}
