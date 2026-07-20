package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.SkillDb;
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
class SkillRepositoryTest {

    @Autowired
    private SkillRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnSkill_whenExists() {
        // Arrange
        SkillDb skill = DomainBuilderDatabase.getSkillDb();
        repository.save(skill);

        // Act
        Optional<SkillDb> result = repository.findByExtid(skill.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(skill.getExtid(), result.get().getExtid());
        assertEquals(skill.getName(), result.get().getName());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<SkillDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        SkillDb active1 = DomainBuilderDatabase.getSkillDb();
        active1.setActive(ActiveEnum.ACTIVE);
        SkillDb active2 = DomainBuilderDatabase.getSkillDb();
        active2.setActive(ActiveEnum.ACTIVE);
        SkillDb inactive = DomainBuilderDatabase.getSkillDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<SkillDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(c -> c.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        SkillDb active = DomainBuilderDatabase.getSkillDb();
        active.setActive(ActiveEnum.ACTIVE);
        SkillDb inactive = DomainBuilderDatabase.getSkillDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<SkillDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        SkillDb skill = DomainBuilderDatabase.getSkillDb();
        repository.save(skill);

        // Act
        boolean result = repository.existsByExtid(skill.getExtid());

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
    void findByName_shouldReturnSkill_whenExists() {
        // Arrange
        SkillDb skill = DomainBuilderDatabase.getSkillDb();
        repository.save(skill);

        // Act
        Optional<SkillDb> result = repository.findByName(skill.getName());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(skill.getName(), result.get().getName());
    }

    @Test
    void save_shouldPersistSkill() {
        // Arrange
        SkillDb skill = DomainBuilderDatabase.getSkillDb();

        // Act
        SkillDb saved = repository.save(skill);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(skill.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllSkills() {
        // Arrange
        SkillDb skill1 = DomainBuilderDatabase.getSkillDb();
        SkillDb skill2 = DomainBuilderDatabase.getSkillDb();
        repository.save(skill1);
        repository.save(skill2);

        // Act
        List<SkillDb> result = (List<SkillDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveSkill() {
        // Arrange
        SkillDb skill = DomainBuilderDatabase.getSkillDb();
        SkillDb saved = repository.save(skill);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<SkillDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
