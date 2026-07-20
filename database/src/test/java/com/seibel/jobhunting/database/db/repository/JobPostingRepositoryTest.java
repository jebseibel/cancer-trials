package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.JobPostingDb;
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
class JobPostingRepositoryTest {

    @Autowired
    private JobPostingRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnJobPosting_whenExists() {
        // Arrange
        JobPostingDb jobPosting = DomainBuilderDatabase.getJobPostingDb();
        repository.save(jobPosting);

        // Act
        Optional<JobPostingDb> result = repository.findByExtid(jobPosting.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(jobPosting.getExtid(), result.get().getExtid());
        assertEquals(jobPosting.getTitle(), result.get().getTitle());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<JobPostingDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        JobPostingDb active1 = DomainBuilderDatabase.getJobPostingDb();
        active1.setActive(ActiveEnum.ACTIVE);
        JobPostingDb active2 = DomainBuilderDatabase.getJobPostingDb();
        active2.setActive(ActiveEnum.ACTIVE);
        JobPostingDb inactive = DomainBuilderDatabase.getJobPostingDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobPostingDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(c -> c.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        JobPostingDb active = DomainBuilderDatabase.getJobPostingDb();
        active.setActive(ActiveEnum.ACTIVE);
        JobPostingDb inactive = DomainBuilderDatabase.getJobPostingDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobPostingDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        JobPostingDb jobPosting = DomainBuilderDatabase.getJobPostingDb();
        repository.save(jobPosting);

        // Act
        boolean result = repository.existsByExtid(jobPosting.getExtid());

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
    void findBySourceUrl_shouldReturnJobPosting_whenExists() {
        // Arrange
        JobPostingDb jobPosting = DomainBuilderDatabase.getJobPostingDb();
        repository.save(jobPosting);

        // Act
        Optional<JobPostingDb> result = repository.findBySourceUrl(jobPosting.getSourceUrl());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(jobPosting.getSourceUrl(), result.get().getSourceUrl());
    }

    @Test
    void save_shouldPersistJobPosting() {
        // Arrange
        JobPostingDb jobPosting = DomainBuilderDatabase.getJobPostingDb();

        // Act
        JobPostingDb saved = repository.save(jobPosting);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(jobPosting.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllJobPostings() {
        // Arrange
        JobPostingDb jobPosting1 = DomainBuilderDatabase.getJobPostingDb();
        JobPostingDb jobPosting2 = DomainBuilderDatabase.getJobPostingDb();
        repository.save(jobPosting1);
        repository.save(jobPosting2);

        // Act
        List<JobPostingDb> result = (List<JobPostingDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveJobPosting() {
        // Arrange
        JobPostingDb jobPosting = DomainBuilderDatabase.getJobPostingDb();
        JobPostingDb saved = repository.save(jobPosting);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<JobPostingDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
