package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.AppUser;
import com.seibel.cancer.database.db.entity.AppUserDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppUserMapperTest {

    private AppUserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AppUserMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        AppUserDb db = DomainBuilderDatabase.getAppUserDb();

        // Act
        AppUser domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getUsername(), domain.getUsername());
        assertEquals(db.getPasswordHash(), domain.getPasswordHash());
        assertEquals(db.getDisplayName(), domain.getDisplayName());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        AppUser domain = DomainBuilderDatabase.getAppUser();

        // Act
        AppUserDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getUsername(), db.getUsername());
        assertEquals(domain.getPasswordHash(), db.getPasswordHash());
        assertEquals(domain.getDisplayName(), db.getDisplayName());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        AppUserDb db1 = DomainBuilderDatabase.getAppUserDb();
        AppUserDb db2 = DomainBuilderDatabase.getAppUserDb();
        List<AppUserDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<AppUser> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<AppUserDb> dbList = Arrays.asList();

        // Act
        List<AppUser> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<AppUser> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        AppUser domain1 = DomainBuilderDatabase.getAppUser();
        AppUser domain2 = DomainBuilderDatabase.getAppUser();
        List<AppUser> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<AppUserDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<AppUser> domainList = Arrays.asList();

        // Act
        List<AppUserDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<AppUserDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
