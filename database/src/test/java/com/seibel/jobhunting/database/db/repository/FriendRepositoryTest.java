package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.FriendDb;
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
class FriendRepositoryTest {

    @Autowired
    private FriendRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnFriend_whenExists() {
        // Arrange
        FriendDb friend = DomainBuilderDatabase.getFriendDb();
        repository.save(friend);

        // Act
        Optional<FriendDb> result = repository.findByExtid(friend.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(friend.getExtid(), result.get().getExtid());
        assertEquals(friend.getName(), result.get().getName());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<FriendDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        FriendDb active1 = DomainBuilderDatabase.getFriendDb();
        active1.setActive(ActiveEnum.ACTIVE);
        FriendDb active2 = DomainBuilderDatabase.getFriendDb();
        active2.setActive(ActiveEnum.ACTIVE);
        FriendDb inactive = DomainBuilderDatabase.getFriendDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<FriendDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(c -> c.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        FriendDb active = DomainBuilderDatabase.getFriendDb();
        active.setActive(ActiveEnum.ACTIVE);
        FriendDb inactive = DomainBuilderDatabase.getFriendDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<FriendDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        FriendDb friend = DomainBuilderDatabase.getFriendDb();
        repository.save(friend);

        // Act
        boolean result = repository.existsByExtid(friend.getExtid());

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
    void save_shouldPersistFriend() {
        // Arrange
        FriendDb friend = DomainBuilderDatabase.getFriendDb();

        // Act
        FriendDb saved = repository.save(friend);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(friend.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllFriends() {
        // Arrange
        FriendDb friend1 = DomainBuilderDatabase.getFriendDb();
        FriendDb friend2 = DomainBuilderDatabase.getFriendDb();
        repository.save(friend1);
        repository.save(friend2);

        // Act
        List<FriendDb> result = (List<FriendDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveFriend() {
        // Arrange
        FriendDb friend = DomainBuilderDatabase.getFriendDb();
        FriendDb saved = repository.save(friend);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<FriendDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
