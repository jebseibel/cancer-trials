package com.seibel.jobhunting.testutils;

import com.seibel.jobhunting.common.domain.Application;
import com.seibel.jobhunting.common.domain.Company;
import com.seibel.jobhunting.common.domain.Contact;
import com.seibel.jobhunting.common.domain.Customer;
import com.seibel.jobhunting.common.domain.Friend;
import com.seibel.jobhunting.common.domain.FriendCompany;
import com.seibel.jobhunting.common.domain.FriendJobPosting;
import com.seibel.jobhunting.common.domain.FriendSkill;
import com.seibel.jobhunting.common.domain.JobPosting;
import com.seibel.jobhunting.common.domain.JobPostingSkill;
import com.seibel.jobhunting.common.domain.Purchase;
import com.seibel.jobhunting.common.domain.Skill;
import com.seibel.jobhunting.common.domain.User;
import com.seibel.jobhunting.common.domain.UserSkill;
import com.seibel.jobhunting.common.enums.ApplicationStatus;
import com.seibel.jobhunting.common.enums.JobPostingStatus;
import com.seibel.jobhunting.common.enums.JobSource;
import com.seibel.jobhunting.common.enums.WorkMode;
import com.seibel.jobhunting.database.db.entity.ApplicationDb;
import com.seibel.jobhunting.database.db.entity.CompanyDb;
import com.seibel.jobhunting.database.db.entity.ContactDb;
import com.seibel.jobhunting.database.db.entity.CustomerDb;
import com.seibel.jobhunting.database.db.entity.FriendCompanyDb;
import com.seibel.jobhunting.database.db.entity.FriendDb;
import com.seibel.jobhunting.database.db.entity.FriendJobPostingDb;
import com.seibel.jobhunting.database.db.entity.FriendSkillDb;
import com.seibel.jobhunting.database.db.entity.JobPostingDb;
import com.seibel.jobhunting.database.db.entity.JobPostingSkillDb;
import com.seibel.jobhunting.database.db.entity.PurchaseDb;
import com.seibel.jobhunting.database.db.entity.SkillDb;
import com.seibel.jobhunting.database.db.entity.UserDb;
import com.seibel.jobhunting.database.db.entity.UserSkillDb;
import com.seibel.jobhunting.database.db.mapper.ApplicationMapper;
import com.seibel.jobhunting.database.db.mapper.CompanyMapper;
import com.seibel.jobhunting.database.db.mapper.ContactMapper;
import com.seibel.jobhunting.database.db.mapper.CustomerMapper;
import com.seibel.jobhunting.database.db.mapper.FriendCompanyMapper;
import com.seibel.jobhunting.database.db.mapper.FriendJobPostingMapper;
import com.seibel.jobhunting.database.db.mapper.FriendMapper;
import com.seibel.jobhunting.database.db.mapper.FriendSkillMapper;
import com.seibel.jobhunting.database.db.mapper.JobPostingMapper;
import com.seibel.jobhunting.database.db.mapper.JobPostingSkillMapper;
import com.seibel.jobhunting.database.db.mapper.PurchaseMapper;
import com.seibel.jobhunting.database.db.mapper.SkillMapper;
import com.seibel.jobhunting.database.db.mapper.UserMapper;
import com.seibel.jobhunting.database.db.mapper.UserSkillMapper;

