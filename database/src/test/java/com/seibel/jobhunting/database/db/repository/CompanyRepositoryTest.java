package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.CompanyDb;
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
class CompanyRepositoryTest {

    @Autowired
    private CompanyRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnCompany_whenExists() {
        // Arrange
        CompanyDb company = DomainBuilderDatabase.getCompanyDb();
        repository.save(company);

        // Act
        Optional<CompanyDb> result = repository.findByExtid(company.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(company.getExtid(), result.get().getExtid());
        assertEquals(company.getName(), result.get().getName());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<CompanyDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        CompanyDb active1 = DomainBuilderDatabase.getCompanyDb();
        active1.setActive(ActiveEnum.ACTIVE);
        CompanyDb active2 = DomainBuilderDatabase.getCompanyDb();
        active2.setActive(ActiveEnum.ACTIVE);
        CompanyDb inactive = DomainBuilderDatabase.getCompanyDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<CompanyDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(c -> c.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        CompanyDb active = DomainBuilderDatabase.getCompanyDb();
        active.setActive(ActiveEnum.ACTIVE);
        CompanyDb inactive = DomainBuilderDatabase.getCompanyDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<CompanyDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        CompanyDb company = DomainBuilderDatabase.getCompanyDb();
        repository.save(company);

        // Act
        boolean result = repository.existsByExtid(company.getExtid());

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
    void findByName_shouldReturnCompany_whenExists() {
        // Arrange
        CompanyDb company = DomainBuilderDatabase.getCompanyDb();
        repository.save(company);

        // Act
        Optional<CompanyDb> result = repository.findByName(company.getName());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(company.getName(), result.get().getName());
    }

    @Test
    void save_shouldPersistCompany() {
        // Arrange
        CompanyDb company = DomainBuilderDatabase.getCompanyDb();

        // Act
        CompanyDb saved = repository.save(company);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(company.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllCompanies() {
        // Arrange
        CompanyDb company1 = DomainBuilderDatabase.getCompanyDb();
        CompanyDb company2 = DomainBuilderDatabase.getCompanyDb();
        repository.save(company1);
        repository.save(company2);

        // Act
        List<CompanyDb> result = (List<CompanyDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveCompany() {
        // Arrange
        CompanyDb company = DomainBuilderDatabase.getCompanyDb();
        CompanyDb saved = repository.save(company);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<CompanyDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
