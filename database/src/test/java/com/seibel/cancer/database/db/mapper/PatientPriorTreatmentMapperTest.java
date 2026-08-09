package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.database.db.entity.PatientPriorTreatmentDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientPriorTreatmentMapperTest {

    private PatientPriorTreatmentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PatientPriorTreatmentMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        PatientPriorTreatmentDb db = DomainBuilderDatabase.getPatientPriorTreatmentDb();

        PatientPriorTreatment model = mapper.toModel(db);

        assertEquals(db.getExtid(), model.getExtid());
        assertEquals(db.getAppUserId(), model.getAppUserId());
        assertEquals(db.getPatientDiagnosisId(), model.getPatientDiagnosisId());
        assertEquals(db.getCdk46Status(), model.getCdk46Status());
        assertEquals(db.getEndocrineStatus(), model.getEndocrineStatus());
        assertEquals(db.getSerdStatus(), model.getSerdStatus());
        assertEquals(db.getChemoStatus(), model.getChemoStatus());
        assertEquals(db.getHer2TherapyStatus(), model.getHer2TherapyStatus());
        assertEquals(db.getHer2AdcStatus(), model.getHer2AdcStatus());
        assertEquals(db.getTrop2AdcStatus(), model.getTrop2AdcStatus());
        assertEquals(db.getParpStatus(), model.getParpStatus());
        assertEquals(db.getPi3kAktMtorStatus(), model.getPi3kAktMtorStatus());
        assertEquals(db.getImmunotherapyStatus(), model.getImmunotherapyStatus());
        assertEquals(db.getTaxaneStatus(), model.getTaxaneStatus());
        assertEquals(db.getAnthracyclineStatus(), model.getAnthracyclineStatus());
        assertEquals(db.getPlatinumStatus(), model.getPlatinumStatus());
        assertEquals(db.getCurrentDrugNames(), model.getCurrentDrugNames());
        assertEquals(db.getPriorDrugNames(), model.getPriorDrugNames());
        assertEquals(db.getLinesOfTherapyMetastatic(), model.getLinesOfTherapyMetastatic());
        assertEquals(db.getHadNeoadjuvant(), model.getHadNeoadjuvant());
        assertEquals(db.getHadAdjuvant(), model.getHadAdjuvant());
        assertEquals(db.getHadRadiation(), model.getHadRadiation());
        assertEquals(db.getHadSurgery(), model.getHadSurgery());
        assertEquals(db.getLastTreatmentEndDate(), model.getLastTreatmentEndDate());
        assertEquals(db.getCurrentlyOnTreatment(), model.getCurrentlyOnTreatment());
        assertEquals(db.getOtherTreatments(), model.getOtherTreatments());
        assertEquals(db.getNotes(), model.getNotes());
        assertEquals(db.getCreatedAt(), model.getCreatedAt());
        assertEquals(db.getUpdatedAt(), model.getUpdatedAt());
        assertEquals(db.getDeletedAt(), model.getDeletedAt());
        assertEquals(db.getActive(), model.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        PatientPriorTreatment model = DomainBuilderDatabase.getPatientPriorTreatment();

        PatientPriorTreatmentDb db = mapper.toDb(model);

        assertEquals(model.getExtid(), db.getExtid());
        assertEquals(model.getAppUserId(), db.getAppUserId());
        assertEquals(model.getPatientDiagnosisId(), db.getPatientDiagnosisId());
        assertEquals(model.getCdk46Status(), db.getCdk46Status());
        assertEquals(model.getEndocrineStatus(), db.getEndocrineStatus());
        assertEquals(model.getSerdStatus(), db.getSerdStatus());
        assertEquals(model.getChemoStatus(), db.getChemoStatus());
        assertEquals(model.getHer2TherapyStatus(), db.getHer2TherapyStatus());
        assertEquals(model.getHer2AdcStatus(), db.getHer2AdcStatus());
        assertEquals(model.getTrop2AdcStatus(), db.getTrop2AdcStatus());
        assertEquals(model.getParpStatus(), db.getParpStatus());
        assertEquals(model.getPi3kAktMtorStatus(), db.getPi3kAktMtorStatus());
        assertEquals(model.getImmunotherapyStatus(), db.getImmunotherapyStatus());
        assertEquals(model.getTaxaneStatus(), db.getTaxaneStatus());
        assertEquals(model.getAnthracyclineStatus(), db.getAnthracyclineStatus());
        assertEquals(model.getPlatinumStatus(), db.getPlatinumStatus());
        assertEquals(model.getCurrentDrugNames(), db.getCurrentDrugNames());
        assertEquals(model.getPriorDrugNames(), db.getPriorDrugNames());
        assertEquals(model.getLinesOfTherapyMetastatic(), db.getLinesOfTherapyMetastatic());
        assertEquals(model.getHadNeoadjuvant(), db.getHadNeoadjuvant());
        assertEquals(model.getHadAdjuvant(), db.getHadAdjuvant());
        assertEquals(model.getHadRadiation(), db.getHadRadiation());
        assertEquals(model.getHadSurgery(), db.getHadSurgery());
        assertEquals(model.getLastTreatmentEndDate(), db.getLastTreatmentEndDate());
        assertEquals(model.getCurrentlyOnTreatment(), db.getCurrentlyOnTreatment());
        assertEquals(model.getOtherTreatments(), db.getOtherTreatments());
        assertEquals(model.getNotes(), db.getNotes());
        assertEquals(model.getCreatedAt(), db.getCreatedAt());
        assertEquals(model.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(model.getDeletedAt(), db.getDeletedAt());
        assertEquals(model.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        List<PatientPriorTreatmentDb> dbs = List.of(
                DomainBuilderDatabase.getPatientPriorTreatmentDb(),
                DomainBuilderDatabase.getPatientPriorTreatmentDb());

        List<PatientPriorTreatment> models = mapper.toModelList(dbs);

        assertEquals(2, models.size());
        assertEquals(dbs.get(0).getExtid(), models.get(0).getExtid());
        assertEquals(dbs.get(1).getExtid(), models.get(1).getExtid());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        List<PatientPriorTreatment> models = List.of(
                DomainBuilderDatabase.getPatientPriorTreatment(),
                DomainBuilderDatabase.getPatientPriorTreatment());

        List<PatientPriorTreatmentDb> dbs = mapper.toDbList(models);

        assertEquals(2, dbs.size());
        assertEquals(models.get(0).getExtid(), dbs.get(0).getExtid());
        assertEquals(models.get(1).getExtid(), dbs.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        List<PatientPriorTreatment> models = mapper.toModelList(List.of());

        assertNotNull(models);
        assertTrue(models.isEmpty());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        List<PatientPriorTreatmentDb> dbs = mapper.toDbList(List.of());

        assertNotNull(dbs);
        assertTrue(dbs.isEmpty());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        List<PatientPriorTreatment> models = mapper.toModelList(null);

        assertNotNull(models);
        assertEquals(0, models.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        List<PatientPriorTreatmentDb> dbs = mapper.toDbList(null);

        assertNotNull(dbs);
        assertEquals(0, dbs.size());
    }
}
