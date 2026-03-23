package com.seibel.basic.database.db.mapper;

import com.seibel.basic.common.domain.User;
import com.seibel.basic.database.db.entity.UserDb;
import com.seibel.basic.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserMapperTest {

    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        UserDb db = DomainBuilderDatabase.getUserDb();

        // Act
        User domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getUsername(), domain.getUsername());
        assertEquals(db.getPassword(), domain.getPassword());
        assertEquals(db.getEmail(), domain.getEmail());
        assertEquals(db.getRole(), domain.getRole());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        User domain = DomainBuilderDatabase.getUser();

        // Act
        UserDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getUsername(), db.getUsername());
        assertEquals(domain.getPassword(), db.getPassword());
        assertEquals(domain.getEmail(), db.getEmail());
        assertEquals(domain.getRole(), db.getRole());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        UserDb db1 = DomainBuilderDatabase.getUserDb();
        UserDb db2 = DomainBuilderDatabase.getUserDb();
        List<UserDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<User> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<UserDb> dbList = Arrays.asList();

        // Act
        List<User> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<User> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        User domain1 = DomainBuilderDatabase.getUser();
        User domain2 = DomainBuilderDatabase.getUser();
        List<User> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<UserDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<User> domainList = Arrays.asList();

        // Act
        List<UserDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<UserDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
