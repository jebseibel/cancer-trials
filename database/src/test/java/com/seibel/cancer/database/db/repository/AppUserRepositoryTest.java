package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.AppUserDb;
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
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnAppUser_whenExists() {
        // Arrange
        AppUserDb appUser = DomainBuilderDatabase.getAppUserDb();
        repository.save(appUser);

        // Act
        Optional<AppUserDb> result = repository.findByExtid(appUser.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(appUser.getExtid(), result.get().getExtid());
        assertEquals(appUser.getUsername(), result.get().getUsername());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<AppUserDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByUsername_shouldReturnAppUser_whenExists() {
        // Arrange
        AppUserDb appUser = DomainBuilderDatabase.getAppUserDb();
        repository.save(appUser);

        // Act
        Optional<AppUserDb> result = repository.findByUsername(appUser.getUsername());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(appUser.getUsername(), result.get().getUsername());
    }

    @Test
    void findByUsername_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<AppUserDb> result = repository.findByUsername("nonexistent-username");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        AppUserDb active1 = DomainBuilderDatabase.getAppUserDb();
        active1.setActive(ActiveEnum.ACTIVE);
        AppUserDb active2 = DomainBuilderDatabase.getAppUserDb();
        active2.setActive(ActiveEnum.ACTIVE);
        AppUserDb inactive = DomainBuilderDatabase.getAppUserDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<AppUserDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(u -> u.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        AppUserDb active = DomainBuilderDatabase.getAppUserDb();
        active.setActive(ActiveEnum.ACTIVE);
        AppUserDb inactive = DomainBuilderDatabase.getAppUserDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<AppUserDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        AppUserDb appUser = DomainBuilderDatabase.getAppUserDb();
        repository.save(appUser);

        // Act
        boolean result = repository.existsByExtid(appUser.getExtid());

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
    void save_shouldPersistAppUser() {
        // Arrange
        AppUserDb appUser = DomainBuilderDatabase.getAppUserDb();

        // Act
        AppUserDb saved = repository.save(appUser);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(appUser.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllAppUsers() {
        // Arrange
        AppUserDb appUser1 = DomainBuilderDatabase.getAppUserDb();
        AppUserDb appUser2 = DomainBuilderDatabase.getAppUserDb();
        repository.save(appUser1);
        repository.save(appUser2);

        // Act
        List<AppUserDb> result = (List<AppUserDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveAppUser() {
        // Arrange
        AppUserDb appUser = DomainBuilderDatabase.getAppUserDb();
        AppUserDb saved = repository.save(appUser);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<AppUserDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
