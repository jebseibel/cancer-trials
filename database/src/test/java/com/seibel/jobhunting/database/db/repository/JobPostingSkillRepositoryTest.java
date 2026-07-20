package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.JobPostingSkillDb;
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
class JobPostingSkillRepositoryTest {

    @Autowired
    private JobPostingSkillRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnJobPostingSkill_whenExists() {
        // Arrange
        JobPostingSkillDb item = DomainBuilderDatabase.getJobPostingSkillDb();
        repository.save(item);

        // Act
        Optional<JobPostingSkillDb> result = repository.findByExtid(item.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(item.getExtid(), result.get().getExtid());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<JobPostingSkillDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        JobPostingSkillDb active1 = DomainBuilderDatabase.getJobPostingSkillDb();
        active1.setActive(ActiveEnum.ACTIVE);
        JobPostingSkillDb active2 = DomainBuilderDatabase.getJobPostingSkillDb();
        active2.setActive(ActiveEnum.ACTIVE);
        JobPostingSkillDb inactive = DomainBuilderDatabase.getJobPostingSkillDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobPostingSkillDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(c -> c.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        JobPostingSkillDb active = DomainBuilderDatabase.getJobPostingSkillDb();
        active.setActive(ActiveEnum.ACTIVE);
        JobPostingSkillDb inactive = DomainBuilderDatabase.getJobPostingSkillDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobPostingSkillDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        JobPostingSkillDb item = DomainBuilderDatabase.getJobPostingSkillDb();
        repository.save(item);

        // Act
        boolean result = repository.existsByExtid(item.getExtid());

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
    void findByJobPostingId_shouldReturnMatchingLinks() {
        // Arrange
        JobPostingSkillDb link1 = DomainBuilderDatabase.getJobPostingSkillDb(42L, 1L, null);
        JobPostingSkillDb link2 = DomainBuilderDatabase.getJobPostingSkillDb(99L, 1L, null);

        repository.save(link1);
        repository.save(link2);

        // Act
        List<JobPostingSkillDb> result = repository.findByJobPostingId(42L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).getJobPostingId());
    }

    @Test
    void findBySkillId_shouldReturnMatchingLinks() {
        // Arrange
        JobPostingSkillDb link1 = DomainBuilderDatabase.getJobPostingSkillDb(1L, 42L, null);
        JobPostingSkillDb link2 = DomainBuilderDatabase.getJobPostingSkillDb(1L, 99L, null);

        repository.save(link1);
        repository.save(link2);

        // Act
        List<JobPostingSkillDb> result = repository.findBySkillId(42L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).getSkillId());
    }

    @Test
    void save_shouldPersistJobPostingSkill() {
        // Arrange
        JobPostingSkillDb item = DomainBuilderDatabase.getJobPostingSkillDb();

        // Act
        JobPostingSkillDb saved = repository.save(item);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(item.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllJobPostingSkills() {
        // Arrange
        JobPostingSkillDb item1 = DomainBuilderDatabase.getJobPostingSkillDb();
        JobPostingSkillDb item2 = DomainBuilderDatabase.getJobPostingSkillDb();
        repository.save(item1);
        repository.save(item2);

        // Act
        List<JobPostingSkillDb> result = (List<JobPostingSkillDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveJobPostingSkill() {
        // Arrange
        JobPostingSkillDb item = DomainBuilderDatabase.getJobPostingSkillDb();
        JobPostingSkillDb saved = repository.save(item);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<JobPostingSkillDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
