package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.SponsorDb;
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
class SponsorRepositoryTest {

    @Autowired
    private SponsorRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnSponsor_whenExists() {
        // Arrange
        SponsorDb sponsor = DomainBuilderDatabase.getSponsorDb();
        repository.save(sponsor);

        // Act
        Optional<SponsorDb> result = repository.findByExtid(sponsor.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(sponsor.getExtid(), result.get().getExtid());
        assertEquals(sponsor.getName(), result.get().getName());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<SponsorDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByName_shouldReturnSponsor_whenExists() {
        // Arrange
        SponsorDb sponsor = DomainBuilderDatabase.getSponsorDb("Acme Pharma", null);
        repository.save(sponsor);

        // Act
        Optional<SponsorDb> result = repository.findByName("Acme Pharma");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(sponsor.getExtid(), result.get().getExtid());
    }

    @Test
    void findByName_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<SponsorDb> result = repository.findByName("Nonexistent Sponsor");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        SponsorDb active1 = DomainBuilderDatabase.getSponsorDb();
        active1.setActive(ActiveEnum.ACTIVE);
        SponsorDb active2 = DomainBuilderDatabase.getSponsorDb();
        active2.setActive(ActiveEnum.ACTIVE);
        SponsorDb inactive = DomainBuilderDatabase.getSponsorDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<SponsorDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(o -> o.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        SponsorDb active = DomainBuilderDatabase.getSponsorDb();
        active.setActive(ActiveEnum.ACTIVE);
        SponsorDb inactive = DomainBuilderDatabase.getSponsorDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<SponsorDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        SponsorDb sponsor = DomainBuilderDatabase.getSponsorDb();
        repository.save(sponsor);

        // Act
        boolean result = repository.existsByExtid(sponsor.getExtid());

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
    void save_shouldPersistSponsor() {
        // Arrange
        SponsorDb sponsor = DomainBuilderDatabase.getSponsorDb();

        // Act
        SponsorDb saved = repository.save(sponsor);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(sponsor.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllSponsors() {
        // Arrange
        SponsorDb sponsor1 = DomainBuilderDatabase.getSponsorDb();
        SponsorDb sponsor2 = DomainBuilderDatabase.getSponsorDb();
        repository.save(sponsor1);
        repository.save(sponsor2);

        // Act
        List<SponsorDb> result = (List<SponsorDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveSponsor() {
        // Arrange
        SponsorDb sponsor = DomainBuilderDatabase.getSponsorDb();
        SponsorDb saved = repository.save(sponsor);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<SponsorDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
