package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.LabResult;
import com.seibel.cancer.database.db.entity.LabResultDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabResultMapperTest {

    private LabResultMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LabResultMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        LabResultDb db = DomainBuilderDatabase.getLabResultDb();

        LabResult model = mapper.toModel(db);

        assertNotNull(model);
        assertEquals(db.getExtid(), model.getExtid());
        assertEquals(db.getFhirResourceId(), model.getFhirResourceId());
        assertEquals(db.getTestName(), model.getTestName());
        assertEquals(db.getLoincCode(), model.getLoincCode());
        assertEquals(db.getStatus(), model.getStatus());
        assertEquals(db.getCategory(), model.getCategory());
        assertEquals(db.getEffectiveAt(), model.getEffectiveAt());
        assertEquals(db.getIssuedAt(), model.getIssuedAt());
        assertEquals(db.getValueQuantity(), model.getValueQuantity());
        assertEquals(db.getValueUnit(), model.getValueUnit());
        assertEquals(db.getValueString(), model.getValueString());
        assertEquals(db.getInterpretation(), model.getInterpretation());
        assertEquals(db.getReferenceRangeLow(), model.getReferenceRangeLow());
        assertEquals(db.getReferenceRangeHigh(), model.getReferenceRangeHigh());
        assertEquals(db.getReferenceRangeText(), model.getReferenceRangeText());
        assertEquals(db.getIsPanel(), model.getIsPanel());
        assertEquals(db.getDisplayText(), model.getDisplayText());
        assertEquals(db.getCreatedAt(), model.getCreatedAt());
        assertEquals(db.getUpdatedAt(), model.getUpdatedAt());
        assertEquals(db.getDeletedAt(), model.getDeletedAt());
        assertEquals(db.getActive(), model.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        LabResult model = DomainBuilderDatabase.getLabResult();

        LabResultDb db = mapper.toDb(model);

        assertNotNull(db);
        assertEquals(model.getExtid(), db.getExtid());
        assertEquals(model.getFhirResourceId(), db.getFhirResourceId());
        assertEquals(model.getTestName(), db.getTestName());
        assertEquals(model.getLoincCode(), db.getLoincCode());
        assertEquals(model.getStatus(), db.getStatus());
        assertEquals(model.getCategory(), db.getCategory());
        assertEquals(model.getEffectiveAt(), db.getEffectiveAt());
        assertEquals(model.getIssuedAt(), db.getIssuedAt());
        assertEquals(model.getValueQuantity(), db.getValueQuantity());
        assertEquals(model.getValueUnit(), db.getValueUnit());
        assertEquals(model.getValueString(), db.getValueString());
        assertEquals(model.getInterpretation(), db.getInterpretation());
        assertEquals(model.getReferenceRangeLow(), db.getReferenceRangeLow());
        assertEquals(model.getReferenceRangeHigh(), db.getReferenceRangeHigh());
        assertEquals(model.getReferenceRangeText(), db.getReferenceRangeText());
        assertEquals(model.getIsPanel(), db.getIsPanel());
        assertEquals(model.getDisplayText(), db.getDisplayText());
        assertEquals(model.getCreatedAt(), db.getCreatedAt());
        assertEquals(model.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(model.getDeletedAt(), db.getDeletedAt());
        assertEquals(model.getActive(), db.getActive());
    }

    @Test
    void toModel_shouldPreserveNullValueUnit() {
        // Epic returns valueQuantity with no unit - the mapper must not invent one.
        LabResultDb db = DomainBuilderDatabase.getLabResultDb();
        db.setValueUnit(null);

        assertNull(mapper.toModel(db).getValueUnit());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        List<LabResultDb> dbList = List.of(
                DomainBuilderDatabase.getLabResultDb(),
                DomainBuilderDatabase.getLabResultDb());

        List<LabResult> models = mapper.toModelList(dbList);

        assertEquals(2, models.size());
        assertEquals(dbList.get(0).getFhirResourceId(), models.get(0).getFhirResourceId());
        assertEquals(dbList.get(1).getFhirResourceId(), models.get(1).getFhirResourceId());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        List<LabResult> models = List.of(
                DomainBuilderDatabase.getLabResult(),
                DomainBuilderDatabase.getLabResult());

        List<LabResultDb> dbList = mapper.toDbList(models);

        assertEquals(2, dbList.size());
        assertEquals(models.get(0).getFhirResourceId(), dbList.get(0).getFhirResourceId());
        assertEquals(models.get(1).getFhirResourceId(), dbList.get(1).getFhirResourceId());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        List<LabResult> models = mapper.toModelList(List.of());

        assertNotNull(models);
        assertTrue(models.isEmpty());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        List<LabResultDb> dbList = mapper.toDbList(List.of());

        assertNotNull(dbList);
        assertTrue(dbList.isEmpty());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        List<LabResult> models = mapper.toModelList(null);

        // Must be an empty list, never null - a mapper returning null here is a bug.
        assertNotNull(models);
        assertEquals(0, models.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        List<LabResultDb> dbList = mapper.toDbList(null);

        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
