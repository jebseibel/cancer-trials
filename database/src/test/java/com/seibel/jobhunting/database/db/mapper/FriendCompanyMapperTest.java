package com.seibel.jobhunting.database.db.mapper;

import com.seibel.jobhunting.common.domain.FriendCompany;
import com.seibel.jobhunting.database.db.entity.FriendCompanyDb;
import com.seibel.jobhunting.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FriendCompanyMapperTest {

    private FriendCompanyMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FriendCompanyMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        FriendCompanyDb db = DomainBuilderDatabase.getFriendCompanyDb();

        // Act
        FriendCompany domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getFriendId(), domain.getFriendId());
        assertEquals(db.getCompanyId(), domain.getCompanyId());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        FriendCompany domain = DomainBuilderDatabase.getFriendCompany();

        // Act
        FriendCompanyDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getFriendId(), db.getFriendId());
        assertEquals(domain.getCompanyId(), db.getCompanyId());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        FriendCompanyDb db1 = DomainBuilderDatabase.getFriendCompanyDb();
        FriendCompanyDb db2 = DomainBuilderDatabase.getFriendCompanyDb();
        List<FriendCompanyDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<FriendCompany> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<FriendCompanyDb> dbList = Arrays.asList();

        // Act
        List<FriendCompany> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<FriendCompany> domainList = mapper.toModelList(null);

        // Assert
        assertNull(domainList);
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        FriendCompany domain1 = DomainBuilderDatabase.getFriendCompany();
        FriendCompany domain2 = DomainBuilderDatabase.getFriendCompany();
        List<FriendCompany> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<FriendCompanyDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<FriendCompany> domainList = Arrays.asList();

        // Act
        List<FriendCompanyDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<FriendCompanyDb> dbList = mapper.toDbList(null);

        // Assert
        assertNull(dbList);
    }
}
