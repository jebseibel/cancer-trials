package com.seibel.jobhunting.database.db.mapper;

import com.seibel.jobhunting.common.domain.Friend;
import com.seibel.jobhunting.database.db.entity.FriendDb;
import com.seibel.jobhunting.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FriendMapperTest {

    private FriendMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FriendMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        FriendDb db = DomainBuilderDatabase.getFriendDb();

        // Act
        Friend domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getName(), domain.getName());
        assertEquals(db.getRelationship(), domain.getRelationship());
        assertEquals(db.getEmail(), domain.getEmail());
        assertEquals(db.getPhone(), domain.getPhone());
        assertEquals(db.getLinkedinUrl(), domain.getLinkedinUrl());
        assertEquals(db.getLastContactedAt(), domain.getLastContactedAt());
        assertEquals(db.getNotes(), domain.getNotes());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Friend domain = DomainBuilderDatabase.getFriend();

        // Act
        FriendDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getName(), db.getName());
        assertEquals(domain.getRelationship(), db.getRelationship());
        assertEquals(domain.getEmail(), db.getEmail());
        assertEquals(domain.getPhone(), db.getPhone());
        assertEquals(domain.getLinkedinUrl(), db.getLinkedinUrl());
        assertEquals(domain.getLastContactedAt(), db.getLastContactedAt());
        assertEquals(domain.getNotes(), db.getNotes());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        FriendDb db1 = DomainBuilderDatabase.getFriendDb();
        FriendDb db2 = DomainBuilderDatabase.getFriendDb();
        List<FriendDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Friend> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<FriendDb> dbList = Arrays.asList();

        // Act
        List<Friend> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Friend> domainList = mapper.toModelList(null);

        // Assert
        assertNull(domainList);
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Friend domain1 = DomainBuilderDatabase.getFriend();
        Friend domain2 = DomainBuilderDatabase.getFriend();
        List<Friend> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<FriendDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Friend> domainList = Arrays.asList();

        // Act
        List<FriendDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<FriendDb> dbList = mapper.toDbList(null);

        // Assert
        assertNull(dbList);
    }
}
