package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.database.db.entity.PatientVariantDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientVariantMapperTest {

    private PatientVariantMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PatientVariantMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        PatientVariantDb db = DomainBuilderDatabase.getPatientVariantDb();

        PatientVariant model = mapper.toModel(db);

        assertEquals(db.getExtid(), model.getExtid());
        assertEquals(db.getPatientId(), model.getPatientId());
        assertEquals(db.getPatientDiagnosisId(), model.getPatientDiagnosisId());
        assertEquals(db.getPik3caStatus(), model.getPik3caStatus());
        assertEquals(db.getEsr1Status(), model.getEsr1Status());
        assertEquals(db.getTp53Status(), model.getTp53Status());
        assertEquals(db.getAkt1Status(), model.getAkt1Status());
        assertEquals(db.getPtenStatus(), model.getPtenStatus());
        assertEquals(db.getErbb2SomaticStatus(), model.getErbb2SomaticStatus());
        assertEquals(db.getBrca1Status(), model.getBrca1Status());
        assertEquals(db.getBrca2Status(), model.getBrca2Status());
        assertEquals(db.getPalb2Status(), model.getPalb2Status());
        assertEquals(db.getAtmStatus(), model.getAtmStatus());
        assertEquals(db.getChek2Status(), model.getChek2Status());
        assertEquals(db.getHrdStatus(), model.getHrdStatus());
        assertEquals(db.getPdl1Status(), model.getPdl1Status());
        assertEquals(db.getKi67Percent(), model.getKi67Percent());
        assertEquals(db.getGermlineTestDone(), model.getGermlineTestDone());
        assertEquals(db.getSomaticTestDone(), model.getSomaticTestDone());
        assertEquals(db.getTestDate(), model.getTestDate());
        assertEquals(db.getTestLab(), model.getTestLab());
        assertEquals(db.getOtherVariants(), model.getOtherVariants());
        assertEquals(db.getNotes(), model.getNotes());
        assertEquals(db.getCreatedAt(), model.getCreatedAt());
        assertEquals(db.getUpdatedAt(), model.getUpdatedAt());
        assertEquals(db.getDeletedAt(), model.getDeletedAt());
        assertEquals(db.getActive(), model.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        PatientVariant model = DomainBuilderDatabase.getPatientVariant();

        PatientVariantDb db = mapper.toDb(model);

        assertEquals(model.getExtid(), db.getExtid());
        assertEquals(model.getPatientId(), db.getPatientId());
        assertEquals(model.getPatientDiagnosisId(), db.getPatientDiagnosisId());
        assertEquals(model.getPik3caStatus(), db.getPik3caStatus());
        assertEquals(model.getEsr1Status(), db.getEsr1Status());
        assertEquals(model.getTp53Status(), db.getTp53Status());
        assertEquals(model.getAkt1Status(), db.getAkt1Status());
        assertEquals(model.getPtenStatus(), db.getPtenStatus());
        assertEquals(model.getErbb2SomaticStatus(), db.getErbb2SomaticStatus());
        assertEquals(model.getBrca1Status(), db.getBrca1Status());
        assertEquals(model.getBrca2Status(), db.getBrca2Status());
        assertEquals(model.getPalb2Status(), db.getPalb2Status());
        assertEquals(model.getAtmStatus(), db.getAtmStatus());
        assertEquals(model.getChek2Status(), db.getChek2Status());
        assertEquals(model.getHrdStatus(), db.getHrdStatus());
        assertEquals(model.getPdl1Status(), db.getPdl1Status());
        assertEquals(model.getKi67Percent(), db.getKi67Percent());
        assertEquals(model.getGermlineTestDone(), db.getGermlineTestDone());
        assertEquals(model.getSomaticTestDone(), db.getSomaticTestDone());
        assertEquals(model.getTestDate(), db.getTestDate());
        assertEquals(model.getTestLab(), db.getTestLab());
        assertEquals(model.getOtherVariants(), db.getOtherVariants());
        assertEquals(model.getNotes(), db.getNotes());
        assertEquals(model.getCreatedAt(), db.getCreatedAt());
        assertEquals(model.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(model.getDeletedAt(), db.getDeletedAt());
        assertEquals(model.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        List<PatientVariantDb> dbs = List.of(
                DomainBuilderDatabase.getPatientVariantDb(),
                DomainBuilderDatabase.getPatientVariantDb());

        List<PatientVariant> models = mapper.toModelList(dbs);

        assertEquals(2, models.size());
        assertEquals(dbs.get(0).getExtid(), models.get(0).getExtid());
        assertEquals(dbs.get(1).getExtid(), models.get(1).getExtid());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        List<PatientVariant> models = List.of(
                DomainBuilderDatabase.getPatientVariant(),
                DomainBuilderDatabase.getPatientVariant());

        List<PatientVariantDb> dbs = mapper.toDbList(models);

        assertEquals(2, dbs.size());
        assertEquals(models.get(0).getExtid(), dbs.get(0).getExtid());
        assertEquals(models.get(1).getExtid(), dbs.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        List<PatientVariant> models = mapper.toModelList(List.of());

        assertNotNull(models);
        assertTrue(models.isEmpty());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        List<PatientVariantDb> dbs = mapper.toDbList(List.of());

        assertNotNull(dbs);
        assertTrue(dbs.isEmpty());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        List<PatientVariant> models = mapper.toModelList(null);

        assertNotNull(models);
        assertEquals(0, models.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        List<PatientVariantDb> dbs = mapper.toDbList(null);

        assertNotNull(dbs);
        assertEquals(0, dbs.size());
    }
}
