package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.PatientMedication;
import com.seibel.cancer.database.db.entity.PatientMedicationDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientMedicationMapperTest {

    private PatientMedicationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PatientMedicationMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        PatientMedicationDb db = DomainBuilderDatabase.getPatientMedicationDb();

        PatientMedication model = mapper.toModel(db);

        assertNotNull(model);
        assertEquals(db.getExtid(), model.getExtid());
        assertEquals(db.getFhirResourceId(), model.getFhirResourceId());
        assertEquals(db.getMedicationName(), model.getMedicationName());
        assertEquals(db.getRxnormCode(), model.getRxnormCode());
        assertEquals(db.getStatus(), model.getStatus());
        assertEquals(db.getIntent(), model.getIntent());
        assertEquals(db.getAuthoredOn(), model.getAuthoredOn());
        assertEquals(db.getDosageText(), model.getDosageText());
        assertEquals(db.getDoseQuantity(), model.getDoseQuantity());
        assertEquals(db.getDoseUnit(), model.getDoseUnit());
        assertEquals(db.getRoute(), model.getRoute());
        assertEquals(db.getFrequencyText(), model.getFrequencyText());
        assertEquals(db.getPrescriberName(), model.getPrescriberName());
        assertEquals(db.getReasonText(), model.getReasonText());
        assertEquals(db.getValidityStart(), model.getValidityStart());
        assertEquals(db.getValidityEnd(), model.getValidityEnd());
        assertEquals(db.getRefillsAllowed(), model.getRefillsAllowed());
        assertEquals(db.getDisplayText(), model.getDisplayText());
        assertEquals(db.getCreatedAt(), model.getCreatedAt());
        assertEquals(db.getUpdatedAt(), model.getUpdatedAt());
        assertEquals(db.getDeletedAt(), model.getDeletedAt());
        assertEquals(db.getActive(), model.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        PatientMedication model = DomainBuilderDatabase.getPatientMedication();

        PatientMedicationDb db = mapper.toDb(model);

        assertNotNull(db);
        assertEquals(model.getExtid(), db.getExtid());
        assertEquals(model.getFhirResourceId(), db.getFhirResourceId());
        assertEquals(model.getMedicationName(), db.getMedicationName());
        assertEquals(model.getRxnormCode(), db.getRxnormCode());
        assertEquals(model.getStatus(), db.getStatus());
        assertEquals(model.getIntent(), db.getIntent());
        assertEquals(model.getAuthoredOn(), db.getAuthoredOn());
        assertEquals(model.getDosageText(), db.getDosageText());
        assertEquals(model.getDoseQuantity(), db.getDoseQuantity());
        assertEquals(model.getDoseUnit(), db.getDoseUnit());
        assertEquals(model.getRoute(), db.getRoute());
        assertEquals(model.getFrequencyText(), db.getFrequencyText());
        assertEquals(model.getPrescriberName(), db.getPrescriberName());
        assertEquals(model.getReasonText(), db.getReasonText());
        assertEquals(model.getValidityStart(), db.getValidityStart());
        assertEquals(model.getValidityEnd(), db.getValidityEnd());
        assertEquals(model.getRefillsAllowed(), db.getRefillsAllowed());
        assertEquals(model.getDisplayText(), db.getDisplayText());
        assertEquals(model.getCreatedAt(), db.getCreatedAt());
        assertEquals(model.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(model.getDeletedAt(), db.getDeletedAt());
        assertEquals(model.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        List<PatientMedicationDb> dbList = List.of(
                DomainBuilderDatabase.getPatientMedicationDb(),
                DomainBuilderDatabase.getPatientMedicationDb());

        List<PatientMedication> models = mapper.toModelList(dbList);

        assertEquals(2, models.size());
        assertEquals(dbList.get(0).getFhirResourceId(), models.get(0).getFhirResourceId());
        assertEquals(dbList.get(1).getFhirResourceId(), models.get(1).getFhirResourceId());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        List<PatientMedication> models = List.of(
                DomainBuilderDatabase.getPatientMedication(),
                DomainBuilderDatabase.getPatientMedication());

        List<PatientMedicationDb> dbList = mapper.toDbList(models);

        assertEquals(2, dbList.size());
        assertEquals(models.get(0).getFhirResourceId(), dbList.get(0).getFhirResourceId());
        assertEquals(models.get(1).getFhirResourceId(), dbList.get(1).getFhirResourceId());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        List<PatientMedication> models = mapper.toModelList(List.of());

        assertNotNull(models);
        assertTrue(models.isEmpty());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        List<PatientMedicationDb> dbList = mapper.toDbList(List.of());

        assertNotNull(dbList);
        assertTrue(dbList.isEmpty());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        List<PatientMedication> models = mapper.toModelList(null);

        // Must be an empty list, never null - a mapper returning null here is a bug.
        assertNotNull(models);
        assertEquals(0, models.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        List<PatientMedicationDb> dbList = mapper.toDbList(null);

        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
