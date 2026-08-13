package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.database.db.entity.PatientDiagnosisDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientDiagnosisMapperTest {

    private PatientDiagnosisMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PatientDiagnosisMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        PatientDiagnosisDb db = DomainBuilderDatabase.getPatientDiagnosisDb();

        PatientDiagnosis model = mapper.toModel(db);

        assertNotNull(model);
        assertEquals(db.getExtid(), model.getExtid());
        assertEquals(db.getPatientId(), model.getPatientId());
        assertEquals(db.getCancerType(), model.getCancerType());
        assertEquals(db.getStage(), model.getStage());
        assertEquals(db.getStageSystem(), model.getStageSystem());
        assertEquals(db.getIsMetastatic(), model.getIsMetastatic());
        assertEquals(db.getMetastasisSites(), model.getMetastasisSites());
        assertEquals(db.getReceptorSubtype(), model.getReceptorSubtype());
        assertEquals(db.getErStatus(), model.getErStatus());
        assertEquals(db.getPrStatus(), model.getPrStatus());
        assertEquals(db.getHer2Status(), model.getHer2Status());
        assertEquals(db.getBiomarkers(), model.getBiomarkers());
        assertEquals(db.getEcogStatus(), model.getEcogStatus());
        assertEquals(db.getPriorChemoRegimens(), model.getPriorChemoRegimens());
        assertEquals(db.getLastChemoEndDate(), model.getLastChemoEndDate());
        assertEquals(db.getPriorTreatments(), model.getPriorTreatments());
        assertEquals(db.getHasMeasurableDisease(), model.getHasMeasurableDisease());
        assertEquals(db.getMenopausalStatus(), model.getMenopausalStatus());
        assertEquals(db.getDiagnosisDate(), model.getDiagnosisDate());
        assertEquals(db.getNotes(), model.getNotes());
        assertEquals(db.getCreatedAt(), model.getCreatedAt());
        assertEquals(db.getUpdatedAt(), model.getUpdatedAt());
        assertEquals(db.getDeletedAt(), model.getDeletedAt());
        assertEquals(db.getActive(), model.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        PatientDiagnosis model = DomainBuilderDatabase.getPatientDiagnosis();

        PatientDiagnosisDb db = mapper.toDb(model);

        assertNotNull(db);
        assertEquals(model.getExtid(), db.getExtid());
        assertEquals(model.getPatientId(), db.getPatientId());
        assertEquals(model.getCancerType(), db.getCancerType());
        assertEquals(model.getStage(), db.getStage());
        assertEquals(model.getStageSystem(), db.getStageSystem());
        assertEquals(model.getIsMetastatic(), db.getIsMetastatic());
        assertEquals(model.getMetastasisSites(), db.getMetastasisSites());
        assertEquals(model.getReceptorSubtype(), db.getReceptorSubtype());
        assertEquals(model.getErStatus(), db.getErStatus());
        assertEquals(model.getPrStatus(), db.getPrStatus());
        assertEquals(model.getHer2Status(), db.getHer2Status());
        assertEquals(model.getBiomarkers(), db.getBiomarkers());
        assertEquals(model.getEcogStatus(), db.getEcogStatus());
        assertEquals(model.getPriorChemoRegimens(), db.getPriorChemoRegimens());
        assertEquals(model.getLastChemoEndDate(), db.getLastChemoEndDate());
        assertEquals(model.getPriorTreatments(), db.getPriorTreatments());
        assertEquals(model.getHasMeasurableDisease(), db.getHasMeasurableDisease());
        assertEquals(model.getMenopausalStatus(), db.getMenopausalStatus());
        assertEquals(model.getDiagnosisDate(), db.getDiagnosisDate());
        assertEquals(model.getNotes(), db.getNotes());
        assertEquals(model.getCreatedAt(), db.getCreatedAt());
        assertEquals(model.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(model.getDeletedAt(), db.getDeletedAt());
        assertEquals(model.getActive(), db.getActive());
    }

    @Test
    void toModel_shouldPreserveNullOptionalFields() {
        // Most of the staging fields are optional - the mapper must not invent values.
        PatientDiagnosisDb db = DomainBuilderDatabase.getPatientDiagnosisDb();
        db.setStage(null);
        db.setEcogStatus(null);
        db.setLastChemoEndDate(null);

        PatientDiagnosis model = mapper.toModel(db);

        assertNull(model.getStage());
        assertNull(model.getEcogStatus());
        assertNull(model.getLastChemoEndDate());
        assertNotNull(model.getCancerType());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        List<PatientDiagnosisDb> dbList = List.of(
                DomainBuilderDatabase.getPatientDiagnosisDb(),
                DomainBuilderDatabase.getPatientDiagnosisDb());

        List<PatientDiagnosis> models = mapper.toModelList(dbList);

        assertEquals(2, models.size());
        assertEquals(dbList.get(0).getCancerType(), models.get(0).getCancerType());
        assertEquals(dbList.get(1).getCancerType(), models.get(1).getCancerType());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        List<PatientDiagnosis> models = List.of(
                DomainBuilderDatabase.getPatientDiagnosis(),
                DomainBuilderDatabase.getPatientDiagnosis());

        List<PatientDiagnosisDb> dbList = mapper.toDbList(models);

        assertEquals(2, dbList.size());
        assertEquals(models.get(0).getCancerType(), dbList.get(0).getCancerType());
        assertEquals(models.get(1).getCancerType(), dbList.get(1).getCancerType());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        List<PatientDiagnosis> models = mapper.toModelList(List.of());

        assertNotNull(models);
        assertTrue(models.isEmpty());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        List<PatientDiagnosisDb> dbList = mapper.toDbList(List.of());

        assertNotNull(dbList);
        assertTrue(dbList.isEmpty());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        List<PatientDiagnosis> models = mapper.toModelList(null);

        // Must be an empty list, never null - a mapper returning null here is a bug.
        assertNotNull(models);
        assertEquals(0, models.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        List<PatientDiagnosisDb> dbList = mapper.toDbList(null);

        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
