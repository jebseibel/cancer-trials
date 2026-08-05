package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.LabResultDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test-database")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LabResultRepositoryTest {

    @Autowired
    private LabResultRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void save_shouldPersistLabResult() {
        LabResultDb item = DomainBuilderDatabase.getLabResultDb();

        LabResultDb saved = repository.save(item);

        assertNotNull(saved.getId());
        assertEquals(item.getFhirResourceId(), saved.getFhirResourceId());
        assertEquals(item.getTestName(), saved.getTestName());
    }

    @Test
    void save_shouldPersistNullValueUnit() {
        // Epic's real payload returned a value with no unit - the column must accept it.
        LabResultDb item = DomainBuilderDatabase.getLabResultDb();
        item.setValueUnit(null);

        LabResultDb saved = repository.save(item);

        assertNotNull(saved.getId());
        assertNull(saved.getValueUnit());
        assertNotNull(saved.getValueQuantity());
    }

    @Test
    void findByExtid_shouldReturnLabResult_whenExists() {
        LabResultDb saved = repository.save(DomainBuilderDatabase.getLabResultDb());

        Optional<LabResultDb> found = repository.findByExtid(saved.getExtid());

        assertTrue(found.isPresent());
        assertEquals(saved.getFhirResourceId(), found.get().getFhirResourceId());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        assertTrue(repository.findByExtid(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    void findByFhirResourceId_shouldReturnLabResult_whenExists() {
        LabResultDb saved = repository.save(
                DomainBuilderDatabase.getLabResultDb("Obs-12345", "Hemoglobin A1C"));

        Optional<LabResultDb> found = repository.findByFhirResourceId("Obs-12345");

        assertTrue(found.isPresent());
        assertEquals(saved.getExtid(), found.get().getExtid());
        assertEquals("Hemoglobin A1C", found.get().getTestName());
    }

    @Test
    void findByFhirResourceId_shouldReturnEmpty_whenNotExists() {
        assertTrue(repository.findByFhirResourceId("Obs-does-not-exist").isEmpty());
    }

    @Test
    void findByLoincCodeOrderByEffectiveAtDesc_shouldReturnNewestFirst() {
        LabResultDb older = DomainBuilderDatabase.getLabResultDb();
        older.setLoincCode("4548-4");
        older.setEffectiveAt(LocalDateTime.now().minusDays(30));
        repository.save(older);

        LabResultDb newer = DomainBuilderDatabase.getLabResultDb();
        newer.setLoincCode("4548-4");
        newer.setEffectiveAt(LocalDateTime.now().minusDays(1));
        repository.save(newer);

        LabResultDb other = DomainBuilderDatabase.getLabResultDb();
        other.setLoincCode("9999-9");
        repository.save(other);

        List<LabResultDb> found = repository.findByLoincCodeOrderByEffectiveAtDesc("4548-4");

        assertEquals(2, found.size());
        assertEquals(newer.getExtid(), found.get(0).getExtid());
        assertEquals(older.getExtid(), found.get(1).getExtid());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        LabResultDb saved = repository.save(DomainBuilderDatabase.getLabResultDb());

        assertTrue(repository.existsByExtid(saved.getExtid()));
    }

    @Test
    void existsByExtid_shouldReturnFalse_whenNotExists() {
        assertFalse(repository.existsByExtid(UUID.randomUUID().toString()));
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        repository.save(DomainBuilderDatabase.getLabResultDb());

        LabResultDb inactive = DomainBuilderDatabase.getLabResultDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        Page<LabResultDb> found = repository.findByActive(ActiveEnum.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, found.getTotalElements());
        assertEquals(ActiveEnum.ACTIVE, found.getContent().get(0).getActive());
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        repository.save(DomainBuilderDatabase.getLabResultDb());

        LabResultDb inactive = DomainBuilderDatabase.getLabResultDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        Page<LabResultDb> found = repository.findByActive(ActiveEnum.INACTIVE, PageRequest.of(0, 10));

        assertEquals(1, found.getTotalElements());
        assertEquals(ActiveEnum.INACTIVE, found.getContent().get(0).getActive());
    }

    @Test
    void findAll_shouldReturnAllLabResults() {
        repository.save(DomainBuilderDatabase.getLabResultDb());
        repository.save(DomainBuilderDatabase.getLabResultDb());

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void deleteById_shouldRemoveLabResult() {
        LabResultDb saved = repository.save(DomainBuilderDatabase.getLabResultDb());

        repository.deleteById(saved.getId());

        assertTrue(repository.findByExtid(saved.getExtid()).isEmpty());
    }
}