import java.time.LocalDate;
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

    // ///////////////////////////////////////////////////////////////////
    // JobPosting
    public static JobPosting getJobPosting() {
        JobPostingDb item = getJobPostingDb();
        return new JobPostingMapper().toModel(item);
    }

    public static JobPosting getJobPosting(JobPostingDb item) {
        return new JobPostingMapper().toModel(item);
    }

    public static JobPostingDb getJobPostingDb() {
        return getJobPostingDb(null, null, null, null);
    }

    public static JobPostingDb getJobPostingDb(String title, Long companyId) {
        return getJobPostingDb(title, companyId, null, null);
    }

    public static JobPostingDb getJobPostingDb(String title, Long companyId, String sourceUrl, String extid) {
        JobPostingDb item = new JobPostingDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setTitle(title != null ? title : getNameRandom("Title_"));
        item.setCompanyId(companyId != null ? companyId : 1L);
        item.setDescription(getDescriptionRandom("JobPosting Description "));
        item.setCity("Springfield");
        item.setState("IL");
        item.setCountry("USA");
        item.setWorkMode(WorkMode.REMOTE);
        item.setSalaryMin(80000);
        item.setSalaryMax(120000);
        item.setSalaryCurrency("USD");
        item.setSource(JobSource.MANUAL);
        item.setSourceUrl(sourceUrl != null ? sourceUrl : "https://example.com/jobs/" + randomString());
        item.setStatus(JobPostingStatus.NEW);
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // Company
    public static Company getCompany() {
        CompanyDb item = getCompanyDb();
        return new CompanyMapper().toModel(item);
    }

    public static Company getCompany(CompanyDb item) {
        return new CompanyMapper().toModel(item);
    }

    public static CompanyDb getCompanyDb() {
        return getCompanyDb(null, null);
    }

    public static CompanyDb getCompanyDb(String name, String extid) {
        CompanyDb item = new CompanyDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setName(name != null ? name : getNameRandom("Company_"));
        item.setWebsite("https://example.com/" + randomString());
        item.setIndustry(getNameRandom("Industry_"));
        item.setNotes(getDescriptionRandom("Company Notes "));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // Skill
    public static Skill getSkill() {
        SkillDb item = getSkillDb();
        return new SkillMapper().toModel(item);
    }

    public static Skill getSkill(SkillDb item) {
        return new SkillMapper().toModel(item);
    }

    public static SkillDb getSkillDb() {
        return getSkillDb(null, null);
    }

    public static SkillDb getSkillDb(String name, String extid) {
        SkillDb item = new SkillDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setName(name != null ? name : getNameRandom("Skill_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // Application
    public static Application getApplication() {
        ApplicationDb item = getApplicationDb();
        return new ApplicationMapper().toModel(item);
    }

    public static Application getApplication(ApplicationDb item) {
        return new ApplicationMapper().toModel(item);
    }

    public static ApplicationDb getApplicationDb() {
        return getApplicationDb(null, null);
    }

    public static ApplicationDb getApplicationDb(Long jobPostingId, String extid) {
        ApplicationDb item = new ApplicationDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setJobPostingId(jobPostingId != null ? jobPostingId : 1L);
        item.setDateApplied(LocalDate.now());
        item.setResumeVersion(getVersionRandom("Resume_"));
        item.setApplicationStatus(ApplicationStatus.APPLIED);
        item.setNotes(getDescriptionRandom("Application Notes "));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // Contact
    public static Contact getContact() {
        ContactDb item = getContactDb();
        return new ContactMapper().toModel(item);
    }

    public static Contact getContact(ContactDb item) {
        return new ContactMapper().toModel(item);
    }

    public static ContactDb getContactDb() {
        return getContactDb(null, null);
    }

    public static ContactDb getContactDb(String name, String extid) {
        ContactDb item = new ContactDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setCompanyId(1L);
        item.setJobPostingId(1L);
        item.setName(name != null ? name : getNameRandom("Contact_"));
        item.setRole(getNameRandom("Role_"));
        item.setEmail(getEmailRandom("contact"));
        item.setPhone(getPhoneRandom());
        item.setNotes(getDescriptionRandom("Contact Notes "));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // Friend
    public static Friend getFriend() {
        FriendDb item = getFriendDb();
        return new FriendMapper().toModel(item);
    }

    public static Friend getFriend(FriendDb item) {
        return new FriendMapper().toModel(item);
    }

    public static FriendDb getFriendDb() {
        return getFriendDb(null, null);
    }

    public static FriendDb getFriendDb(String name, String extid) {
        FriendDb item = new FriendDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setName(name != null ? name : getNameRandom("Friend_"));
        item.setRelationship(getNameRandom("Relationship_"));
        item.setEmail(getEmailRandom("friend"));
        item.setPhone(getPhoneRandom());
        item.setLinkedinUrl("https://linkedin.com/in/" + randomString());
        item.setLastContactedAt(LocalDate.now());
        item.setNotes(getDescriptionRandom("Friend Notes "));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // JobPostingSkill
    public static JobPostingSkill getJobPostingSkill() {
        JobPostingSkillDb item = getJobPostingSkillDb();
        return new JobPostingSkillMapper().toModel(item);
    }

    public static JobPostingSkill getJobPostingSkill(JobPostingSkillDb item) {
        return new JobPostingSkillMapper().toModel(item);
    }

    public static JobPostingSkillDb getJobPostingSkillDb() {
        return getJobPostingSkillDb(null, null, null);
    }

    public static JobPostingSkillDb getJobPostingSkillDb(Long jobPostingId, Long skillId, String extid) {
        JobPostingSkillDb item = new JobPostingSkillDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setJobPostingId(jobPostingId != null ? jobPostingId : 1L);
        item.setSkillId(skillId != null ? skillId : 1L);
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // UserSkill
    public static UserSkill getUserSkill() {
        UserSkillDb item = getUserSkillDb();
        return new UserSkillMapper().toModel(item);
    }

    public static UserSkill getUserSkill(UserSkillDb item) {
        return new UserSkillMapper().toModel(item);
    }

    public static UserSkillDb getUserSkillDb() {
        return getUserSkillDb(null, null, null);
    }

    public static UserSkillDb getUserSkillDb(Long userId, Long skillId, String extid) {
        UserSkillDb item = new UserSkillDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setUserId(userId != null ? userId : 1L);
        item.setSkillId(skillId != null ? skillId : 1L);
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // FriendSkill
    public static FriendSkill getFriendSkill() {
        FriendSkillDb item = getFriendSkillDb();
        return new FriendSkillMapper().toModel(item);
    }

    public static FriendSkill getFriendSkill(FriendSkillDb item) {
        return new FriendSkillMapper().toModel(item);
    }

    public static FriendSkillDb getFriendSkillDb() {
        return getFriendSkillDb(null, null, null);
    }

    public static FriendSkillDb getFriendSkillDb(Long friendId, Long skillId, String extid) {
        FriendSkillDb item = new FriendSkillDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setFriendId(friendId != null ? friendId : 1L);
        item.setSkillId(skillId != null ? skillId : 1L);
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // FriendCompany
    public static FriendCompany getFriendCompany() {
        FriendCompanyDb item = getFriendCompanyDb();
        return new FriendCompanyMapper().toModel(item);
    }

    public static FriendCompany getFriendCompany(FriendCompanyDb item) {
        return new FriendCompanyMapper().toModel(item);
    }

    public static FriendCompanyDb getFriendCompanyDb() {
        return getFriendCompanyDb(null, null, null);
    }

    public static FriendCompanyDb getFriendCompanyDb(Long friendId, Long companyId, String extid) {
        FriendCompanyDb item = new FriendCompanyDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setFriendId(friendId != null ? friendId : 1L);
        item.setCompanyId(companyId != null ? companyId : 1L);
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // FriendJobPosting
    public static FriendJobPosting getFriendJobPosting() {
        FriendJobPostingDb item = getFriendJobPostingDb();
        return new FriendJobPostingMapper().toModel(item);
    }

    public static FriendJobPosting getFriendJobPosting(FriendJobPostingDb item) {
        return new FriendJobPostingMapper().toModel(item);
    }

    public static FriendJobPostingDb getFriendJobPostingDb() {
        return getFriendJobPostingDb(null, null, null);
    }

    public static FriendJobPostingDb getFriendJobPostingDb(Long friendId, Long jobPostingId, String extid) {
        FriendJobPostingDb item = new FriendJobPostingDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setFriendId(friendId != null ? friendId : 1L);
        item.setJobPostingId(jobPostingId != null ? jobPostingId : 1L);
        setBaseSyncFields(item);
        return item;
    }

}

