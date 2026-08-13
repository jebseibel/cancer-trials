package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.PatientDiagnosisDb;
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
class PatientDiagnosisRepositoryTest {

    @Autowired
    private PatientDiagnosisRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void save_shouldPersistPatientDiagnosis() {
        PatientDiagnosisDb item = DomainBuilderDatabase.getPatientDiagnosisDb();

        PatientDiagnosisDb saved = repository.save(item);

        assertNotNull(saved.getId());
        assertEquals(item.getPatientId(), saved.getPatientId());
        assertEquals(item.getCancerType(), saved.getCancerType());
    }

    @Test
    void findByExtid_shouldReturnPatientDiagnosis_whenExists() {
        PatientDiagnosisDb saved = repository.save(DomainBuilderDatabase.getPatientDiagnosisDb());

        Optional<PatientDiagnosisDb> found = repository.findByExtid(saved.getExtid());

        assertTrue(found.isPresent());
        assertEquals(saved.getCancerType(), found.get().getCancerType());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        assertTrue(repository.findByExtid(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    void findByPatientId_shouldReturnPatientDiagnosis_whenExists() {
        PatientDiagnosisDb saved = repository.save(
                DomainBuilderDatabase.getPatientDiagnosisDb(4242L, "Breast Cancer"));
        repository.save(DomainBuilderDatabase.getPatientDiagnosisDb(9999L, "Lung Cancer"));

        List<PatientDiagnosisDb> found = repository.findByPatientId(4242L);

        assertEquals(1, found.size());
        assertEquals(saved.getExtid(), found.get(0).getExtid());
        assertEquals("Breast Cancer", found.get(0).getCancerType());
    }

    @Test
    void findByPatientId_shouldReturnEmpty_whenNotExists() {
        repository.save(DomainBuilderDatabase.getPatientDiagnosisDb(4242L, "Breast Cancer"));

        assertTrue(repository.findByPatientId(123456L).isEmpty());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        PatientDiagnosisDb saved = repository.save(DomainBuilderDatabase.getPatientDiagnosisDb());

        assertTrue(repository.existsByExtid(saved.getExtid()));
    }

    @Test
    void existsByExtid_shouldReturnFalse_whenNotExists() {
        assertFalse(repository.existsByExtid(UUID.randomUUID().toString()));
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        repository.save(DomainBuilderDatabase.getPatientDiagnosisDb());

        PatientDiagnosisDb inactive = DomainBuilderDatabase.getPatientDiagnosisDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        Page<PatientDiagnosisDb> found = repository.findByActive(ActiveEnum.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, found.getTotalElements());
        assertEquals(ActiveEnum.ACTIVE, found.getContent().get(0).getActive());
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        repository.save(DomainBuilderDatabase.getPatientDiagnosisDb());

        PatientDiagnosisDb inactive = DomainBuilderDatabase.getPatientDiagnosisDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        Page<PatientDiagnosisDb> found = repository.findByActive(ActiveEnum.INACTIVE, PageRequest.of(0, 10));

        assertEquals(1, found.getTotalElements());
        assertEquals(ActiveEnum.INACTIVE, found.getContent().get(0).getActive());
    }

    @Test
    void findAll_shouldReturnAllPatientDiagnoses() {
        repository.save(DomainBuilderDatabase.getPatientDiagnosisDb());
        repository.save(DomainBuilderDatabase.getPatientDiagnosisDb());

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void deleteById_shouldRemovePatientDiagnosis() {
        PatientDiagnosisDb saved = repository.save(DomainBuilderDatabase.getPatientDiagnosisDb());

        repository.deleteById(saved.getId());

        assertTrue(repository.findByExtid(saved.getExtid()).isEmpty());
    }
}
