package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.FriendCompanyDb;
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
class FriendCompanyRepositoryTest {

    @Autowired
    private FriendCompanyRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnFriendCompany_whenExists() {
        // Arrange
        FriendCompanyDb item = DomainBuilderDatabase.getFriendCompanyDb();
        repository.save(item);

        // Act
        Optional<FriendCompanyDb> result = repository.findByExtid(item.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(item.getExtid(), result.get().getExtid());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<FriendCompanyDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        FriendCompanyDb active1 = DomainBuilderDatabase.getFriendCompanyDb();
        active1.setActive(ActiveEnum.ACTIVE);
        FriendCompanyDb active2 = DomainBuilderDatabase.getFriendCompanyDb();
        active2.setActive(ActiveEnum.ACTIVE);
        FriendCompanyDb inactive = DomainBuilderDatabase.getFriendCompanyDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<FriendCompanyDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(c -> c.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        FriendCompanyDb active = DomainBuilderDatabase.getFriendCompanyDb();
        active.setActive(ActiveEnum.ACTIVE);
        FriendCompanyDb inactive = DomainBuilderDatabase.getFriendCompanyDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<FriendCompanyDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        FriendCompanyDb item = DomainBuilderDatabase.getFriendCompanyDb();
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
    void findByFriendId_shouldReturnMatchingLinks() {
        // Arrange
        FriendCompanyDb link1 = DomainBuilderDatabase.getFriendCompanyDb(42L, 1L, null);
        FriendCompanyDb link2 = DomainBuilderDatabase.getFriendCompanyDb(99L, 1L, null);

        repository.save(link1);
        repository.save(link2);

        // Act
        List<FriendCompanyDb> result = repository.findByFriendId(42L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).getFriendId());
    }

    @Test
    void findByCompanyId_shouldReturnMatchingLinks() {
        // Arrange
        FriendCompanyDb link1 = DomainBuilderDatabase.getFriendCompanyDb(1L, 42L, null);
        FriendCompanyDb link2 = DomainBuilderDatabase.getFriendCompanyDb(1L, 99L, null);

        repository.save(link1);
        repository.save(link2);

        // Act
        List<FriendCompanyDb> result = repository.findByCompanyId(42L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).getCompanyId());
    }

    @Test
    void save_shouldPersistFriendCompany() {
        // Arrange
        FriendCompanyDb item = DomainBuilderDatabase.getFriendCompanyDb();

        // Act
        FriendCompanyDb saved = repository.save(item);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(item.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllFriendCompanies() {
        // Arrange
        FriendCompanyDb item1 = DomainBuilderDatabase.getFriendCompanyDb();
        FriendCompanyDb item2 = DomainBuilderDatabase.getFriendCompanyDb();
        repository.save(item1);
        repository.save(item2);

        // Act
        List<FriendCompanyDb> result = (List<FriendCompanyDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveFriendCompany() {
        // Arrange
        FriendCompanyDb item = DomainBuilderDatabase.getFriendCompanyDb();
        FriendCompanyDb saved = repository.save(item);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<FriendCompanyDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
