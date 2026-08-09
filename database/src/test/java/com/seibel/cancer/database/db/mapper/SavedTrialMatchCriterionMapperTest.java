package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.SavedTrialMatchCriterion;
import com.seibel.cancer.database.db.entity.SavedTrialMatchCriterionDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavedTrialMatchCriterionMapperTest {

    private SavedTrialMatchCriterionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SavedTrialMatchCriterionMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        SavedTrialMatchCriterionDb db = DomainBuilderDatabase.getSavedTrialMatchCriterionDb();

        SavedTrialMatchCriterion model = mapper.toModel(db);

        assertEquals(db.getExtid(), model.getExtid());
        assertEquals(db.getTrialMatchId(), model.getTrialMatchId());
        assertEquals(db.getChunkText(), model.getChunkText());
        assertEquals(db.getScore(), model.getScore());
        assertEquals(db.getIsExclusion(), model.getIsExclusion());
        assertEquals(db.getSource(), model.getSource());
        assertEquals(db.getOrdinal(), model.getOrdinal());
        assertEquals(db.getCreatedAt(), model.getCreatedAt());
        assertEquals(db.getUpdatedAt(), model.getUpdatedAt());
        assertEquals(db.getDeletedAt(), model.getDeletedAt());
        assertEquals(db.getActive(), model.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        SavedTrialMatchCriterion model = DomainBuilderDatabase.getSavedTrialMatchCriterion();

        SavedTrialMatchCriterionDb db = mapper.toDb(model);

        assertEquals(model.getExtid(), db.getExtid());
        assertEquals(model.getTrialMatchId(), db.getTrialMatchId());
        assertEquals(model.getChunkText(), db.getChunkText());
        assertEquals(model.getScore(), db.getScore());
        assertEquals(model.getIsExclusion(), db.getIsExclusion());
        assertEquals(model.getSource(), db.getSource());
        assertEquals(model.getOrdinal(), db.getOrdinal());
        assertEquals(model.getCreatedAt(), db.getCreatedAt());
        assertEquals(model.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(model.getDeletedAt(), db.getDeletedAt());
        assertEquals(model.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        List<SavedTrialMatchCriterionDb> items = List.of(
                DomainBuilderDatabase.getSavedTrialMatchCriterionDb(),
                DomainBuilderDatabase.getSavedTrialMatchCriterionDb());

        List<SavedTrialMatchCriterion> result = mapper.toModelList(items);

        assertEquals(2, result.size());
        assertEquals(items.get(0).getExtid(), result.get(0).getExtid());
        assertEquals(items.get(1).getExtid(), result.get(1).getExtid());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        List<SavedTrialMatchCriterion> items = List.of(
                DomainBuilderDatabase.getSavedTrialMatchCriterion(),
                DomainBuilderDatabase.getSavedTrialMatchCriterion());

        List<SavedTrialMatchCriterionDb> result = mapper.toDbList(items);

        assertEquals(2, result.size());
        assertEquals(items.get(0).getExtid(), result.get(0).getExtid());
        assertEquals(items.get(1).getExtid(), result.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        List<SavedTrialMatchCriterion> result = mapper.toModelList(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        List<SavedTrialMatchCriterionDb> result = mapper.toDbList(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        List<SavedTrialMatchCriterion> result = mapper.toModelList(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        List<SavedTrialMatchCriterionDb> result = mapper.toDbList(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
