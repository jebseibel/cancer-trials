package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Keyword;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.KeywordDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.KeywordMapper;
import com.seibel.cancer.database.db.repository.KeywordRepository;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
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
class KeywordDbServiceTest {

    @Mock
    private KeywordRepository repository;

    @Mock
    private KeywordMapper mapper;

    @InjectMocks
    private KeywordDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        String name = "Test Keyword";
        KeywordDb savedDb = DomainBuilderDatabase.getKeywordDb(name, null);
        Keyword expectedDomain = DomainBuilderDatabase.getKeyword(savedDb);

        when(repository.save(any(KeywordDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Keyword result = service.create(name);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<KeywordDb> captor = ArgumentCaptor.forClass(KeywordDb.class);
        verify(repository).save(captor.capture());

        KeywordDb captured = captor.getValue();
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
        when(repository.save(any(KeywordDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.create("Some Keyword");
        });
        verify(repository).save(any(KeywordDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        String name = "Updated Name";

        KeywordDb existingDb = DomainBuilderDatabase.getKeywordDb("Old Name", extid);
        KeywordDb updatedDb = DomainBuilderDatabase.getKeywordDb(name, extid);
        Keyword expectedDomain = DomainBuilderDatabase.getKeyword(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(KeywordDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Keyword result = service.update(extid, name);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<KeywordDb> captor = ArgumentCaptor.forClass(KeywordDb.class);
        verify(repository).save(captor.capture());

        KeywordDb captured = captor.getValue();
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
        assertThrows(ServiceException.class, () -> {
            service.update(extid, "New Name");
        });
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        KeywordDb existingDb = DomainBuilderDatabase.getKeywordDb("Name", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(KeywordDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.update(extid, "New Name");
        });
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        KeywordDb existingDb = DomainBuilderDatabase.getKeywordDb("Name", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(KeywordDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<KeywordDb> captor = ArgumentCaptor.forClass(KeywordDb.class);
        verify(repository).save(captor.capture());

        KeywordDb captured = captor.getValue();
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
    void findByExtid_shouldReturnEntity_whenExists() {
        // Arrange
        String extid = "existing-extid";
        KeywordDb db = DomainBuilderDatabase.getKeywordDb("Name", extid);
        Keyword expectedDomain = DomainBuilderDatabase.getKeyword(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Keyword result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllEntities() {
        // Arrange
        KeywordDb db1 = DomainBuilderDatabase.getKeywordDb();
        KeywordDb db2 = DomainBuilderDatabase.getKeywordDb();
        List<KeywordDb> dbList = Arrays.asList(db1, db2);

        Keyword domain1 = DomainBuilderDatabase.getKeyword(db1);
        Keyword domain2 = DomainBuilderDatabase.getKeyword(db2);
        List<Keyword> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Keyword> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        // Arrange
        KeywordDb db1 = DomainBuilderDatabase.getKeywordDb();
        db1.setActive(ActiveEnum.ACTIVE);
        KeywordDb db2 = DomainBuilderDatabase.getKeywordDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<KeywordDb> dbList = Arrays.asList(db1, db2);

        Keyword domain1 = DomainBuilderDatabase.getKeyword(db1);
        Keyword domain2 = DomainBuilderDatabase.getKeyword(db2);
        List<Keyword> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Keyword> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }
}
