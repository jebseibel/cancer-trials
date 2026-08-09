package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.SavedTrialMatch;
import com.seibel.cancer.database.db.entity.SavedTrialMatchDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrialMatchMapperTest {

    private SavedTrialMatchMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SavedTrialMatchMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        SavedTrialMatchDb db = DomainBuilderDatabase.getSavedTrialMatchDb();

        SavedTrialMatch model = mapper.toModel(db);

        assertEquals(db.getExtid(), model.getExtid());
        assertEquals(db.getTrialId(), model.getTrialId());
        assertEquals(db.getAppUserId(), model.getAppUserId());
        assertEquals(db.getPatientDiagnosisId(), model.getPatientDiagnosisId());
        assertEquals(db.getSearchRunId(), model.getSearchRunId());
        assertEquals(db.getQueryText(), model.getQueryText());
        assertEquals(db.getTopScore(), model.getTopScore());
        assertEquals(db.getMatchRank(), model.getMatchRank());
        assertEquals(db.getSnapshotErStatus(), model.getSnapshotErStatus());
        assertEquals(db.getSnapshotPrStatus(), model.getSnapshotPrStatus());
        assertEquals(db.getSnapshotHer2Status(), model.getSnapshotHer2Status());
        assertEquals(db.getSnapshotStage(), model.getSnapshotStage());
        assertEquals(db.getSnapshotBiomarkers(), model.getSnapshotBiomarkers());
        assertEquals(db.getMatchedAt(), model.getMatchedAt());
        assertEquals(db.getCreatedAt(), model.getCreatedAt());
        assertEquals(db.getUpdatedAt(), model.getUpdatedAt());
        assertEquals(db.getDeletedAt(), model.getDeletedAt());
        assertEquals(db.getActive(), model.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        SavedTrialMatch model = DomainBuilderDatabase.getSavedTrialMatch();

        SavedTrialMatchDb db = mapper.toDb(model);

        assertEquals(model.getExtid(), db.getExtid());
        assertEquals(model.getTrialId(), db.getTrialId());
        assertEquals(model.getAppUserId(), db.getAppUserId());
        assertEquals(model.getPatientDiagnosisId(), db.getPatientDiagnosisId());
        assertEquals(model.getSearchRunId(), db.getSearchRunId());
        assertEquals(model.getQueryText(), db.getQueryText());
        assertEquals(model.getTopScore(), db.getTopScore());
        assertEquals(model.getMatchRank(), db.getMatchRank());
        assertEquals(model.getSnapshotErStatus(), db.getSnapshotErStatus());
        assertEquals(model.getSnapshotPrStatus(), db.getSnapshotPrStatus());
        assertEquals(model.getSnapshotHer2Status(), db.getSnapshotHer2Status());
        assertEquals(model.getSnapshotStage(), db.getSnapshotStage());
        assertEquals(model.getSnapshotBiomarkers(), db.getSnapshotBiomarkers());
        assertEquals(model.getMatchedAt(), db.getMatchedAt());
        assertEquals(model.getCreatedAt(), db.getCreatedAt());
        assertEquals(model.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(model.getDeletedAt(), db.getDeletedAt());
        assertEquals(model.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        List<SavedTrialMatchDb> items = List.of(
                DomainBuilderDatabase.getSavedTrialMatchDb(),
                DomainBuilderDatabase.getSavedTrialMatchDb());

        List<SavedTrialMatch> result = mapper.toModelList(items);

        assertEquals(2, result.size());
        assertEquals(items.get(0).getExtid(), result.get(0).getExtid());
        assertEquals(items.get(1).getExtid(), result.get(1).getExtid());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        List<SavedTrialMatch> items = List.of(
                DomainBuilderDatabase.getSavedTrialMatch(),
                DomainBuilderDatabase.getSavedTrialMatch());

        List<SavedTrialMatchDb> result = mapper.toDbList(items);

        assertEquals(2, result.size());
        assertEquals(items.get(0).getExtid(), result.get(0).getExtid());
        assertEquals(items.get(1).getExtid(), result.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        List<SavedTrialMatch> result = mapper.toModelList(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        List<SavedTrialMatchDb> result = mapper.toDbList(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        List<SavedTrialMatch> result = mapper.toModelList(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        List<SavedTrialMatchDb> result = mapper.toDbList(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
