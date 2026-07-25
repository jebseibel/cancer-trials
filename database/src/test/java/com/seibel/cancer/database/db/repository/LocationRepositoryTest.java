package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.LocationDb;
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
class LocationRepositoryTest {

    @Autowired
    private LocationRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnLocation_whenExists() {
        // Arrange
        LocationDb location = DomainBuilderDatabase.getLocationDb();
        repository.save(location);

        // Act
        Optional<LocationDb> result = repository.findByExtid(location.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(location.getExtid(), result.get().getExtid());
        assertEquals(location.getFacility(), result.get().getFacility());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<LocationDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByTrialId_shouldReturnLocations_whenExists() {
        // Arrange
        LocationDb location = DomainBuilderDatabase.getLocationDb(42L, null);
        repository.save(location);

        // Act
        List<LocationDb> result = repository.findByTrialId(42L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(location.getExtid(), result.get(0).getExtid());
    }

    @Test
    void findByTrialId_shouldReturnEmpty_whenNotExists() {
        // Act
        List<LocationDb> result = repository.findByTrialId(999999L);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        LocationDb active1 = DomainBuilderDatabase.getLocationDb();
        active1.setActive(ActiveEnum.ACTIVE);
        LocationDb active2 = DomainBuilderDatabase.getLocationDb();
        active2.setActive(ActiveEnum.ACTIVE);
        LocationDb inactive = DomainBuilderDatabase.getLocationDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<LocationDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(o -> o.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        LocationDb active = DomainBuilderDatabase.getLocationDb();
        active.setActive(ActiveEnum.ACTIVE);
        LocationDb inactive = DomainBuilderDatabase.getLocationDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<LocationDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        LocationDb location = DomainBuilderDatabase.getLocationDb();
        repository.save(location);

        // Act
        boolean result = repository.existsByExtid(location.getExtid());

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
    void save_shouldPersistLocation() {
        // Arrange
        LocationDb location = DomainBuilderDatabase.getLocationDb();

        // Act
        LocationDb saved = repository.save(location);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(location.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllLocations() {
        // Arrange
        LocationDb location1 = DomainBuilderDatabase.getLocationDb();
        LocationDb location2 = DomainBuilderDatabase.getLocationDb();
        repository.save(location1);
        repository.save(location2);

        // Act
        List<LocationDb> result = (List<LocationDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveLocation() {
        // Arrange
        LocationDb location = DomainBuilderDatabase.getLocationDb();
        LocationDb saved = repository.save(location);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<LocationDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
