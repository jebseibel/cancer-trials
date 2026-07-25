package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.EligibilityRule;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.EligibilityRuleDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.EligibilityRuleMapper;
import com.seibel.cancer.database.db.repository.EligibilityRuleRepository;
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
class EligibilityRuleDbServiceTest {

    @Mock
    private EligibilityRuleRepository repository;

    @Mock
    private EligibilityRuleMapper mapper;

    @InjectMocks
    private EligibilityRuleDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        EligibilityRule input = DomainBuilderDatabase.getEligibilityRule();
        EligibilityRuleDb savedDb = DomainBuilderDatabase.getEligibilityRuleDb(input.getTrialId(), input.getNodeType());
        EligibilityRule expectedDomain = DomainBuilderDatabase.getEligibilityRule(savedDb);

        when(mapper.toDb(input)).thenReturn(new EligibilityRuleDb());
        when(repository.save(any(EligibilityRuleDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        EligibilityRule result = service.create(input);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<EligibilityRuleDb> captor = ArgumentCaptor.forClass(EligibilityRuleDb.class);
        verify(repository).save(captor.capture());

        EligibilityRuleDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        EligibilityRule input = DomainBuilderDatabase.getEligibilityRule();
        when(mapper.toDb(input)).thenReturn(new EligibilityRuleDb());
        when(repository.save(any(EligibilityRuleDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(input));
        verify(repository).save(any(EligibilityRuleDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        EligibilityRuleDb existingDb = DomainBuilderDatabase.getEligibilityRuleDb(100L, "GROUP", "AND", extid);

        EligibilityRule changes = EligibilityRule.builder()
                .nodeType("CRITERION")
                .operator("OR")
                .build();

        EligibilityRuleDb updatedDb = DomainBuilderDatabase.getEligibilityRuleDb(100L, "CRITERION", "OR", extid);
        EligibilityRule expectedDomain = DomainBuilderDatabase.getEligibilityRule(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(EligibilityRuleDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        EligibilityRule result = service.update(extid, changes);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<EligibilityRuleDb> captor = ArgumentCaptor.forClass(EligibilityRuleDb.class);
        verify(repository).save(captor.capture());

        EligibilityRuleDb captured = captor.getValue();
        assertEquals("CRITERION", captured.getNodeType());
        assertEquals("OR", captured.getOperator());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(updatedDb);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        EligibilityRule changes = EligibilityRule.builder().nodeType("Doesn't matter").build();
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, changes));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        EligibilityRuleDb existingDb = DomainBuilderDatabase.getEligibilityRuleDb(100L, "GROUP", "AND", extid);
        EligibilityRule changes = EligibilityRule.builder().nodeType("CRITERION").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(EligibilityRuleDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, changes));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        EligibilityRuleDb existingDb = DomainBuilderDatabase.getEligibilityRuleDb(100L, "GROUP", "AND", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(EligibilityRuleDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<EligibilityRuleDb> captor = ArgumentCaptor.forClass(EligibilityRuleDb.class);
        verify(repository).save(captor.capture());

        EligibilityRuleDb captured = captor.getValue();
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
        EligibilityRuleDb db = DomainBuilderDatabase.getEligibilityRuleDb(100L, "GROUP", "AND", extid);
        EligibilityRule expectedDomain = DomainBuilderDatabase.getEligibilityRule(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        EligibilityRule result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllEntities() {
        // Arrange
        EligibilityRuleDb db1 = DomainBuilderDatabase.getEligibilityRuleDb();
        EligibilityRuleDb db2 = DomainBuilderDatabase.getEligibilityRuleDb();
        List<EligibilityRuleDb> dbList = Arrays.asList(db1, db2);

        EligibilityRule domain1 = DomainBuilderDatabase.getEligibilityRule(db1);
        EligibilityRule domain2 = DomainBuilderDatabase.getEligibilityRule(db2);
        List<EligibilityRule> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<EligibilityRule> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        // Arrange
        EligibilityRuleDb db1 = DomainBuilderDatabase.getEligibilityRuleDb();
        db1.setActive(ActiveEnum.ACTIVE);
        EligibilityRuleDb db2 = DomainBuilderDatabase.getEligibilityRuleDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<EligibilityRuleDb> dbList = Arrays.asList(db1, db2);

        EligibilityRule domain1 = DomainBuilderDatabase.getEligibilityRule(db1);
        EligibilityRule domain2 = DomainBuilderDatabase.getEligibilityRule(db2);
        List<EligibilityRule> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(any(ActiveEnum.class))).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<EligibilityRule> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(any(ActiveEnum.class));
        verify(mapper).toModelList(dbList);
    }
}
