package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.UserPatient;
import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.database.db.entity.UserPatientDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPatientMapperTest {

    private UserPatientMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserPatientMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        UserPatientDb source = DomainBuilderDatabase.getUserPatientDb();

        UserPatient result = mapper.toModel(source);

        assertNotNull(result);
        assertEquals(source.getExtid(), result.getExtid());
        assertEquals(source.getUserId(), result.getUserId());
        assertEquals(source.getPatientId(), result.getPatientId());
        assertEquals(source.getAccessLevel(), result.getAccessLevel());
        assertEquals(source.getGrantedByUserId(), result.getGrantedByUserId());
        assertEquals(source.getGrantedAt(), result.getGrantedAt());
        assertEquals(source.getRevokedAt(), result.getRevokedAt());
        assertEquals(source.getNote(), result.getNote());
        assertEquals(source.getCreatedAt(), result.getCreatedAt());
        assertEquals(source.getUpdatedAt(), result.getUpdatedAt());
        assertEquals(source.getActive(), result.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        UserPatient source = DomainBuilderDatabase.getUserPatient();

        UserPatientDb result = mapper.toDb(source);

        assertNotNull(result);
        assertEquals(source.getExtid(), result.getExtid());
        assertEquals(source.getUserId(), result.getUserId());
        assertEquals(source.getPatientId(), result.getPatientId());
        assertEquals(source.getAccessLevel(), result.getAccessLevel());
        assertEquals(source.getGrantedByUserId(), result.getGrantedByUserId());
        assertEquals(source.getGrantedAt(), result.getGrantedAt());
        assertEquals(source.getRevokedAt(), result.getRevokedAt());
        assertEquals(source.getNote(), result.getNote());
    }

    /** revokedAt is what decides whether a grant is in force, so a null must survive as null. */
    @Test
    void toModel_shouldPreserveNullRevokedAt() {
        UserPatientDb source = DomainBuilderDatabase.getUserPatientDb();
        source.setRevokedAt(null);

        UserPatient result = mapper.toModel(source);

        assertNull(result.getRevokedAt());
        assertTrue(result.isActiveGrant());
    }

    @Test
    void toModel_shouldPreserveSetRevokedAt() {
        UserPatientDb source = DomainBuilderDatabase.getUserPatientDb();
        LocalDateTime revoked = LocalDateTime.now();
        source.setRevokedAt(revoked);

        UserPatient result = mapper.toModel(source);

        assertEquals(revoked, result.getRevokedAt());
        assertFalse(result.isActiveGrant());
    }

    /** An access level that changed in transit would hand out the wrong access. */
    @Test
    void toModel_shouldPreserveEveryAccessLevel() {
        for (AccessLevel level : AccessLevel.values()) {
            UserPatientDb source = DomainBuilderDatabase.getUserPatientDb(1L, 2L, level, null);

            assertEquals(level, mapper.toModel(source).getAccessLevel());
        }
    }

    @Test
    void toModelList_shouldMapAllItems() {
        List<UserPatientDb> source = List.of(
                DomainBuilderDatabase.getUserPatientDb(),
                DomainBuilderDatabase.getUserPatientDb());

        List<UserPatient> result = mapper.toModelList(source);

        assertEquals(2, result.size());
        assertEquals(source.get(0).getUserId(), result.get(0).getUserId());
        assertEquals(source.get(1).getUserId(), result.get(1).getUserId());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        List<UserPatient> source = List.of(
                DomainBuilderDatabase.getUserPatient(),
                DomainBuilderDatabase.getUserPatient());

        List<UserPatientDb> result = mapper.toDbList(source);

        assertEquals(2, result.size());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        List<UserPatient> result = mapper.toModelList(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        List<UserPatientDb> result = mapper.toDbList(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        List<UserPatient> result = mapper.toModelList(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        List<UserPatientDb> result = mapper.toDbList(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
