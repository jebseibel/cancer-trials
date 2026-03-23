package com.seibel.basic.testutils;

import com.seibel.basic.common.domain.Customer;
import com.seibel.basic.common.domain.Purchase;
import com.seibel.basic.common.domain.User;
import com.seibel.basic.database.db.entity.CustomerDb;
import com.seibel.basic.database.db.entity.PurchaseDb;
import com.seibel.basic.database.db.entity.UserDb;
import com.seibel.basic.database.db.mapper.CustomerMapper;
import com.seibel.basic.database.db.mapper.PurchaseMapper;
import com.seibel.basic.database.db.mapper.UserMapper;

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

