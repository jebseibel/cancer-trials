package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.ContactDb;
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
class ContactRepositoryTest {

    @Autowired
    private ContactRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnContact_whenExists() {
        // Arrange
        ContactDb contact = DomainBuilderDatabase.getContactDb();
        repository.save(contact);

        // Act
        Optional<ContactDb> result = repository.findByExtid(contact.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(contact.getExtid(), result.get().getExtid());
        assertEquals(contact.getName(), result.get().getName());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<ContactDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        ContactDb active1 = DomainBuilderDatabase.getContactDb();
        active1.setActive(ActiveEnum.ACTIVE);
        ContactDb active2 = DomainBuilderDatabase.getContactDb();
        active2.setActive(ActiveEnum.ACTIVE);
        ContactDb inactive = DomainBuilderDatabase.getContactDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<ContactDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(c -> c.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        ContactDb active = DomainBuilderDatabase.getContactDb();
        active.setActive(ActiveEnum.ACTIVE);
        ContactDb inactive = DomainBuilderDatabase.getContactDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<ContactDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        ContactDb contact = DomainBuilderDatabase.getContactDb();
        repository.save(contact);

        // Act
        boolean result = repository.existsByExtid(contact.getExtid());

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
    void findByCompanyId_shouldReturnMatchingContacts() {
        // Arrange
        ContactDb contact1 = DomainBuilderDatabase.getContactDb();
        contact1.setCompanyId(42L);
        ContactDb contact2 = DomainBuilderDatabase.getContactDb();
        contact2.setCompanyId(99L);

        repository.save(contact1);
        repository.save(contact2);

        // Act
        List<ContactDb> result = repository.findByCompanyId(42L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).getCompanyId());
    }

    @Test
    void findByJobPostingId_shouldReturnMatchingContacts() {
        // Arrange
        ContactDb contact1 = DomainBuilderDatabase.getContactDb();
        contact1.setJobPostingId(42L);
        ContactDb contact2 = DomainBuilderDatabase.getContactDb();
        contact2.setJobPostingId(99L);

        repository.save(contact1);
        repository.save(contact2);

        // Act
        List<ContactDb> result = repository.findByJobPostingId(42L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).getJobPostingId());
    }

    @Test
    void save_shouldPersistContact() {
        // Arrange
        ContactDb contact = DomainBuilderDatabase.getContactDb();

        // Act
        ContactDb saved = repository.save(contact);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(contact.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllContacts() {
        // Arrange
        ContactDb contact1 = DomainBuilderDatabase.getContactDb();
        ContactDb contact2 = DomainBuilderDatabase.getContactDb();
        repository.save(contact1);
        repository.save(contact2);

        // Act
        List<ContactDb> result = (List<ContactDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveContact() {
        // Arrange
        ContactDb contact = DomainBuilderDatabase.getContactDb();
        ContactDb saved = repository.save(contact);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<ContactDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
