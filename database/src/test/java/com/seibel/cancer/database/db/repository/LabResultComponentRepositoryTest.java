package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.LabResultComponentDb;
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
class LabResultComponentRepositoryTest {

    @Autowired
    private LabResultComponentRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void save_shouldPersistLabResultComponent() {
        LabResultComponentDb item = DomainBuilderDatabase.getLabResultComponentDb();

        LabResultComponentDb saved = repository.save(item);

        assertNotNull(saved.getId());
        assertEquals(item.getLabResultId(), saved.getLabResultId());
        assertEquals(item.getComponentName(), saved.getComponentName());
    }

    @Test
    void findByExtid_shouldReturnLabResultComponent_whenExists() {
        LabResultComponentDb saved = repository.save(DomainBuilderDatabase.getLabResultComponentDb());

        Optional<LabResultComponentDb> found = repository.findByExtid(saved.getExtid());

        assertTrue(found.isPresent());
        assertEquals(saved.getComponentName(), found.get().getComponentName());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        assertTrue(repository.findByExtid(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    void findByLabResultId_shouldReturnComponents_whenExists() {
        // Two components on one panel, one on another - only the panel's own come back.
        repository.save(DomainBuilderDatabase.getLabResultComponentDb(4242L, "Hemoglobin"));
        repository.save(DomainBuilderDatabase.getLabResultComponentDb(4242L, "Hematocrit"));
        repository.save(DomainBuilderDatabase.getLabResultComponentDb(9999L, "Platelets"));

        List<LabResultComponentDb> found = repository.findByLabResultId(4242L);

        assertEquals(2, found.size());
        assertTrue(found.stream().allMatch(c -> c.getLabResultId().equals(4242L)));
    }

    @Test
    void findByLabResultId_shouldReturnEmpty_whenNotExists() {
        assertTrue(repository.findByLabResultId(123456L).isEmpty());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        LabResultComponentDb saved = repository.save(DomainBuilderDatabase.getLabResultComponentDb());

        assertTrue(repository.existsByExtid(saved.getExtid()));
    }

    @Test
    void existsByExtid_shouldReturnFalse_whenNotExists() {
        assertFalse(repository.existsByExtid(UUID.randomUUID().toString()));
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        repository.save(DomainBuilderDatabase.getLabResultComponentDb());

        LabResultComponentDb inactive = DomainBuilderDatabase.getLabResultComponentDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        Page<LabResultComponentDb> found = repository.findByActive(ActiveEnum.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, found.getTotalElements());
        assertEquals(ActiveEnum.ACTIVE, found.getContent().get(0).getActive());
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        repository.save(DomainBuilderDatabase.getLabResultComponentDb());

        LabResultComponentDb inactive = DomainBuilderDatabase.getLabResultComponentDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        Page<LabResultComponentDb> found = repository.findByActive(ActiveEnum.INACTIVE, PageRequest.of(0, 10));

        assertEquals(1, found.getTotalElements());
        assertEquals(ActiveEnum.INACTIVE, found.getContent().get(0).getActive());
    }

    @Test
    void findAll_shouldReturnAllLabResultComponents() {
        repository.save(DomainBuilderDatabase.getLabResultComponentDb());
        repository.save(DomainBuilderDatabase.getLabResultComponentDb());

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void deleteById_shouldRemoveLabResultComponent() {
        LabResultComponentDb saved = repository.save(DomainBuilderDatabase.getLabResultComponentDb());

        repository.deleteById(saved.getId());

        assertTrue(repository.findByExtid(saved.getExtid()).isEmpty());
    }
}
