package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.LabResultComponent;
import com.seibel.cancer.database.db.entity.LabResultComponentDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabResultComponentMapperTest {

    private LabResultComponentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LabResultComponentMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        LabResultComponentDb db = DomainBuilderDatabase.getLabResultComponentDb();

        LabResultComponent model = mapper.toModel(db);

        assertNotNull(model);
        assertEquals(db.getExtid(), model.getExtid());
        assertEquals(db.getLabResultId(), model.getLabResultId());
        assertEquals(db.getComponentName(), model.getComponentName());
        assertEquals(db.getLoincCode(), model.getLoincCode());
        assertEquals(db.getValueQuantity(), model.getValueQuantity());
        assertEquals(db.getValueUnit(), model.getValueUnit());
        assertEquals(db.getValueString(), model.getValueString());
        assertEquals(db.getInterpretation(), model.getInterpretation());
        assertEquals(db.getReferenceRangeLow(), model.getReferenceRangeLow());
        assertEquals(db.getReferenceRangeHigh(), model.getReferenceRangeHigh());
        assertEquals(db.getReferenceRangeText(), model.getReferenceRangeText());
        assertEquals(db.getDisplayText(), model.getDisplayText());
        assertEquals(db.getCreatedAt(), model.getCreatedAt());
        assertEquals(db.getUpdatedAt(), model.getUpdatedAt());
        assertEquals(db.getDeletedAt(), model.getDeletedAt());
        assertEquals(db.getActive(), model.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        LabResultComponent model = DomainBuilderDatabase.getLabResultComponent();

        LabResultComponentDb db = mapper.toDb(model);

        assertNotNull(db);
        assertEquals(model.getExtid(), db.getExtid());
        assertEquals(model.getLabResultId(), db.getLabResultId());
        assertEquals(model.getComponentName(), db.getComponentName());
        assertEquals(model.getLoincCode(), db.getLoincCode());
        assertEquals(model.getValueQuantity(), db.getValueQuantity());
        assertEquals(model.getValueUnit(), db.getValueUnit());
        assertEquals(model.getValueString(), db.getValueString());
        assertEquals(model.getInterpretation(), db.getInterpretation());
        assertEquals(model.getReferenceRangeLow(), db.getReferenceRangeLow());
        assertEquals(model.getReferenceRangeHigh(), db.getReferenceRangeHigh());
        assertEquals(model.getReferenceRangeText(), db.getReferenceRangeText());
        assertEquals(model.getDisplayText(), db.getDisplayText());
        assertEquals(model.getCreatedAt(), db.getCreatedAt());
        assertEquals(model.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(model.getDeletedAt(), db.getDeletedAt());
        assertEquals(model.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        List<LabResultComponentDb> dbList = List.of(
                DomainBuilderDatabase.getLabResultComponentDb(),
                DomainBuilderDatabase.getLabResultComponentDb());

        List<LabResultComponent> models = mapper.toModelList(dbList);

        assertEquals(2, models.size());
        assertEquals(dbList.get(0).getComponentName(), models.get(0).getComponentName());
        assertEquals(dbList.get(1).getComponentName(), models.get(1).getComponentName());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        List<LabResultComponent> models = List.of(
                DomainBuilderDatabase.getLabResultComponent(),
                DomainBuilderDatabase.getLabResultComponent());

        List<LabResultComponentDb> dbList = mapper.toDbList(models);

        assertEquals(2, dbList.size());
        assertEquals(models.get(0).getComponentName(), dbList.get(0).getComponentName());
        assertEquals(models.get(1).getComponentName(), dbList.get(1).getComponentName());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        List<LabResultComponent> models = mapper.toModelList(List.of());

        assertNotNull(models);
        assertTrue(models.isEmpty());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        List<LabResultComponentDb> dbList = mapper.toDbList(List.of());

        assertNotNull(dbList);
        assertTrue(dbList.isEmpty());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        List<LabResultComponent> models = mapper.toModelList(null);

        assertNotNull(models);
        assertEquals(0, models.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        List<LabResultComponentDb> dbList = mapper.toDbList(null);

        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
