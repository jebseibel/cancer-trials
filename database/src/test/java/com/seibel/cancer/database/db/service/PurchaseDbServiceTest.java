package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Purchase;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.PurchaseDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.PurchaseMapper;
import com.seibel.cancer.database.db.repository.PurchaseRepository;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseDbServiceTest {

    @Mock
    private PurchaseRepository repository;

    @Mock
    private PurchaseMapper mapper;

    @InjectMocks
    private PurchaseDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        String customer = "CUST001";
        String items = "Item A, Item B";
        String status = "PENDING";
        PurchaseDb savedDb = DomainBuilderDatabase.getPurchaseDb(customer, items, status, null);
        Purchase expectedDomain = DomainBuilderDatabase.getPurchase(savedDb);

        when(repository.save(any(PurchaseDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Purchase result = service.create(customer, items, status);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<PurchaseDb> captor = ArgumentCaptor.forClass(PurchaseDb.class);
        verify(repository).save(captor.capture());

        PurchaseDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(customer, captured.getCustomer());
        assertEquals(items, captured.getItems());
        assertEquals(status, captured.getStatus());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(PurchaseDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.create("CUST001", "Items", "Status");
        });
        verify(repository).save(any(PurchaseDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        String customer = "UPDATED_CUST";
        String items = "Updated Items";
        String status = "UPDATED_STATUS";

        PurchaseDb existingDb = DomainBuilderDatabase.getPurchaseDb("OLD_CUST", "Old Items", "Old Status", extid);
        PurchaseDb updatedDb = DomainBuilderDatabase.getPurchaseDb(customer, items, status, extid);
        Purchase expectedDomain = DomainBuilderDatabase.getPurchase(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(PurchaseDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Purchase result = service.update(extid, customer, items, status);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<PurchaseDb> captor = ArgumentCaptor.forClass(PurchaseDb.class);
        verify(repository).save(captor.capture());

        PurchaseDb captured = captor.getValue();
        assertEquals(customer, captured.getCustomer());
        assertEquals(items, captured.getItems());
        assertEquals(status, captured.getStatus());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(updatedDb);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.update(extid, "CUST", "Items", "Status");
        });
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        PurchaseDb existingDb = DomainBuilderDatabase.getPurchaseDb("CUST", "Items", "Status", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(PurchaseDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.update(extid, "NEW_CUST", "New Items", "New Status");
        });
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        PurchaseDb existingDb = DomainBuilderDatabase.getPurchaseDb("CUST", "Items", "Status", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(PurchaseDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<PurchaseDb> captor = ArgumentCaptor.forClass(PurchaseDb.class);
        verify(repository).save(captor.capture());

        PurchaseDb captured = captor.getValue();
        assertNotNull(captured.getDeletedAt());
        assertEquals(ActiveEnum.INACTIVE, captured.getActive());
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.delete(extid);
        });
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
    }

    @Test
    void findByExtid_shouldReturnPurchase_whenExists() {
        // Arrange
        String extid = "existing-extid";
        PurchaseDb db = DomainBuilderDatabase.getPurchaseDb("CUST", "Items", "Status", extid);
        Purchase expectedDomain = DomainBuilderDatabase.getPurchase(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Purchase result = service.findByExtid(extid);

        // Assert
        assertNotNull(result);
        assertEquals(expectedDomain.getExtid(), result.getExtid());
        verify(repository).findByExtid(extid);
        verify(mapper).toModel(db);
    }

    @Test
    void findByExtid_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.findByExtid(extid);
        });
        verify(repository).findByExtid(extid);
        verify(mapper, never()).toModel(any());
    }

    @Test
    void findAll_shouldReturnAllPurchases() {
        // Arrange
        PurchaseDb db1 = DomainBuilderDatabase.getPurchaseDb();
        PurchaseDb db2 = DomainBuilderDatabase.getPurchaseDb();
        List<PurchaseDb> dbList = Arrays.asList(db1, db2);

        Purchase domain1 = DomainBuilderDatabase.getPurchase(db1);
        Purchase domain2 = DomainBuilderDatabase.getPurchase(db2);
        List<Purchase> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Purchase> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredPurchases() {
        // Arrange
        PurchaseDb db1 = DomainBuilderDatabase.getPurchaseDb();
        db1.setActive(ActiveEnum.ACTIVE);
        PurchaseDb db2 = DomainBuilderDatabase.getPurchaseDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<PurchaseDb> dbList = Arrays.asList(db1, db2);

        Purchase domain1 = DomainBuilderDatabase.getPurchase(db1);
        Purchase domain2 = DomainBuilderDatabase.getPurchase(db2);
        List<Purchase> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(any(ActiveEnum.class))).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Purchase> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(any(ActiveEnum.class));
        verify(mapper).toModelList(dbList);
    }
}
