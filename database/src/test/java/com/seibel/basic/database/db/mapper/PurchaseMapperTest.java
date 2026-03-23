package com.seibel.basic.database.db.mapper;

import com.seibel.basic.common.domain.Purchase;
import com.seibel.basic.database.db.entity.PurchaseDb;
import com.seibel.basic.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PurchaseMapperTest {

    private PurchaseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PurchaseMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        PurchaseDb db = DomainBuilderDatabase.getPurchaseDb();

        // Act
        Purchase domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getCustomer(), domain.getCustomer());
        assertEquals(db.getItems(), domain.getItems());
        assertEquals(db.getStatus(), domain.getStatus());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Purchase domain = DomainBuilderDatabase.getPurchase();

        // Act
        PurchaseDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getCustomer(), db.getCustomer());
        assertEquals(domain.getItems(), db.getItems());
        assertEquals(domain.getStatus(), db.getStatus());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        PurchaseDb db1 = DomainBuilderDatabase.getPurchaseDb();
        PurchaseDb db2 = DomainBuilderDatabase.getPurchaseDb();
        List<PurchaseDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Purchase> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<PurchaseDb> dbList = Arrays.asList();

        // Act
        List<Purchase> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Purchase> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Purchase domain1 = DomainBuilderDatabase.getPurchase();
        Purchase domain2 = DomainBuilderDatabase.getPurchase();
        List<Purchase> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<PurchaseDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Purchase> domainList = Arrays.asList();

        // Act
        List<PurchaseDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<PurchaseDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
