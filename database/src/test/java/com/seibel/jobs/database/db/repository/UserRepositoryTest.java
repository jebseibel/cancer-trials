package com.seibel.jobs.database.db.repository;

import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.database.db.entity.UserDb;
import com.seibel.jobs.testutils.DomainBuilderDatabase;
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
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnUser_whenExists() {
        // Arrange
        UserDb user = DomainBuilderDatabase.getUserDb();
        repository.save(user);

        // Act
        Optional<UserDb> result = repository.findByExtid(user.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(user.getExtid(), result.get().getExtid());
        assertEquals(user.getUsername(), result.get().getUsername());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<UserDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByUsername_shouldReturnUser_whenExists() {
        // Arrange
        UserDb user = DomainBuilderDatabase.getUserDb();
        repository.save(user);

        // Act
        Optional<UserDb> result = repository.findByUsername(user.getUsername());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(user.getUsername(), result.get().getUsername());
    }

    @Test
    void findByUsername_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<UserDb> result = repository.findByUsername("nonexistent-username");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        UserDb active1 = DomainBuilderDatabase.getUserDb();
        active1.setActive(ActiveEnum.ACTIVE);
        UserDb active2 = DomainBuilderDatabase.getUserDb();
        active2.setActive(ActiveEnum.ACTIVE);
        UserDb inactive = DomainBuilderDatabase.getUserDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(u -> u.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        UserDb active = DomainBuilderDatabase.getUserDb();
        active.setActive(ActiveEnum.ACTIVE);
        UserDb inactive = DomainBuilderDatabase.getUserDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        UserDb user = DomainBuilderDatabase.getUserDb();
        repository.save(user);

        // Act
        boolean result = repository.existsByExtid(user.getExtid());

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
    void existsByUsername_shouldReturnTrue_whenExists() {
        // Arrange
        UserDb user = DomainBuilderDatabase.getUserDb();
        repository.save(user);

        // Act
        boolean result = repository.existsByUsername(user.getUsername());

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByUsername_shouldReturnFalse_whenNotExists() {
        // Act
        boolean result = repository.existsByUsername("nonexistent-username");

        // Assert
        assertFalse(result);
    }

    @Test
    void save_shouldPersistUser() {
        // Arrange
        UserDb user = DomainBuilderDatabase.getUserDb();

        // Act
        UserDb saved = repository.save(user);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(user.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        // Arrange
        UserDb user1 = DomainBuilderDatabase.getUserDb();
        UserDb user2 = DomainBuilderDatabase.getUserDb();
        repository.save(user1);
        repository.save(user2);

        // Act
        List<UserDb> result = (List<UserDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveUser() {
        // Arrange
        UserDb user = DomainBuilderDatabase.getUserDb();
        UserDb saved = repository.save(user);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<UserDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
