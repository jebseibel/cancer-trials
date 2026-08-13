package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Patient;
import com.seibel.cancer.database.db.entity.PatientDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientMapperTest {

    private PatientMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PatientMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        PatientDb source = DomainBuilderDatabase.getPatientDb();

        Patient result = mapper.toModel(source);

        assertNotNull(result);
        assertEquals(source.getExtid(), result.getExtid());
        assertEquals(source.getDisplayName(), result.getDisplayName());
        assertEquals(source.getFullName(), result.getFullName());
        assertEquals(source.getDateOfBirth(), result.getDateOfBirth());
        assertEquals(source.getSex(), result.getSex());
        assertEquals(source.getNotes(), result.getNotes());
        assertEquals(source.getCreatedAt(), result.getCreatedAt());
        assertEquals(source.getUpdatedAt(), result.getUpdatedAt());
        assertEquals(source.getDeletedAt(), result.getDeletedAt());
        assertEquals(source.getActive(), result.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        Patient source = DomainBuilderDatabase.getPatient();

        PatientDb result = mapper.toDb(source);

        assertNotNull(result);
        assertEquals(source.getExtid(), result.getExtid());
        assertEquals(source.getDisplayName(), result.getDisplayName());
        assertEquals(source.getFullName(), result.getFullName());
        assertEquals(source.getDateOfBirth(), result.getDateOfBirth());
        assertEquals(source.getSex(), result.getSex());
        assertEquals(source.getNotes(), result.getNotes());
        assertEquals(source.getCreatedAt(), result.getCreatedAt());
        assertEquals(source.getUpdatedAt(), result.getUpdatedAt());
        assertEquals(source.getDeletedAt(), result.getDeletedAt());
        assertEquals(source.getActive(), result.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        List<PatientDb> source = List.of(
                DomainBuilderDatabase.getPatientDb(),
                DomainBuilderDatabase.getPatientDb());

        List<Patient> result = mapper.toModelList(source);

        assertEquals(2, result.size());
        assertEquals(source.get(0).getDisplayName(), result.get(0).getDisplayName());
        assertEquals(source.get(1).getDisplayName(), result.get(1).getDisplayName());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        List<Patient> source = List.of(
                DomainBuilderDatabase.getPatient(),
                DomainBuilderDatabase.getPatient());

        List<PatientDb> result = mapper.toDbList(source);

        assertEquals(2, result.size());
        assertEquals(source.get(0).getDisplayName(), result.get(0).getDisplayName());
        assertEquals(source.get(1).getDisplayName(), result.get(1).getDisplayName());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        List<Patient> result = mapper.toModelList(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        List<PatientDb> result = mapper.toDbList(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        List<Patient> result = mapper.toModelList(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        List<PatientDb> result = mapper.toDbList(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
