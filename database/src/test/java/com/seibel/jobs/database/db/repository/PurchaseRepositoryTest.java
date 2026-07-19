package com.seibel.jobs.database.db.repository;

import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.database.db.entity.PurchaseDb;
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
class PurchaseRepositoryTest {

    @Autowired
    private PurchaseRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnPurchase_whenExists() {
        // Arrange
        PurchaseDb purchase = DomainBuilderDatabase.getPurchaseDb();
        repository.save(purchase);

        // Act
        Optional<PurchaseDb> result = repository.findByExtid(purchase.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(purchase.getExtid(), result.get().getExtid());
        assertEquals(purchase.getCustomer(), result.get().getCustomer());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<PurchaseDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        PurchaseDb active1 = DomainBuilderDatabase.getPurchaseDb();
        active1.setActive(ActiveEnum.ACTIVE);
        PurchaseDb active2 = DomainBuilderDatabase.getPurchaseDb();
        active2.setActive(ActiveEnum.ACTIVE);
        PurchaseDb inactive = DomainBuilderDatabase.getPurchaseDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<PurchaseDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(o -> o.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        PurchaseDb active = DomainBuilderDatabase.getPurchaseDb();
        active.setActive(ActiveEnum.ACTIVE);
        PurchaseDb inactive = DomainBuilderDatabase.getPurchaseDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<PurchaseDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        PurchaseDb purchase = DomainBuilderDatabase.getPurchaseDb();
        repository.save(purchase);

        // Act
        boolean result = repository.existsByExtid(purchase.getExtid());

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
    void save_shouldPersistPurchase() {
        // Arrange
        PurchaseDb purchase = DomainBuilderDatabase.getPurchaseDb();

        // Act
        PurchaseDb saved = repository.save(purchase);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(purchase.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllPurchases() {
        // Arrange
        PurchaseDb purchase1 = DomainBuilderDatabase.getPurchaseDb();
        PurchaseDb purchase2 = DomainBuilderDatabase.getPurchaseDb();
        repository.save(purchase1);
        repository.save(purchase2);

        // Act
        List<PurchaseDb> result = (List<PurchaseDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemovePurchase() {
        // Arrange
        PurchaseDb purchase = DomainBuilderDatabase.getPurchaseDb();
        PurchaseDb saved = repository.save(purchase);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<PurchaseDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
