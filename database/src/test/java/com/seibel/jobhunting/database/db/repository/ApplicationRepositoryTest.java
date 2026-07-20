package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.ApplicationDb;
import com.seibel.jobhunting.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApplicationRepositoryTest {

    @Autowired
    private ApplicationRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnApplication_whenExists() {
        // Arrange
        ApplicationDb application = DomainBuilderDatabase.getApplicationDb();
        repository.save(application);

        // Act
        Optional<ApplicationDb> result = repository.findByExtid(application.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(application.getExtid(), result.get().getExtid());
        assertEquals(application.getJobPostingId(), result.get().getJobPostingId());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<ApplicationDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        ApplicationDb active1 = DomainBuilderDatabase.getApplicationDb();
        active1.setActive(ActiveEnum.ACTIVE);
        ApplicationDb active2 = DomainBuilderDatabase.getApplicationDb();
        active2.setActive(ActiveEnum.ACTIVE);
        ApplicationDb inactive = DomainBuilderDatabase.getApplicationDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<ApplicationDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(c -> c.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        ApplicationDb active = DomainBuilderDatabase.getApplicationDb();
        active.setActive(ActiveEnum.ACTIVE);
        ApplicationDb inactive = DomainBuilderDatabase.getApplicationDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<ApplicationDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        ApplicationDb application = DomainBuilderDatabase.getApplicationDb();
        repository.save(application);

        // Act
        boolean result = repository.existsByExtid(application.getExtid());

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
    void findByJobPostingId_shouldReturnMatchingApplications() {
        // Arrange
        ApplicationDb application1 = DomainBuilderDatabase.getApplicationDb();
        application1.setJobPostingId(42L);
        ApplicationDb application2 = DomainBuilderDatabase.getApplicationDb();
        application2.setJobPostingId(99L);

        repository.save(application1);
        repository.save(application2);

        // Act
        List<ApplicationDb> result = repository.findByJobPostingId(42L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).getJobPostingId());
    }

    @Test
    void save_shouldPersistApplication() {
        // Arrange
        ApplicationDb application = DomainBuilderDatabase.getApplicationDb();

        // Act
        ApplicationDb saved = repository.save(application);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(application.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllApplications() {
        // Arrange
        ApplicationDb application1 = DomainBuilderDatabase.getApplicationDb();
        ApplicationDb application2 = DomainBuilderDatabase.getApplicationDb();
        repository.save(application1);
        repository.save(application2);

        // Act
        List<ApplicationDb> result = (List<ApplicationDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveApplication() {
        // Arrange
        ApplicationDb application = DomainBuilderDatabase.getApplicationDb();
        ApplicationDb saved = repository.save(application);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<ApplicationDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
