package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.PatientDb;
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
class PatientRepositoryTest {

    @Autowired
    private PatientRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnPatient_whenExists() {
        PatientDb saved = repository.save(DomainBuilderDatabase.getPatientDb());

        Optional<PatientDb> found = repository.findByExtid(saved.getExtid());

        assertTrue(found.isPresent());
        assertEquals(saved.getDisplayName(), found.get().getDisplayName());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotFound() {
        Optional<PatientDb> found = repository.findByExtid(UUID.randomUUID().toString());

        assertTrue(found.isEmpty());
    }

    @Test
    void findByActive_shouldReturnOnlyActivePatients() {
        repository.save(DomainBuilderDatabase.getPatientDb());

        PatientDb inactive = DomainBuilderDatabase.getPatientDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        List<PatientDb> active = repository.findByActive(ActiveEnum.ACTIVE);

        assertEquals(1, active.size());
        assertEquals(ActiveEnum.ACTIVE, active.get(0).getActive());
    }

    @Test
    void findByActive_shouldReturnOnlyInactivePatients() {
        repository.save(DomainBuilderDatabase.getPatientDb());

        PatientDb inactive = DomainBuilderDatabase.getPatientDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        List<PatientDb> found = repository.findByActive(ActiveEnum.INACTIVE);

        assertEquals(1, found.size());
        assertEquals(ActiveEnum.INACTIVE, found.get(0).getActive());
    }

    @Test
    void findByActive_shouldPage() {
        repository.save(DomainBuilderDatabase.getPatientDb());
        repository.save(DomainBuilderDatabase.getPatientDb());
        repository.save(DomainBuilderDatabase.getPatientDb());

        Page<PatientDb> page = repository.findByActive(ActiveEnum.ACTIVE, PageRequest.of(0, 2));

        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        PatientDb saved = repository.save(DomainBuilderDatabase.getPatientDb());

        assertTrue(repository.existsByExtid(saved.getExtid()));
    }

    @Test
    void existsByExtid_shouldReturnFalse_whenNotFound() {
        assertFalse(repository.existsByExtid(UUID.randomUUID().toString()));
    }

    @Test
    void save_shouldPersistPatient() {
        PatientDb item = DomainBuilderDatabase.getPatientDb();

        PatientDb saved = repository.save(item);

        assertNotNull(saved.getId());
        assertEquals(item.getExtid(), saved.getExtid());
        assertEquals(item.getDisplayName(), saved.getDisplayName());
        assertEquals(item.getFullName(), saved.getFullName());
        assertEquals(item.getDateOfBirth(), saved.getDateOfBirth());
        assertEquals(item.getSex(), saved.getSex());
    }

    @Test
    void findAll_shouldReturnAllPatients() {
        repository.save(DomainBuilderDatabase.getPatientDb());
        repository.save(DomainBuilderDatabase.getPatientDb());

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void findAllActive_shouldReturnOnlyActive() {
        repository.save(DomainBuilderDatabase.getPatientDb());

        PatientDb inactive = DomainBuilderDatabase.getPatientDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        assertEquals(1, repository.findAllActive().size());
    }

    @Test
    void deleteById_shouldRemovePatient() {
        PatientDb saved = repository.save(DomainBuilderDatabase.getPatientDb());

        repository.deleteById(saved.getId());

        assertTrue(repository.findByExtid(saved.getExtid()).isEmpty());
    }
}
