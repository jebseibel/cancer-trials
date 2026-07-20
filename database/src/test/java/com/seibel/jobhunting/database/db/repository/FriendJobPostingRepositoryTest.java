package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.FriendJobPostingDb;
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
class FriendJobPostingRepositoryTest {

    @Autowired
    private FriendJobPostingRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnFriendJobPosting_whenExists() {
        // Arrange
        FriendJobPostingDb item = DomainBuilderDatabase.getFriendJobPostingDb();
        repository.save(item);

        // Act
        Optional<FriendJobPostingDb> result = repository.findByExtid(item.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(item.getExtid(), result.get().getExtid());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<FriendJobPostingDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        FriendJobPostingDb active1 = DomainBuilderDatabase.getFriendJobPostingDb();
        active1.setActive(ActiveEnum.ACTIVE);
        FriendJobPostingDb active2 = DomainBuilderDatabase.getFriendJobPostingDb();
        active2.setActive(ActiveEnum.ACTIVE);
        FriendJobPostingDb inactive = DomainBuilderDatabase.getFriendJobPostingDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<FriendJobPostingDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(c -> c.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        FriendJobPostingDb active = DomainBuilderDatabase.getFriendJobPostingDb();
        active.setActive(ActiveEnum.ACTIVE);
        FriendJobPostingDb inactive = DomainBuilderDatabase.getFriendJobPostingDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<FriendJobPostingDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        FriendJobPostingDb item = DomainBuilderDatabase.getFriendJobPostingDb();
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
        FriendJobPostingDb link1 = DomainBuilderDatabase.getFriendJobPostingDb(42L, 1L, null);
        FriendJobPostingDb link2 = DomainBuilderDatabase.getFriendJobPostingDb(99L, 1L, null);

        repository.save(link1);
        repository.save(link2);

        // Act
        List<FriendJobPostingDb> result = repository.findByFriendId(42L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).getFriendId());
    }

    @Test
    void findByJobPostingId_shouldReturnMatchingLinks() {
        // Arrange
        FriendJobPostingDb link1 = DomainBuilderDatabase.getFriendJobPostingDb(1L, 42L, null);
        FriendJobPostingDb link2 = DomainBuilderDatabase.getFriendJobPostingDb(1L, 99L, null);

        repository.save(link1);
        repository.save(link2);

        // Act
        List<FriendJobPostingDb> result = repository.findByJobPostingId(42L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).getJobPostingId());
    }

    @Test
    void save_shouldPersistFriendJobPosting() {
        // Arrange
        FriendJobPostingDb item = DomainBuilderDatabase.getFriendJobPostingDb();

        // Act
        FriendJobPostingDb saved = repository.save(item);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(item.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllFriendJobPostings() {
        // Arrange
        FriendJobPostingDb item1 = DomainBuilderDatabase.getFriendJobPostingDb();
        FriendJobPostingDb item2 = DomainBuilderDatabase.getFriendJobPostingDb();
        repository.save(item1);
        repository.save(item2);

        // Act
        List<FriendJobPostingDb> result = (List<FriendJobPostingDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveFriendJobPosting() {
        // Arrange
        FriendJobPostingDb item = DomainBuilderDatabase.getFriendJobPostingDb();
        FriendJobPostingDb saved = repository.save(item);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<FriendJobPostingDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
