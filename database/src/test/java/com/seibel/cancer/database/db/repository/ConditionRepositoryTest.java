package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.ConditionDb;
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
class ConditionRepositoryTest {

    @Autowired
    private ConditionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnCondition_whenExists() {
        // Arrange
        ConditionDb condition = DomainBuilderDatabase.getConditionDb();
        repository.save(condition);

        // Act
        Optional<ConditionDb> result = repository.findByExtid(condition.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(condition.getExtid(), result.get().getExtid());
        assertEquals(condition.getName(), result.get().getName());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<ConditionDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByName_shouldReturnCondition_whenExists() {
        // Arrange
        ConditionDb condition = DomainBuilderDatabase.getConditionDb();
        repository.save(condition);

        // Act
        Optional<ConditionDb> result = repository.findByName(condition.getName());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(condition.getName(), result.get().getName());
    }

    @Test
    void findByName_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<ConditionDb> result = repository.findByName("nonexistent-name");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        ConditionDb active1 = DomainBuilderDatabase.getConditionDb();
        active1.setActive(ActiveEnum.ACTIVE);
        ConditionDb active2 = DomainBuilderDatabase.getConditionDb();
        active2.setActive(ActiveEnum.ACTIVE);
        ConditionDb inactive = DomainBuilderDatabase.getConditionDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<ConditionDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(c -> c.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        ConditionDb active = DomainBuilderDatabase.getConditionDb();
        active.setActive(ActiveEnum.ACTIVE);
        ConditionDb inactive = DomainBuilderDatabase.getConditionDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<ConditionDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        ConditionDb condition = DomainBuilderDatabase.getConditionDb();
        repository.save(condition);

        // Act
        boolean result = repository.existsByExtid(condition.getExtid());

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
    void save_shouldPersistCondition() {
        // Arrange
        ConditionDb condition = DomainBuilderDatabase.getConditionDb();

        // Act
        ConditionDb saved = repository.save(condition);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(condition.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllConditions() {
        // Arrange
        ConditionDb condition1 = DomainBuilderDatabase.getConditionDb();
        ConditionDb condition2 = DomainBuilderDatabase.getConditionDb();
        repository.save(condition1);
        repository.save(condition2);

        // Act
        List<ConditionDb> result = (List<ConditionDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveCondition() {
        // Arrange
        ConditionDb condition = DomainBuilderDatabase.getConditionDb();
        ConditionDb saved = repository.save(condition);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<ConditionDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
