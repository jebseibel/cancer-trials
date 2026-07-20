package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.Company;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.CompanyDb;
import com.seibel.jobhunting.database.db.mapper.CompanyMapper;
import com.seibel.jobhunting.database.db.repository.CompanyRepository;
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
class CompanyDbServiceTest {

    @Mock
    private CompanyRepository repository;

    @Mock
    private CompanyMapper mapper;

    @InjectMocks
    private CompanyDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        String name = "Acme Corp";
        CompanyDb savedDb = DomainBuilderDatabase.getCompanyDb(name, null);
        Company expectedDomain = DomainBuilderDatabase.getCompany(savedDb);

        when(repository.save(any(CompanyDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Company result = service.create(name, "https://acme.example.com", "Software", "Notes");

        // Assert
        assertNotNull(result);
        ArgumentCaptor<CompanyDb> captor = ArgumentCaptor.forClass(CompanyDb.class);
        verify(repository).save(captor.capture());

        CompanyDb captured = captor.getValue();
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
        when(repository.save(any(CompanyDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create("Acme", null, null, null));
        verify(repository).save(any(CompanyDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        String name = "Updated Corp";

        CompanyDb existingDb = DomainBuilderDatabase.getCompanyDb("Old Corp", extid);
        CompanyDb updatedDb = DomainBuilderDatabase.getCompanyDb(name, extid);
        Company expectedDomain = DomainBuilderDatabase.getCompany(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(CompanyDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Company result = service.update(extid, name, "https://new.example.com", "Finance", "New notes");

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<CompanyDb> captor = ArgumentCaptor.forClass(CompanyDb.class);
        verify(repository).save(captor.capture());

        CompanyDb captured = captor.getValue();
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
        assertThrows(ServiceException.class, () -> service.update(extid, "Name", null, null, null));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        CompanyDb existingDb = DomainBuilderDatabase.getCompanyDb("Acme", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(CompanyDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, "New Name", null, null, null));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        CompanyDb existingDb = DomainBuilderDatabase.getCompanyDb("Acme", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(CompanyDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<CompanyDb> captor = ArgumentCaptor.forClass(CompanyDb.class);
        verify(repository).save(captor.capture());

        CompanyDb captured = captor.getValue();
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
    void findByExtid_shouldReturnCompany_whenExists() {
        // Arrange
        String extid = "existing-extid";
        CompanyDb db = DomainBuilderDatabase.getCompanyDb("Acme", extid);
        Company expectedDomain = DomainBuilderDatabase.getCompany(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Company result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllCompanies() {
        // Arrange
        CompanyDb db1 = DomainBuilderDatabase.getCompanyDb();
        CompanyDb db2 = DomainBuilderDatabase.getCompanyDb();
        List<CompanyDb> dbList = Arrays.asList(db1, db2);

        Company domain1 = DomainBuilderDatabase.getCompany(db1);
        Company domain2 = DomainBuilderDatabase.getCompany(db2);
        List<Company> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Company> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredCompanies() {
        // Arrange
        CompanyDb db1 = DomainBuilderDatabase.getCompanyDb();
        db1.setActive(ActiveEnum.ACTIVE);
        CompanyDb db2 = DomainBuilderDatabase.getCompanyDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<CompanyDb> dbList = Arrays.asList(db1, db2);

        Company domain1 = DomainBuilderDatabase.getCompany(db1);
        Company domain2 = DomainBuilderDatabase.getCompany(db2);
        List<Company> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Company> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }
}
