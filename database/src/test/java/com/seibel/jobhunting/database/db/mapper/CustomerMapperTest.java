package com.seibel.jobhunting.database.db.mapper;

import com.seibel.jobhunting.common.domain.Customer;
import com.seibel.jobhunting.database.db.entity.CustomerDb;
import com.seibel.jobhunting.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomerMapperTest {

    private CustomerMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CustomerMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        CustomerDb db = DomainBuilderDatabase.getCustomerDb();

        // Act
        Customer domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getCode(), domain.getCode());
        assertEquals(db.getName(), domain.getName());
        assertEquals(db.getContactName(), domain.getContactName());
        assertEquals(db.getDescription(), domain.getDescription());
        assertEquals(db.getContactEmail(), domain.getContactEmail());
        assertEquals(db.getContactPhone(), domain.getContactPhone());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Customer domain = DomainBuilderDatabase.getCustomer();

        // Act
        CustomerDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getCode(), db.getCode());
        assertEquals(domain.getName(), db.getName());
        assertEquals(domain.getContactName(), db.getContactName());
        assertEquals(domain.getDescription(), db.getDescription());
        assertEquals(domain.getContactEmail(), db.getContactEmail());
        assertEquals(domain.getContactPhone(), db.getContactPhone());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        CustomerDb db1 = DomainBuilderDatabase.getCustomerDb();
        CustomerDb db2 = DomainBuilderDatabase.getCustomerDb();
        List<CustomerDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Customer> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<CustomerDb> dbList = Arrays.asList();

        // Act
        List<Customer> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Customer> domainList = mapper.toModelList(null);

        // Assert
        assertNull(domainList);
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Customer domain1 = DomainBuilderDatabase.getCustomer();
        Customer domain2 = DomainBuilderDatabase.getCustomer();
        List<Customer> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<CustomerDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Customer> domainList = Arrays.asList();

        // Act
        List<CustomerDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<CustomerDb> dbList = mapper.toDbList(null);

        // Assert
        assertNull(dbList);
    }
}
