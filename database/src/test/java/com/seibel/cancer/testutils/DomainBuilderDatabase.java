package com.seibel.cancer.testutils;

import com.seibel.cancer.common.domain.Customer;
import com.seibel.cancer.common.domain.Purchase;
import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.domain.User;
import com.seibel.cancer.database.db.entity.CustomerDb;
import com.seibel.cancer.database.db.entity.PurchaseDb;
import com.seibel.cancer.database.db.entity.TrialDb;
import com.seibel.cancer.database.db.entity.UserDb;
import com.seibel.cancer.database.db.mapper.CustomerMapper;
import com.seibel.cancer.database.db.mapper.PurchaseMapper;
import com.seibel.cancer.database.db.mapper.TrialMapper;
import com.seibel.cancer.database.db.mapper.UserMapper;

import java.util.UUID;

public class DomainBuilderDatabase extends DomainBuilderBase {

    // ///////////////////////////////////////////////////////////////////
    // Customer
    public static Customer getCustomer() {
        CustomerDb item = getCustomerDb();
        return new CustomerMapper().toModel(item);
    }

    public static Customer getCustomer(CustomerDb item) {
        return new CustomerMapper().toModel(item);
    }

    public static CustomerDb getCustomerDb() {
        return getCustomerDb(null, null, null, null);
    }

    public static CustomerDb getCustomerDb(String code, String name) {
        return getCustomerDb(code, name, null, null);
    }

    public static CustomerDb getCustomerDb(String code, String name, String description, String extid) {
        return getCustomerDb(code, name, null, description, null, null, extid);
    }

    public static CustomerDb getCustomerDb(String code, String name, String contactName, String description, String contactEmail, String contactPhone, String extid) {
        CustomerDb item = new CustomerDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setCode(code != null ? code : getCodeRandom("CU_"));
        item.setName(name != null ? name : getNameRandom("Customer_"));
        item.setContactName(contactName != null ? contactName : getNameRandom("Contact_"));
        item.setDescription(description != null ? description : getDescriptionRandom("Customer Description "));
        item.setContactEmail(contactEmail != null ? contactEmail : getEmailRandom("customer"));
        item.setContactPhone(contactPhone != null ? contactPhone : getPhoneRandom());
        setBaseSyncFields(item);
        return item;
    }


    // ///////////////////////////////////////////////////////////////////
    // Purchase
    public static Purchase getPurchase() {
        PurchaseDb item = getPurchaseDb();
        return new PurchaseMapper().toModel(item);
    }

    public static Purchase getPurchase(PurchaseDb item) {
        return new PurchaseMapper().toModel(item);
    }

    public static PurchaseDb getPurchaseDb() {
        return getPurchaseDb(null, null, null, null);
    }

    public static PurchaseDb getPurchaseDb(String customer, String items) {
        return getPurchaseDb(customer, items, null, null);
    }

    public static PurchaseDb getPurchaseDb(String customer, String items, String status, String extid) {
        PurchaseDb item = new PurchaseDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setCustomer(customer != null ? customer : getCodeRandom("PUR_"));
        item.setItems(items != null ? items : getNameRandom("Items_"));
        item.setStatus(status != null ? status : getNameRandom("Status_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // Trial
    public static Trial getTrial() {
        TrialDb item = getTrialDb();
        return new TrialMapper().toModel(item);
    }

    public static Trial getTrial(TrialDb item) {
        return new TrialMapper().toModel(item);
    }

    public static TrialDb getTrialDb() {
        return getTrialDb(null, null, null, null);
    }

    public static TrialDb getTrialDb(String nctId, String briefTitle) {
        return getTrialDb(nctId, briefTitle, null, null);
    }

    public static TrialDb getTrialDb(String nctId, String briefTitle, String overallStatus, String extid) {
        TrialDb item = new TrialDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setNctId(nctId != null ? nctId : getCodeRandom("NCT"));
        item.setBriefTitle(briefTitle != null ? briefTitle : getNameRandom("Trial_"));
        item.setOfficialTitle(getNameRandom("OfficialTrial_"));
        item.setOverallStatus(overallStatus != null ? overallStatus : getStatusRandom("Sta_"));
        item.setStudyType(getStatusRandom("Type_"));
        item.setBriefSummary(getDescriptionRandom("Summary "));
        item.setDetailedDescription(getDescriptionRandom("Description "));
        item.setStartDate(getDateRandom());
        item.setPrimaryCompletionDate(getDateRandom());
        item.setCompletionDate(getDateRandom());
        item.setLastUpdatePostedDate(getDateRandom());
        item.setEnrollmentCount(100);
        item.setEnrollmentType(getStatusRandom("Enr_"));
        item.setHealthyVolunteers(getBooleanRandom());
        item.setSex("ALL");
        item.setMinimumAge("18 Years");
        item.setMaximumAge("65 Years");
        item.setEligibilityCriteria(getDescriptionRandom("Eligibility "));
        item.setIsPaidStudy(getBooleanRandom());
        item.setPaidAmount(getDecimalRandom());
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // User
    public static User getUser() {
        UserDb item = getUserDb();
        return new UserMapper().toModel(item);
    }

    public static User getUser(UserDb item) {
        return new UserMapper().toModel(item);
    }

    public static UserDb getUserDb() {
        return getUserDb(null, null, null, null, null);
    }

    public static UserDb getUserDb(String username, String password) {
        return getUserDb(username, password, null, null, null);
    }

    public static UserDb getUserDb(String username, String password, String email, String role, String extid) {
        UserDb item = new UserDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setUsername(username != null ? username : getCodeRandom("USR_"));
        item.setPassword(password != null ? password : getNameRandom("Pass_"));
        item.setEmail(email != null ? email : getEmailRandom("user"));
        item.setRole(role != null ? role : "USER");
        setBaseSyncFields(item);
        return item;
    }

}
