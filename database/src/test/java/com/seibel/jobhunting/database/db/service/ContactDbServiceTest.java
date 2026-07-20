package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.Contact;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.ContactDb;
import com.seibel.jobhunting.database.db.mapper.ContactMapper;
import com.seibel.jobhunting.database.db.repository.ContactRepository;
import com.seibel.jobhunting.testutils.DomainBuilderDatabase;
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
class ContactDbServiceTest {

    @Mock
    private ContactRepository repository;

    @Mock
    private ContactMapper mapper;

    @InjectMocks
    private ContactDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        String name = "Recruiter Bob";
        ContactDb savedDb = DomainBuilderDatabase.getContactDb(name, null);
        Contact expectedDomain = DomainBuilderDatabase.getContact(savedDb);

        when(repository.save(any(ContactDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Contact result = service.create(1L, 2L, name, "Technical Recruiter", "bob@example.com", "555-1234", "Notes");

        // Assert
        assertNotNull(result);
        ArgumentCaptor<ContactDb> captor = ArgumentCaptor.forClass(ContactDb.class);
        verify(repository).save(captor.capture());

        ContactDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(name, captured.getName());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(ContactDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(null, null, "Bob", null, null, null, null));
        verify(repository).save(any(ContactDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        String name = "Updated Contact";

        ContactDb existingDb = DomainBuilderDatabase.getContactDb("Old Contact", extid);
        ContactDb updatedDb = DomainBuilderDatabase.getContactDb(name, extid);
        Contact expectedDomain = DomainBuilderDatabase.getContact(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(ContactDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Contact result = service.update(extid, 1L, 2L, name, "Hiring Manager", "new@example.com", "555-5678", "New notes");

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<ContactDb> captor = ArgumentCaptor.forClass(ContactDb.class);
        verify(repository).save(captor.capture());

        ContactDb captured = captor.getValue();
        assertEquals(name, captured.getName());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(updatedDb);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () ->
                service.update(extid, null, null, "Name", null, null, null, null));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        ContactDb existingDb = DomainBuilderDatabase.getContactDb("Bob", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(ContactDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () ->
                service.update(extid, null, null, "New Name", null, null, null, null));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        ContactDb existingDb = DomainBuilderDatabase.getContactDb("Bob", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(ContactDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<ContactDb> captor = ArgumentCaptor.forClass(ContactDb.class);
        verify(repository).save(captor.capture());

        ContactDb captured = captor.getValue();
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
    void findByExtid_shouldReturnContact_whenExists() {
        // Arrange
        String extid = "existing-extid";
        ContactDb db = DomainBuilderDatabase.getContactDb("Bob", extid);
        Contact expectedDomain = DomainBuilderDatabase.getContact(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Contact result = service.findByExtid(extid);

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
        assertThrows(ServiceException.class, () -> service.findByExtid(extid));
        verify(repository).findByExtid(extid);
        verify(mapper, never()).toModel(any());
    }

    @Test
    void findAll_shouldReturnAllContacts() {
        // Arrange
        ContactDb db1 = DomainBuilderDatabase.getContactDb();
        ContactDb db2 = DomainBuilderDatabase.getContactDb();
        List<ContactDb> dbList = Arrays.asList(db1, db2);

        Contact domain1 = DomainBuilderDatabase.getContact(db1);
        Contact domain2 = DomainBuilderDatabase.getContact(db2);
        List<Contact> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Contact> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredContacts() {
        // Arrange
        ContactDb db1 = DomainBuilderDatabase.getContactDb();
        db1.setActive(ActiveEnum.ACTIVE);
        ContactDb db2 = DomainBuilderDatabase.getContactDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<ContactDb> dbList = Arrays.asList(db1, db2);

        Contact domain1 = DomainBuilderDatabase.getContact(db1);
        Contact domain2 = DomainBuilderDatabase.getContact(db2);
        List<Contact> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Contact> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByCompanyId_shouldReturnMatchingContacts() {
        // Arrange
        Long companyId = 42L;
        ContactDb db1 = DomainBuilderDatabase.getContactDb();
        List<ContactDb> dbList = List.of(db1);

        Contact domain1 = DomainBuilderDatabase.getContact(db1);
        List<Contact> domainList = List.of(domain1);

        when(repository.findByCompanyId(companyId)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Contact> result = service.findByCompanyId(companyId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByCompanyId(companyId);
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByJobPostingId_shouldReturnMatchingContacts() {
        // Arrange
        Long jobPostingId = 42L;
        ContactDb db1 = DomainBuilderDatabase.getContactDb();
        List<ContactDb> dbList = List.of(db1);

        Contact domain1 = DomainBuilderDatabase.getContact(db1);
        List<Contact> domainList = List.of(domain1);

        when(repository.findByJobPostingId(jobPostingId)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Contact> result = service.findByJobPostingId(jobPostingId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByJobPostingId(jobPostingId);
        verify(mapper).toModelList(dbList);
    }
}
