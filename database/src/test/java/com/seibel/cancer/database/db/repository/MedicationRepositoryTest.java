package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.MedicationDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test-database")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MedicationRepositoryTest {

    @Autowired
    private MedicationRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnMedication_whenExists() {
        // Arrange
        MedicationDb medication = DomainBuilderDatabase.getMedicationDb();
        repository.save(medication);

        // Act
        Optional<MedicationDb> result = repository.findByExtid(medication.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(medication.getExtid(), result.get().getExtid());
        assertEquals(medication.getName(), result.get().getName());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<MedicationDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByName_shouldReturnMedication_whenExists() {
        // Arrange
        MedicationDb medication = DomainBuilderDatabase.getMedicationDb();
        repository.save(medication);

        // Act
        Optional<MedicationDb> result = repository.findByName(medication.getName());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(medication.getName(), result.get().getName());
    }

    @Test
    void findByName_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<MedicationDb> result = repository.findByName("nonexistent-name");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        MedicationDb active1 = DomainBuilderDatabase.getMedicationDb();
        active1.setActive(ActiveEnum.ACTIVE);
        MedicationDb active2 = DomainBuilderDatabase.getMedicationDb();
        active2.setActive(ActiveEnum.ACTIVE);
        MedicationDb inactive = DomainBuilderDatabase.getMedicationDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<MedicationDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(m -> m.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        MedicationDb active = DomainBuilderDatabase.getMedicationDb();
        active.setActive(ActiveEnum.ACTIVE);
        MedicationDb inactive = DomainBuilderDatabase.getMedicationDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<MedicationDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        MedicationDb medication = DomainBuilderDatabase.getMedicationDb();
        repository.save(medication);

        // Act
        boolean result = repository.existsByExtid(medication.getExtid());

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByExtid_shouldReturnFalse_whenNotExists() {
        // Act
        boolean result = repository.existsByExtid("nonexistent-extid");

        // Assert
        assertFalse(result);
    }

    @Test
    void save_shouldPersistMedication() {
        // Arrange
        MedicationDb medication = DomainBuilderDatabase.getMedicationDb();

        // Act
        MedicationDb saved = repository.save(medication);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(medication.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllMedications() {
        // Arrange
        MedicationDb medication1 = DomainBuilderDatabase.getMedicationDb();
        MedicationDb medication2 = DomainBuilderDatabase.getMedicationDb();
        repository.save(medication1);
        repository.save(medication2);

        // Act
        List<MedicationDb> result = (List<MedicationDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveMedication() {
        // Arrange
        MedicationDb medication = DomainBuilderDatabase.getMedicationDb();
        MedicationDb saved = repository.save(medication);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<MedicationDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
