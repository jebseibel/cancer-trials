package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.KeywordDb;
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
class KeywordRepositoryTest {

    @Autowired
    private KeywordRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnKeyword_whenExists() {
        // Arrange
        KeywordDb keyword = DomainBuilderDatabase.getKeywordDb();
        repository.save(keyword);

        // Act
        Optional<KeywordDb> result = repository.findByExtid(keyword.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(keyword.getExtid(), result.get().getExtid());
        assertEquals(keyword.getName(), result.get().getName());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<KeywordDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByName_shouldReturnKeyword_whenExists() {
        // Arrange
        KeywordDb keyword = DomainBuilderDatabase.getKeywordDb();
        repository.save(keyword);

        // Act
        Optional<KeywordDb> result = repository.findByName(keyword.getName());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(keyword.getName(), result.get().getName());
    }

    @Test
    void findByName_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<KeywordDb> result = repository.findByName("nonexistent-name");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        KeywordDb active1 = DomainBuilderDatabase.getKeywordDb();
        active1.setActive(ActiveEnum.ACTIVE);
        KeywordDb active2 = DomainBuilderDatabase.getKeywordDb();
        active2.setActive(ActiveEnum.ACTIVE);
        KeywordDb inactive = DomainBuilderDatabase.getKeywordDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<KeywordDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(k -> k.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        KeywordDb active = DomainBuilderDatabase.getKeywordDb();
        active.setActive(ActiveEnum.ACTIVE);
        KeywordDb inactive = DomainBuilderDatabase.getKeywordDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<KeywordDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        KeywordDb keyword = DomainBuilderDatabase.getKeywordDb();
        repository.save(keyword);

        // Act
        boolean result = repository.existsByExtid(keyword.getExtid());

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
    void save_shouldPersistKeyword() {
        // Arrange
        KeywordDb keyword = DomainBuilderDatabase.getKeywordDb();

        // Act
        KeywordDb saved = repository.save(keyword);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(keyword.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllKeywords() {
        // Arrange
        KeywordDb keyword1 = DomainBuilderDatabase.getKeywordDb();
        KeywordDb keyword2 = DomainBuilderDatabase.getKeywordDb();
        repository.save(keyword1);
        repository.save(keyword2);

        // Act
        List<KeywordDb> result = (List<KeywordDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveKeyword() {
        // Arrange
        KeywordDb keyword = DomainBuilderDatabase.getKeywordDb();
        KeywordDb saved = repository.save(keyword);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<KeywordDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
