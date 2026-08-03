package com.seibel.cancer.testutils;

import com.seibel.cancer.common.domain.AppUser;
import com.seibel.cancer.common.domain.ArmGroup;
import com.seibel.cancer.common.domain.Condition;
import com.seibel.cancer.common.domain.Customer;
import com.seibel.cancer.common.domain.EligibilityRule;
import com.seibel.cancer.common.domain.Intervention;
import com.seibel.cancer.common.domain.Keyword;
import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.common.domain.Medication;
import com.seibel.cancer.common.domain.Outcome;
import com.seibel.cancer.common.domain.OverallOfficial;
import com.seibel.cancer.common.domain.Purchase;
import com.seibel.cancer.common.domain.Sponsor;
import com.seibel.cancer.common.domain.StagingRawTrial;
import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.domain.TrialSource;
import com.seibel.cancer.common.domain.TrialStatus;
import com.seibel.cancer.common.domain.User;
import com.seibel.cancer.database.db.entity.AppUserDb;
import com.seibel.cancer.database.db.entity.ArmGroupDb;
import com.seibel.cancer.database.db.entity.MedicalConditionDb;
import com.seibel.cancer.database.db.entity.CustomerDb;
import com.seibel.cancer.database.db.entity.EligibilityRuleDb;
import com.seibel.cancer.database.db.entity.InterventionDb;
import com.seibel.cancer.database.db.entity.KeywordDb;
import com.seibel.cancer.database.db.entity.LocationDb;
import com.seibel.cancer.database.db.entity.MedicationDb;
import com.seibel.cancer.database.db.entity.OutcomeDb;
import com.seibel.cancer.database.db.entity.OverallOfficialDb;
import com.seibel.cancer.database.db.entity.PurchaseDb;
import com.seibel.cancer.database.db.entity.SponsorDb;
import com.seibel.cancer.database.db.entity.StagingRawTrialDb;
import com.seibel.cancer.database.db.entity.TrialDb;
import com.seibel.cancer.database.db.entity.TrialSourceDb;
import com.seibel.cancer.database.db.entity.TrialStatusDb;
import com.seibel.cancer.database.db.entity.UserDb;
import com.seibel.cancer.database.db.mapper.AppUserMapper;
import com.seibel.cancer.database.db.mapper.ArmGroupMapper;
import com.seibel.cancer.database.db.mapper.ConditionMapper;
import com.seibel.cancer.database.db.mapper.CustomerMapper;
import com.seibel.cancer.database.db.mapper.EligibilityRuleMapper;
import com.seibel.cancer.database.db.mapper.InterventionMapper;
import com.seibel.cancer.database.db.mapper.KeywordMapper;
import com.seibel.cancer.database.db.mapper.LocationMapper;
import com.seibel.cancer.database.db.mapper.MedicationMapper;
import com.seibel.cancer.database.db.mapper.OutcomeMapper;
import com.seibel.cancer.database.db.mapper.OverallOfficialMapper;
import com.seibel.cancer.database.db.mapper.PurchaseMapper;
import com.seibel.cancer.database.db.mapper.SponsorMapper;
import com.seibel.cancer.database.db.mapper.StagingRawTrialMapper;
import com.seibel.cancer.database.db.mapper.TrialMapper;
import com.seibel.cancer.database.db.mapper.TrialSourceMapper;
import com.seibel.cancer.database.db.mapper.TrialStatusMapper;
import com.seibel.cancer.database.db.mapper.UserMapper;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
        item.setPrimaryTrialSourceId(1L);
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
    // Condition
    public static Condition getCondition() {
        MedicalConditionDb item = getConditionDb();
        return new ConditionMapper().toModel(item);
    }

    public static Condition getCondition(MedicalConditionDb item) {
        return new ConditionMapper().toModel(item);
    }

    public static MedicalConditionDb getConditionDb() {
        return getConditionDb(null, null);
    }

    public static MedicalConditionDb getConditionDb(String name) {
        return getConditionDb(name, null);
    }

    public static MedicalConditionDb getConditionDb(String name, String extid) {
        MedicalConditionDb item = new MedicalConditionDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setName(name != null ? name : getNameRandom("Condition_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // EligibilityRule
    public static EligibilityRule getEligibilityRule() {
        EligibilityRuleDb item = getEligibilityRuleDb();
        return new EligibilityRuleMapper().toModel(item);
    }

    public static EligibilityRule getEligibilityRule(EligibilityRuleDb item) {
        return new EligibilityRuleMapper().toModel(item);
    }

    public static EligibilityRuleDb getEligibilityRuleDb() {
        return getEligibilityRuleDb(null, null, null, null);
    }

    public static EligibilityRuleDb getEligibilityRuleDb(Long trialId, String nodeType) {
        return getEligibilityRuleDb(trialId, nodeType, null, null);
    }

    public static EligibilityRuleDb getEligibilityRuleDb(Long trialId, String nodeType, String operator, String extid) {
        EligibilityRuleDb item = new EligibilityRuleDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setTrialId(trialId != null ? trialId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setParentRuleId(ThreadLocalRandom.current().nextLong(1, 100000));
        item.setNodeType(nodeType != null ? nodeType : getCodeRandom("ND_"));
        item.setOperator(operator != null ? operator : getCodeRandom("OP_"));
        item.setCriterionType(getLabelRandom("CritT_"));
        item.setCriterionId(ThreadLocalRandom.current().nextLong(1, 100000));
        item.setRequirementType(getLabelRandom("ReqT_"));
        item.setSortOrder(ThreadLocalRandom.current().nextInt(0, 1000));
        item.setNotes(getDescriptionRandom("Notes "));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // Intervention
    public static Intervention getIntervention() {
        InterventionDb item = getInterventionDb();
        return new InterventionMapper().toModel(item);
    }

    public static Intervention getIntervention(InterventionDb item) {
        return new InterventionMapper().toModel(item);
    }

    public static InterventionDb getInterventionDb() {
        return getInterventionDb(null, null, null, null);
    }

    public static InterventionDb getInterventionDb(String type, String name) {
        return getInterventionDb(type, name, null, null);
    }

    public static InterventionDb getInterventionDb(String type, String name, Long trialId, String extid) {
        InterventionDb item = new InterventionDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setTrialId(trialId != null ? trialId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setType(type != null ? type : getStatusRandom("Type_"));
        item.setName(name != null ? name : getNameRandom("Intervention_"));
        item.setDescription(getDescriptionRandom("Intervention Description "));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // Keyword
    public static Keyword getKeyword() {
        KeywordDb item = getKeywordDb();
        return new KeywordMapper().toModel(item);
    }

    public static Keyword getKeyword(KeywordDb item) {
        return new KeywordMapper().toModel(item);
    }

    public static KeywordDb getKeywordDb() {
        return getKeywordDb(null, null);
    }

    public static KeywordDb getKeywordDb(String name, String extid) {
        KeywordDb item = new KeywordDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setName(name != null ? name : getNameRandom("Keyword_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // ArmGroup
    public static ArmGroup getArmGroup() {
        ArmGroupDb item = getArmGroupDb();
        return new ArmGroupMapper().toModel(item);
    }

    public static ArmGroup getArmGroup(ArmGroupDb item) {
        return new ArmGroupMapper().toModel(item);
    }

    public static ArmGroupDb getArmGroupDb() {
        return getArmGroupDb(null, null, null, null, null);
    }

    public static ArmGroupDb getArmGroupDb(Long trialId, String label) {
        return getArmGroupDb(trialId, label, null, null, null);
    }

    public static ArmGroupDb getArmGroupDb(Long trialId, String label, String type, String description, String extid) {
        ArmGroupDb item = new ArmGroupDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setTrialId(trialId != null ? trialId : 1L);
        item.setLabel(label != null ? label : getNameRandom("Arm_"));
        item.setType(type != null ? type : getStatusRandom("Type_"));
        item.setDescription(description != null ? description : getDescriptionRandom("ArmGroup Description "));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // Outcome
    public static Outcome getOutcome() {
        OutcomeDb item = getOutcomeDb();
        return new OutcomeMapper().toModel(item);
    }

    public static Outcome getOutcome(OutcomeDb item) {
        return new OutcomeMapper().toModel(item);
    }

    public static OutcomeDb getOutcomeDb() {
        return getOutcomeDb(null, null, null, null);
    }

    public static OutcomeDb getOutcomeDb(Long trialId, String outcomeType) {
        return getOutcomeDb(trialId, outcomeType, null, null);
    }

    public static OutcomeDb getOutcomeDb(Long trialId, String outcomeType, String measure, String extid) {
        OutcomeDb item = new OutcomeDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setTrialId(trialId != null ? trialId : 1L);
        item.setOutcomeType(outcomeType != null ? outcomeType : getCodeRandom("Typ_"));
        item.setMeasure(measure != null ? measure : getDescriptionRandom("Measure "));
        item.setDescription(getDescriptionRandom("Outcome Description "));
        item.setTimeFrame(getNameRandom("TimeFrame_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // StagingRawTrial
    public static StagingRawTrial getStagingRawTrial() {
        StagingRawTrialDb item = getStagingRawTrialDb();
        return new StagingRawTrialMapper().toModel(item);
    }

    public static StagingRawTrial getStagingRawTrial(StagingRawTrialDb item) {
        return new StagingRawTrialMapper().toModel(item);
    }

    public static StagingRawTrialDb getStagingRawTrialDb() {
        return getStagingRawTrialDb(null, null, null, null);
    }

    public static StagingRawTrialDb getStagingRawTrialDb(Long trialSourceId, String sourceTrialId) {
        return getStagingRawTrialDb(trialSourceId, sourceTrialId, null, null);
    }

    public static StagingRawTrialDb getStagingRawTrialDb(Long trialSourceId, String sourceTrialId, String normalizationError, String extid) {
        StagingRawTrialDb item = new StagingRawTrialDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setTrialSourceId(trialSourceId != null ? trialSourceId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setSourceTrialId(sourceTrialId != null ? sourceTrialId : getCodeRandom("SRC_"));
        item.setRawPayload(getDescriptionRandom("Payload "));
        item.setFetchedAt(getDateTimeRandom());
        item.setNormalizedAt(getDateTimeRandom());
        item.setNormalizationError(normalizationError != null ? normalizationError : getDescriptionRandom("Error "));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // TrialStatus
    public static TrialStatus getTrialStatus() {
        TrialStatusDb item = getTrialStatusDb();
        return new TrialStatusMapper().toModel(item);
    }

    public static TrialStatus getTrialStatus(TrialStatusDb item) {
        return new TrialStatusMapper().toModel(item);
    }

    public static TrialStatusDb getTrialStatusDb() {
        return getTrialStatusDb(null, null, null, null);
    }

    public static TrialStatusDb getTrialStatusDb(Long trialId, Long appUserId) {
        return getTrialStatusDb(trialId, appUserId, null, null);
    }

    public static TrialStatusDb getTrialStatusDb(Long trialId, Long appUserId, String status, String extid) {
        TrialStatusDb item = new TrialStatusDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setTrialId(trialId != null ? trialId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setAppUserId(appUserId != null ? appUserId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setStatus(status != null ? status : getStatusRandom("Sta_"));
        item.setNotes(getDescriptionRandom("Notes "));
        item.setStatusChangedAt(LocalDateTime.now());
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // Medication
    public static Medication getMedication() {
        MedicationDb item = getMedicationDb();
        return new MedicationMapper().toModel(item);
    }

    public static Medication getMedication(MedicationDb item) {
        return new MedicationMapper().toModel(item);
    }

    public static MedicationDb getMedicationDb() {
        return getMedicationDb(null, null);
    }

    public static MedicationDb getMedicationDb(String name, String extid) {
        MedicationDb item = new MedicationDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setName(name != null ? name : getNameRandom("Medication_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // TrialSource
    public static TrialSource getTrialSource() {
        TrialSourceDb item = getTrialSourceDb();
        return new TrialSourceMapper().toModel(item);
    }

    public static TrialSource getTrialSource(TrialSourceDb item) {
        return new TrialSourceMapper().toModel(item);
    }

    public static TrialSourceDb getTrialSourceDb() {
        return getTrialSourceDb(null, null, null, null);
    }

    public static TrialSourceDb getTrialSourceDb(String code, String name) {
        return getTrialSourceDb(code, name, null, null);
    }

    public static TrialSourceDb getTrialSourceDb(String code, String name, String baseUrl, String extid) {
        TrialSourceDb item = new TrialSourceDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setCode(code != null ? code : getCodeRandom("TS_"));
        item.setName(name != null ? name : getNameRandom("TrialSource_"));
        item.setBaseUrl(baseUrl != null ? baseUrl : "https://" + getCodeRandom("src") + ".example.com");
        setBaseSyncFields(item);
        return item;
    }


    // ///////////////////////////////////////////////////////////////////
    // Sponsor
    public static Sponsor getSponsor() {
        SponsorDb item = getSponsorDb();
        return new SponsorMapper().toModel(item);
    }

    public static Sponsor getSponsor(SponsorDb item) {
        return new SponsorMapper().toModel(item);
    }

    public static SponsorDb getSponsorDb() {
        return getSponsorDb(null, null, null);
    }

    public static SponsorDb getSponsorDb(String name, String orgClass) {
        return getSponsorDb(name, orgClass, null);
    }

    public static SponsorDb getSponsorDb(String name, String orgClass, String extid) {
        SponsorDb item = new SponsorDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setName(name != null ? name : getNameRandom("Sponsor_"));
        item.setOrgClass(orgClass != null ? orgClass : getLabelRandom("Org_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // Location
    public static Location getLocation() {
        LocationDb item = getLocationDb();
        return new LocationMapper().toModel(item);
    }

    public static Location getLocation(LocationDb item) {
        return new LocationMapper().toModel(item);
    }

    public static LocationDb getLocationDb() {
        return getLocationDb(null, null, null, null);
    }

    public static LocationDb getLocationDb(Long trialId, String facility) {
        return getLocationDb(trialId, facility, null, null);
    }

    public static LocationDb getLocationDb(Long trialId, String facility, String status, String extid) {
        LocationDb item = new LocationDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setTrialId(trialId != null ? trialId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setFacility(facility != null ? facility : getNameRandom("Facility_"));
        item.setCity(getNameRandom("City_"));
        item.setState(getLabelRandom("State_"));
        item.setZip(getCodeRandom("ZIP_"));
        item.setCountry(getNameRandom("Country_"));
        item.setStatus(status != null ? status : getStatusRandom("Sta_"));
        item.setLatitude(getDecimalRandom());
        item.setLongitude(getDecimalRandom());
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // AppUser
    public static AppUser getAppUser() {
        AppUserDb item = getAppUserDb();
        return new AppUserMapper().toModel(item);
    }

    public static AppUser getAppUser(AppUserDb item) {
        return new AppUserMapper().toModel(item);
    }

    public static AppUserDb getAppUserDb() {
        return getAppUserDb(null, null, null, null);
    }

    public static AppUserDb getAppUserDb(String username, String passwordHash) {
        return getAppUserDb(username, passwordHash, null, null);
    }

    public static AppUserDb getAppUserDb(String username, String passwordHash, String displayName, String extid) {
        AppUserDb item = new AppUserDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setUsername(username != null ? username : getCodeRandom("APU_"));
        item.setPasswordHash(passwordHash != null ? passwordHash : getUniqueRandom("Hash_"));
        item.setDisplayName(displayName != null ? displayName : getNameRandom("Display_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // OverallOfficial
    public static OverallOfficial getOverallOfficial() {
        OverallOfficialDb item = getOverallOfficialDb();
        return new OverallOfficialMapper().toModel(item);
    }

    public static OverallOfficial getOverallOfficial(OverallOfficialDb item) {
        return new OverallOfficialMapper().toModel(item);
    }

    public static OverallOfficialDb getOverallOfficialDb() {
        return getOverallOfficialDb(null, null, null, null, null);
    }

    public static OverallOfficialDb getOverallOfficialDb(Long trialId, String name) {
        return getOverallOfficialDb(trialId, name, null, null, null);
    }

    public static OverallOfficialDb getOverallOfficialDb(Long trialId, String name, String affiliation, String role, String extid) {
        OverallOfficialDb item = new OverallOfficialDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setTrialId(trialId != null ? trialId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setName(name != null ? name : getNameRandom("Official_"));
        item.setAffiliation(affiliation != null ? affiliation : getNameRandom("Affiliation_"));
        item.setRole(role != null ? role : getStatusRandom("Role_"));
        setBaseSyncFields(item);
        return item;
    }

}
