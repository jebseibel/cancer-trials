package com.seibel.jobs.database.db.mapper;

import com.seibel.jobs.common.domain.Contact;
import com.seibel.jobs.database.db.entity.ContactDb;
import com.seibel.jobs.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ContactMapperTest {

    private ContactMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ContactMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        ContactDb db = DomainBuilderDatabase.getContactDb();

        // Act
        Contact domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getCompanyId(), domain.getCompanyId());
        assertEquals(db.getJobPostingId(), domain.getJobPostingId());
        assertEquals(db.getName(), domain.getName());
        assertEquals(db.getRole(), domain.getRole());
        assertEquals(db.getEmail(), domain.getEmail());
        assertEquals(db.getPhone(), domain.getPhone());
        assertEquals(db.getNotes(), domain.getNotes());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Contact domain = DomainBuilderDatabase.getContact();

        // Act
        ContactDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getCompanyId(), db.getCompanyId());
        assertEquals(domain.getJobPostingId(), db.getJobPostingId());
        assertEquals(domain.getName(), db.getName());
        assertEquals(domain.getRole(), db.getRole());
        assertEquals(domain.getEmail(), db.getEmail());
        assertEquals(domain.getPhone(), db.getPhone());
        assertEquals(domain.getNotes(), db.getNotes());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        ContactDb db1 = DomainBuilderDatabase.getContactDb();
        ContactDb db2 = DomainBuilderDatabase.getContactDb();
        List<ContactDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Contact> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<ContactDb> dbList = Arrays.asList();

        // Act
        List<Contact> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Contact> domainList = mapper.toModelList(null);

        // Assert
        assertNull(domainList);
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Contact domain1 = DomainBuilderDatabase.getContact();
        Contact domain2 = DomainBuilderDatabase.getContact();
        List<Contact> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<ContactDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Contact> domainList = Arrays.asList();

        // Act
        List<ContactDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<ContactDb> dbList = mapper.toDbList(null);

        // Assert
        assertNull(dbList);
    }
}
