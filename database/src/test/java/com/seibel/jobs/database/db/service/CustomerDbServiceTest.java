package com.seibel.jobs.database.db.service;

import com.seibel.jobs.common.domain.Customer;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.database.db.entity.CustomerDb;
import com.seibel.jobs.common.exceptions.ServiceException;
import com.seibel.jobs.database.db.mapper.CustomerMapper;
import com.seibel.jobs.database.db.repository.CustomerRepository;
import com.seibel.jobs.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerDbServiceTest {

    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerMapper mapper;

    @InjectMocks
    private CustomerDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        String code = "TEST";
        String name = "Test Customer";
        String contactName = "John Doe";
        String description = "Test Description";
        String contactEmail = "john@example.com";
        String contactPhone = "555-1234";
        CustomerDb savedDb = DomainBuilderDatabase.getCustomerDb(code, name, contactName, description, contactEmail, contactPhone, null);
        Customer expectedDomain = DomainBuilderDatabase.getCustomer(savedDb);

        when(repository.save(any(CustomerDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Customer result = service.create(code, name, contactName, description, contactEmail, contactPhone);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<CustomerDb> captor = ArgumentCaptor.forClass(CustomerDb.class);
        verify(repository).save(captor.capture());

        CustomerDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(code, captured.getCode());
        assertEquals(name, captured.getName());
        assertEquals(contactName, captured.getContactName());
        assertEquals(description, captured.getDescription());
        assertEquals(contactEmail, captured.getContactEmail());
        assertEquals(contactPhone, captured.getContactPhone());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(CustomerDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.create("CODE", "Name", "Contact", "Description", "email@test.com", "555-1234");
        });
        verify(repository).save(any(CustomerDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        String code = "UPDATED";
        String name = "Updated Name";
        String contactName = "Jane Doe";
        String description = "Updated Description";
        String contactEmail = "jane@example.com";
        String contactPhone = "555-5678";

        CustomerDb existingDb = DomainBuilderDatabase.getCustomerDb("OLD", "Old Name", "Old Contact", "Old Description", "old@test.com", "555-0000", extid);
        CustomerDb updatedDb = DomainBuilderDatabase.getCustomerDb(code, name, contactName, description, contactEmail, contactPhone, extid);
        Customer expectedDomain = DomainBuilderDatabase.getCustomer(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(CustomerDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Customer result = service.update(extid, code, name, contactName, description, contactEmail, contactPhone);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<CustomerDb> captor = ArgumentCaptor.forClass(CustomerDb.class);
        verify(repository).save(captor.capture());

        CustomerDb captured = captor.getValue();
        assertEquals(code, captured.getCode());
        assertEquals(name, captured.getName());
        assertEquals(contactName, captured.getContactName());
        assertEquals(description, captured.getDescription());
        assertEquals(contactEmail, captured.getContactEmail());
        assertEquals(contactPhone, captured.getContactPhone());
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
            service.update(extid, "CODE", "Name", "Contact", "Description", "email@test.com", "555-1234");
        });
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        CustomerDb existingDb = DomainBuilderDatabase.getCustomerDb("CODE", "Name", "Contact", "Description", "email@test.com", "555-1234", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(CustomerDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.update(extid, "NEW", "New Name", "New Contact", "New Description", "new@test.com", "555-5678");
        });
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        CustomerDb existingDb = DomainBuilderDatabase.getCustomerDb("CODE", "Name", "Contact", "Description", "email@test.com", "555-1234", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(CustomerDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<CustomerDb> captor = ArgumentCaptor.forClass(CustomerDb.class);
        verify(repository).save(captor.capture());

        CustomerDb captured = captor.getValue();
        assertNotNull(captured.getDeletedAt());
        assertEquals(ActiveEnum.INACTIVE, captured.getActive());
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.delete(extid));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
    }

    @Test
    void findByExtid_shouldReturnCustomer_whenExists() {
        // Arrange
        String extid = "existing-extid";
        CustomerDb db = DomainBuilderDatabase.getCustomerDb("CODE", "Name", "Contact", "Description", "email@test.com", "555-1234", extid);
        Customer expectedDomain = DomainBuilderDatabase.getCustomer(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Customer result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllCustomers() {
        // Arrange
        CustomerDb db1 = DomainBuilderDatabase.getCustomerDb();
        CustomerDb db2 = DomainBuilderDatabase.getCustomerDb();
        List<CustomerDb> dbList = Arrays.asList(db1, db2);

        Customer domain1 = DomainBuilderDatabase.getCustomer(db1);
        Customer domain2 = DomainBuilderDatabase.getCustomer(db2);
        List<Customer> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Customer> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredCustomers() {
        // Arrange
        CustomerDb db1 = DomainBuilderDatabase.getCustomerDb();
        db1.setActive(ActiveEnum.ACTIVE);
        CustomerDb db2 = DomainBuilderDatabase.getCustomerDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<CustomerDb> dbList = Arrays.asList(db1, db2);

        Customer domain1 = DomainBuilderDatabase.getCustomer(db1);
        Customer domain2 = DomainBuilderDatabase.getCustomer(db2);
        List<Customer> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Customer> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }
}
