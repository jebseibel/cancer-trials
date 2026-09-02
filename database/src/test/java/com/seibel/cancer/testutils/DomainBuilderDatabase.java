package com.seibel.cancer.testutils;

import com.seibel.cancer.common.domain.ArmGroup;
import com.seibel.cancer.common.domain.Condition;
import com.seibel.cancer.common.domain.Customer;
import com.seibel.cancer.common.domain.EligibilityRule;
import com.seibel.cancer.common.domain.Intervention;
import com.seibel.cancer.common.domain.Keyword;
import com.seibel.cancer.common.domain.LabResult;
import com.seibel.cancer.common.domain.LabResultComponent;
import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.common.domain.Medication;
import com.seibel.cancer.common.domain.Outcome;
import com.seibel.cancer.common.domain.Patient;
import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.domain.PatientMedication;
import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.common.domain.OverallOfficial;
import com.seibel.cancer.common.domain.Purchase;
import com.seibel.cancer.common.domain.Sponsor;
import com.seibel.cancer.common.domain.StagingRawTrial;
import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.domain.SavedTrialMatch;
import com.seibel.cancer.common.domain.SavedTrialMatchCriterion;
import com.seibel.cancer.common.domain.TrialSource;
import com.seibel.cancer.common.domain.TrialStatus;
import com.seibel.cancer.common.domain.User;
import com.seibel.cancer.common.domain.UserPatient;
import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.database.db.entity.ArmGroupDb;
import com.seibel.cancer.database.db.entity.MedicalConditionDb;
import com.seibel.cancer.database.db.entity.PatientDb;
import com.seibel.cancer.database.db.entity.PatientDiagnosisDb;
import com.seibel.cancer.database.db.entity.PatientMedicationDb;
import com.seibel.cancer.database.db.entity.PatientPriorTreatmentDb;
import com.seibel.cancer.database.db.entity.PatientVariantDb;
import com.seibel.cancer.database.db.entity.CustomerDb;
import com.seibel.cancer.database.db.entity.EligibilityRuleDb;
import com.seibel.cancer.database.db.entity.InterventionDb;
import com.seibel.cancer.database.db.entity.KeywordDb;
import com.seibel.cancer.database.db.entity.LabResultDb;
import com.seibel.cancer.database.db.entity.LabResultComponentDb;
import com.seibel.cancer.database.db.entity.LocationDb;
import com.seibel.cancer.database.db.entity.MedicationDb;
import com.seibel.cancer.database.db.entity.OutcomeDb;
import com.seibel.cancer.database.db.entity.OverallOfficialDb;
import com.seibel.cancer.database.db.entity.PurchaseDb;
import com.seibel.cancer.database.db.entity.SponsorDb;
import com.seibel.cancer.database.db.entity.StagingRawTrialDb;
import com.seibel.cancer.database.db.entity.TrialDb;
import com.seibel.cancer.database.db.entity.SavedTrialMatchCriterionDb;
import com.seibel.cancer.database.db.entity.SavedTrialMatchDb;
import com.seibel.cancer.database.db.entity.TrialSourceDb;
import com.seibel.cancer.database.db.entity.TrialStatusDb;
import com.seibel.cancer.database.db.entity.UserDb;
import com.seibel.cancer.database.db.entity.UserPatientDb;
import com.seibel.cancer.database.db.mapper.ArmGroupMapper;
import com.seibel.cancer.database.db.mapper.ConditionMapper;
import com.seibel.cancer.database.db.mapper.CustomerMapper;
import com.seibel.cancer.database.db.mapper.EligibilityRuleMapper;
import com.seibel.cancer.database.db.mapper.InterventionMapper;
import com.seibel.cancer.database.db.mapper.KeywordMapper;
import com.seibel.cancer.database.db.mapper.LabResultMapper;
import com.seibel.cancer.database.db.mapper.LabResultComponentMapper;
import com.seibel.cancer.database.db.mapper.LocationMapper;
import com.seibel.cancer.database.db.mapper.MedicationMapper;
import com.seibel.cancer.database.db.mapper.OutcomeMapper;
import com.seibel.cancer.database.db.mapper.OverallOfficialMapper;
import com.seibel.cancer.database.db.mapper.PatientDiagnosisMapper;
import com.seibel.cancer.database.db.mapper.PatientMedicationMapper;
import com.seibel.cancer.database.db.mapper.PurchaseMapper;
import com.seibel.cancer.database.db.mapper.SponsorMapper;
import com.seibel.cancer.database.db.mapper.StagingRawTrialMapper;
import com.seibel.cancer.database.db.mapper.TrialMapper;
import com.seibel.cancer.database.db.mapper.SavedTrialMatchCriterionMapper;
import com.seibel.cancer.database.db.mapper.SavedTrialMatchMapper;
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
        item.setFriendlyTitle(getNameRandom("FriendlyTrial_"));
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
        // 64 hex chars, matching a real SHA-256 so the column's width is actually exercised.
        StringBuilder hash = new StringBuilder(64);
        for (int i = 0; i < 64; i++) {
            hash.append(Character.forDigit(ThreadLocalRandom.current().nextInt(16), 16));
        }
        item.setPayloadHash(hash.toString());
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

    public static TrialStatusDb getTrialStatusDb(Long trialId, Long patientId) {
        return getTrialStatusDb(trialId, patientId, null, null);
    }

    public static TrialStatusDb getTrialStatusDb(Long trialId, Long patientId, String status, String extid) {
        TrialStatusDb item = new TrialStatusDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setTrialId(trialId != null ? trialId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setPatientId(patientId != null ? patientId : ThreadLocalRandom.current().nextLong(1, 100000));
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
        item.setLatitude(getLatitudeRandom());
        item.setLongitude(getLongitudeRandom());
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
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

    // ///////////////////////////////////////////////////////////////////
    // PatientMedication
    public static PatientMedication getPatientMedication() {
        PatientMedicationDb item = getPatientMedicationDb();
        return new PatientMedicationMapper().toModel(item);
    }

    public static PatientMedication getPatientMedication(PatientMedicationDb item) {
        return new PatientMedicationMapper().toModel(item);
    }

    public static PatientMedicationDb getPatientMedicationDb() {
        return getPatientMedicationDb(null, null, null);
    }

    public static PatientMedicationDb getPatientMedicationDb(String fhirResourceId, String medicationName) {
        return getPatientMedicationDb(fhirResourceId, medicationName, null);
    }

    /**
     * Full-override builder. Only the two identity-relevant fields plus extid are exposed
     * positionally - the other 15 columns get type-matched random defaults, since a
     * 17-parameter signature would be unreadable and easy to transpose.
     */
    public static PatientMedicationDb getPatientMedicationDb(String fhirResourceId, String medicationName, String extid) {
        PatientMedicationDb item = new PatientMedicationDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setFhirResourceId(fhirResourceId != null ? fhirResourceId : getUniqueRandom("Fhir_"));
        item.setMedicationName(medicationName != null ? medicationName : getNameRandom("Med_"));
        item.setRxnormCode(getCodeRandom("Rx_"));
        item.setStatus(getStatusRandom("Sta_"));
        item.setIntent(getStatusRandom("Int_"));
        item.setAuthoredOn(getDateRandom());
        item.setDosageText(getDescriptionRandom("Dosage_"));
        item.setDoseQuantity(getDecimalRandom(3));
        item.setDoseUnit(getCodeRandom("Unit_"));
        item.setRoute(getNameRandom("Route_"));
        item.setFrequencyText(getNameRandom("Freq_"));
        item.setPrescriberName(getNameRandom("Presc_"));
        item.setReasonText(getDescriptionRandom("Reason_"));
        item.setValidityStart(getDateRandom());
        item.setValidityEnd(getDateRandom());
        item.setRefillsAllowed(getIntegerRandom());
        item.setDisplayText(getDescriptionRandom("Display_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // LabResult
    public static LabResult getLabResult() {
        LabResultDb item = getLabResultDb();
        return new LabResultMapper().toModel(item);
    }

    public static LabResult getLabResult(LabResultDb item) {
        return new LabResultMapper().toModel(item);
    }

    public static LabResultDb getLabResultDb() {
        return getLabResultDb(null, null, null);
    }

    public static LabResultDb getLabResultDb(String fhirResourceId, String testName) {
        return getLabResultDb(fhirResourceId, testName, null);
    }

    public static LabResultDb getLabResultDb(String fhirResourceId, String testName, String extid) {
        LabResultDb item = new LabResultDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setFhirResourceId(fhirResourceId != null ? fhirResourceId : getUniqueRandom("Fhir_"));
        item.setTestName(testName != null ? testName : getNameRandom("Test_"));
        item.setLoincCode(getCodeRandom("Loinc_"));
        item.setStatus(getStatusRandom("Sta_"));
        item.setCategory(getCodeRandom("Cat_"));
        item.setEffectiveAt(getDateTimeRandom());
        item.setIssuedAt(getDateTimeRandom());
        item.setValueQuantity(getDecimalRandom(6));
        item.setValueUnit(getCodeRandom("Unit_"));
        item.setValueString(getNameRandom("Val_"));
        item.setInterpretation(getCodeRandom("Interp_"));
        item.setReferenceRangeLow(getDecimalRandom(6));
        item.setReferenceRangeHigh(getDecimalRandom(6));
        item.setReferenceRangeText(getNameRandom("Range_"));
        item.setIsPanel(getBooleanRandom());
        item.setDisplayText(getDescriptionRandom("Display_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // LabResultComponent
    public static LabResultComponent getLabResultComponent() {
        LabResultComponentDb item = getLabResultComponentDb();
        return new LabResultComponentMapper().toModel(item);
    }

    public static LabResultComponent getLabResultComponent(LabResultComponentDb item) {
        return new LabResultComponentMapper().toModel(item);
    }

    public static LabResultComponentDb getLabResultComponentDb() {
        return getLabResultComponentDb(null, null, null);
    }

    public static LabResultComponentDb getLabResultComponentDb(Long labResultId, String componentName) {
        return getLabResultComponentDb(labResultId, componentName, null);
    }

    public static LabResultComponentDb getLabResultComponentDb(Long labResultId, String componentName, String extid) {
        LabResultComponentDb item = new LabResultComponentDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setLabResultId(labResultId != null ? labResultId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setComponentName(componentName != null ? componentName : getNameRandom("Component_"));
        item.setLoincCode(getCodeRandom("Loinc_"));
        item.setValueQuantity(getDecimalRandom(6));
        item.setValueUnit(getCodeRandom("Unit_"));
        item.setValueString(getNameRandom("Val_"));
        item.setInterpretation(getCodeRandom("Interp_"));
        item.setReferenceRangeLow(getDecimalRandom(6));
        item.setReferenceRangeHigh(getDecimalRandom(6));
        item.setReferenceRangeText(getNameRandom("Range_"));
        item.setDisplayText(getDescriptionRandom("Display_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // PatientDiagnosis
    public static PatientDiagnosis getPatientDiagnosis() {
        PatientDiagnosisDb item = getPatientDiagnosisDb();
        return new PatientDiagnosisMapper().toModel(item);
    }

    public static PatientDiagnosis getPatientDiagnosis(PatientDiagnosisDb item) {
        return new PatientDiagnosisMapper().toModel(item);
    }

    public static PatientDiagnosisDb getPatientDiagnosisDb() {
        return getPatientDiagnosisDb(null, null, null);
    }

    public static PatientDiagnosisDb getPatientDiagnosisDb(Long patientId, String cancerType) {
        return getPatientDiagnosisDb(patientId, cancerType, null);
    }

    /**
     * Full-override builder. Only the two identity-relevant fields plus extid are exposed
     * positionally - the other 19 columns get type-matched random defaults, since a
     * 21-parameter signature would be unreadable and easy to transpose.
     */
    public static PatientDiagnosisDb getPatientDiagnosisDb(Long patientId, String cancerType, String extid) {
        PatientDiagnosisDb item = new PatientDiagnosisDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setPatientId(patientId != null ? patientId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setCancerType(cancerType != null ? cancerType : getDescriptionRandom("Cancer_"));
        item.setStage(getVersionRandom("Stg_"));
        item.setStageSystem(getVersionRandom("Sys_"));
        item.setIsMetastatic(getBooleanRandom());
        item.setMetastasisSites(getDescriptionRandom("Mets_"));
        item.setReceptorSubtype(getUniqueRandom("Receptor_"));
        item.setErStatus(getVersionRandom("Er_"));
        item.setPrStatus(getVersionRandom("Pr_"));
        item.setHer2Status(getVersionRandom("Her2_"));
        item.setBiomarkers(getDescriptionRandom("Biomarkers_"));
        item.setEcogStatus(getIntegerRandom(0, 5));
        item.setPriorChemoRegimens(getIntegerRandom(0, 10));
        item.setLastChemoEndDate(getDateRandom());
        item.setPriorTreatments(getDescriptionRandom("Prior_"));
        item.setHasMeasurableDisease(getBooleanRandom());
        item.setMenopausalStatus(getVersionRandom("Meno_"));
        item.setDiagnosisDate(getDateRandom());
        item.setNotes(getDescriptionRandom("Notes_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // SavedTrialMatch
    // ///////////////////////////////////////////////////////////////////

    public static SavedTrialMatch getSavedTrialMatch() {
        SavedTrialMatchDb item = getSavedTrialMatchDb();
        return new SavedTrialMatchMapper().toModel(item);
    }

    public static SavedTrialMatch getSavedTrialMatch(SavedTrialMatchDb item) {
        return new SavedTrialMatchMapper().toModel(item);
    }

    public static SavedTrialMatchDb getSavedTrialMatchDb() {
        return getSavedTrialMatchDb(null, null, null);
    }

    public static SavedTrialMatchDb getSavedTrialMatchDb(Long trialId, String searchRunId) {
        return getSavedTrialMatchDb(trialId, searchRunId, null);
    }

    public static SavedTrialMatchDb getSavedTrialMatchDb(Long trialId, String searchRunId, String extid) {
        SavedTrialMatchDb item = new SavedTrialMatchDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setTrialId(trialId != null ? trialId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setSearchRunId(searchRunId != null ? searchRunId : UUID.randomUUID().toString());
        item.setPatientId(ThreadLocalRandom.current().nextLong(1, 100000));
        item.setPatientDiagnosisId(ThreadLocalRandom.current().nextLong(1, 100000));
        item.setQueryText(getDescriptionRandom("Query_"));
        // Bounded 0..1: top_score is decimal(6,4) and getDecimalRandom's 0..10000 range
        // would not fit.
        item.setTopScore(getScoreRandom());
        item.setMatchRank(getIntegerRandom(1, 50));
        item.setSnapshotErStatus(getVersionRandom("Er_"));
        item.setSnapshotPrStatus(getVersionRandom("Pr_"));
        item.setSnapshotHer2Status(getVersionRandom("Her2_"));
        item.setSnapshotStage(getVersionRandom("Stg_"));
        item.setSnapshotBiomarkers(getDescriptionRandom("Bio_"));
        item.setMatchedAt(LocalDateTime.now());
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // SavedTrialMatchCriterion
    // ///////////////////////////////////////////////////////////////////

    public static SavedTrialMatchCriterion getSavedTrialMatchCriterion() {
        SavedTrialMatchCriterionDb item = getSavedTrialMatchCriterionDb();
        return new SavedTrialMatchCriterionMapper().toModel(item);
    }

    public static SavedTrialMatchCriterion getSavedTrialMatchCriterion(SavedTrialMatchCriterionDb item) {
        return new SavedTrialMatchCriterionMapper().toModel(item);
    }

    public static SavedTrialMatchCriterionDb getSavedTrialMatchCriterionDb() {
        return getSavedTrialMatchCriterionDb(null, null, null);
    }

    public static SavedTrialMatchCriterionDb getSavedTrialMatchCriterionDb(Long trialMatchId, String chunkText) {
        return getSavedTrialMatchCriterionDb(trialMatchId, chunkText, null);
    }

    public static SavedTrialMatchCriterionDb getSavedTrialMatchCriterionDb(Long trialMatchId, String chunkText, String extid) {
        SavedTrialMatchCriterionDb item = new SavedTrialMatchCriterionDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setTrialMatchId(trialMatchId != null ? trialMatchId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setChunkText(chunkText != null ? chunkText : getDescriptionRandom("Chunk_"));
        // Bounded 0..1: score is decimal(6,4).
        item.setScore(getScoreRandom());
        item.setIsExclusion(getBooleanRandom());
        item.setSource(getCodeRandom("Src_"));
        item.setOrdinal(getIntegerRandom(0, 100));
        setBaseSyncFields(item);
        return item;
    }

    // PatientVariant

    /**
     * Real vocabulary values rather than random strings: a failing assertion that prints
     * DETECTED/NOT_TESTED is readable, one that prints Sta_x7f2 is not.
     */
    private static final String[] VARIANT_STATUSES =
            {"DETECTED", "NOT_DETECTED", "VUS", "NOT_TESTED", "UNKNOWN"};

    private static String getVariantStatusRandom() {
        return VARIANT_STATUSES[ThreadLocalRandom.current().nextInt(VARIANT_STATUSES.length)];
    }

    public static PatientVariant getPatientVariant() {
        return getPatientVariant(getPatientVariantDb());
    }

    public static PatientVariant getPatientVariant(PatientVariantDb item) {
        return PatientVariant.builder()
                .id(item.getId())
                .extid(item.getExtid())
                .patientId(item.getPatientId())
                .patientDiagnosisId(item.getPatientDiagnosisId())
                .pik3caStatus(item.getPik3caStatus())
                .esr1Status(item.getEsr1Status())
                .tp53Status(item.getTp53Status())
                .akt1Status(item.getAkt1Status())
                .ptenStatus(item.getPtenStatus())
                .erbb2SomaticStatus(item.getErbb2SomaticStatus())
                .brca1Status(item.getBrca1Status())
                .brca2Status(item.getBrca2Status())
                .palb2Status(item.getPalb2Status())
                .atmStatus(item.getAtmStatus())
                .chek2Status(item.getChek2Status())
                .hrdStatus(item.getHrdStatus())
                .pdl1Status(item.getPdl1Status())
                .ki67Percent(item.getKi67Percent())
                .germlineTestDone(item.getGermlineTestDone())
                .somaticTestDone(item.getSomaticTestDone())
                .testDate(item.getTestDate())
                .testLab(item.getTestLab())
                .otherVariants(item.getOtherVariants())
                .notes(item.getNotes())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .deletedAt(item.getDeletedAt())
                .active(item.getActive())
                .build();
    }

    public static PatientVariantDb getPatientVariantDb() {
        return getPatientVariantDb(null, null);
    }

    public static PatientVariantDb getPatientVariantDb(Long patientId, String pik3caStatus) {
        return getPatientVariantDb(patientId, pik3caStatus, null);
    }

    public static PatientVariantDb getPatientVariantDb(Long patientId, String pik3caStatus, String extid) {
        PatientVariantDb item = new PatientVariantDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setPatientId(patientId != null ? patientId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setPatientDiagnosisId(ThreadLocalRandom.current().nextLong(1, 100000));
        item.setPik3caStatus(pik3caStatus != null ? pik3caStatus : getVariantStatusRandom());
        item.setEsr1Status(getVariantStatusRandom());
        item.setTp53Status(getVariantStatusRandom());
        item.setAkt1Status(getVariantStatusRandom());
        item.setPtenStatus(getVariantStatusRandom());
        item.setErbb2SomaticStatus(getVariantStatusRandom());
        item.setBrca1Status(getVariantStatusRandom());
        item.setBrca2Status(getVariantStatusRandom());
        item.setPalb2Status(getVariantStatusRandom());
        item.setAtmStatus(getVariantStatusRandom());
        item.setChek2Status(getVariantStatusRandom());
        item.setHrdStatus(getVariantStatusRandom());
        item.setPdl1Status(getVariantStatusRandom());
        // Bounded 0..100: ki67_percent is a percentage.
        item.setKi67Percent(getIntegerRandom(0, 101));
        item.setGermlineTestDone(getVariantStatusRandom());
        item.setSomaticTestDone(getVariantStatusRandom());
        item.setTestDate(getDateRandom());
        item.setTestLab(getNameRandom("Lab_"));
        item.setOtherVariants(getDescriptionRandom("Var_"));
        item.setNotes(getDescriptionRandom("Note_"));
        setBaseSyncFields(item);
        return item;
    }

    // PatientPriorTreatment

    /** The five states that separate treatment-naive from post-progression populations. */
    private static final String[] TREATMENT_STATUSES =
            {"NEVER", "CURRENT", "PROGRESSED", "STOPPED_OTHER", "UNKNOWN"};

    private static String getTreatmentStatusRandom() {
        return TREATMENT_STATUSES[ThreadLocalRandom.current().nextInt(TREATMENT_STATUSES.length)];
    }

    public static PatientPriorTreatment getPatientPriorTreatment() {
        return getPatientPriorTreatment(getPatientPriorTreatmentDb());
    }

    public static PatientPriorTreatment getPatientPriorTreatment(PatientPriorTreatmentDb item) {
        return PatientPriorTreatment.builder()
                .id(item.getId())
                .extid(item.getExtid())
                .patientId(item.getPatientId())
                .patientDiagnosisId(item.getPatientDiagnosisId())
                .cdk46Status(item.getCdk46Status())
                .endocrineStatus(item.getEndocrineStatus())
                .serdStatus(item.getSerdStatus())
                .chemoStatus(item.getChemoStatus())
                .her2TherapyStatus(item.getHer2TherapyStatus())
                .her2AdcStatus(item.getHer2AdcStatus())
                .trop2AdcStatus(item.getTrop2AdcStatus())
                .parpStatus(item.getParpStatus())
                .pi3kAktMtorStatus(item.getPi3kAktMtorStatus())
                .immunotherapyStatus(item.getImmunotherapyStatus())
                .taxaneStatus(item.getTaxaneStatus())
                .anthracyclineStatus(item.getAnthracyclineStatus())
                .platinumStatus(item.getPlatinumStatus())
                .currentDrugNames(item.getCurrentDrugNames())
                .priorDrugNames(item.getPriorDrugNames())
                .linesOfTherapyMetastatic(item.getLinesOfTherapyMetastatic())
                .hadNeoadjuvant(item.getHadNeoadjuvant())
                .hadAdjuvant(item.getHadAdjuvant())
                .hadRadiation(item.getHadRadiation())
                .hadSurgery(item.getHadSurgery())
                .lastTreatmentEndDate(item.getLastTreatmentEndDate())
                .currentlyOnTreatment(item.getCurrentlyOnTreatment())
                .otherTreatments(item.getOtherTreatments())
                .notes(item.getNotes())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .deletedAt(item.getDeletedAt())
                .active(item.getActive())
                .build();
    }

    public static PatientPriorTreatmentDb getPatientPriorTreatmentDb() {
        return getPatientPriorTreatmentDb(null, null);
    }

    public static PatientPriorTreatmentDb getPatientPriorTreatmentDb(Long patientId, String cdk46Status) {
        return getPatientPriorTreatmentDb(patientId, cdk46Status, null);
    }

    public static PatientPriorTreatmentDb getPatientPriorTreatmentDb(Long patientId, String cdk46Status, String extid) {
        PatientPriorTreatmentDb item = new PatientPriorTreatmentDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setPatientId(patientId != null ? patientId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setPatientDiagnosisId(ThreadLocalRandom.current().nextLong(1, 100000));
        item.setCdk46Status(cdk46Status != null ? cdk46Status : getTreatmentStatusRandom());
        item.setEndocrineStatus(getTreatmentStatusRandom());
        item.setSerdStatus(getTreatmentStatusRandom());
        item.setChemoStatus(getTreatmentStatusRandom());
        item.setHer2TherapyStatus(getTreatmentStatusRandom());
        item.setHer2AdcStatus(getTreatmentStatusRandom());
        item.setTrop2AdcStatus(getTreatmentStatusRandom());
        item.setParpStatus(getTreatmentStatusRandom());
        item.setPi3kAktMtorStatus(getTreatmentStatusRandom());
        item.setImmunotherapyStatus(getTreatmentStatusRandom());
        item.setTaxaneStatus(getTreatmentStatusRandom());
        item.setAnthracyclineStatus(getTreatmentStatusRandom());
        item.setPlatinumStatus(getTreatmentStatusRandom());
        item.setCurrentDrugNames(getDescriptionRandom("Cur_"));
        item.setPriorDrugNames(getDescriptionRandom("Pri_"));
        item.setLinesOfTherapyMetastatic(getIntegerRandom(0, 6));
        item.setHadNeoadjuvant(getBooleanRandom());
        item.setHadAdjuvant(getBooleanRandom());
        item.setHadRadiation(getBooleanRandom());
        item.setHadSurgery(getBooleanRandom());
        item.setLastTreatmentEndDate(getDateRandom());
        item.setCurrentlyOnTreatment(getBooleanRandom());
        item.setOtherTreatments(getDescriptionRandom("Oth_"));
        item.setNotes(getDescriptionRandom("Note_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // Patient
    public static Patient getPatient() {
        return getPatient(getPatientDb());
    }

    public static Patient getPatient(PatientDb item) {
        return Patient.builder()
                .id(item.getId())
                .extid(item.getExtid())
                .displayName(item.getDisplayName())
                .fullName(item.getFullName())
                .dateOfBirth(item.getDateOfBirth())
                .sex(item.getSex())
                .notes(item.getNotes())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .deletedAt(item.getDeletedAt())
                .active(item.getActive())
                .build();
    }

    public static PatientDb getPatientDb() {
        return getPatientDb(null, null);
    }

    public static PatientDb getPatientDb(String displayName, String sex) {
        return getPatientDb(displayName, sex, null);
    }

    public static PatientDb getPatientDb(String displayName, String sex, String extid) {
        PatientDb item = new PatientDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setDisplayName(displayName != null ? displayName : getNameRandom("Patient_"));
        item.setFullName(getNameRandom("Full_"));
        item.setDateOfBirth(getDateRandom());
        // getCodeRandom, not getNameRandom: sex is varchar(16) and a 32-char name overflows it.
        item.setSex(sex != null ? sex : getCodeRandom("S_"));
        item.setNotes(getDescriptionRandom("Note_"));
        setBaseSyncFields(item);
        return item;
    }

    // ///////////////////////////////////////////////////////////////////
    // UserPatient
    public static UserPatient getUserPatient() {
        return getUserPatient(getUserPatientDb());
    }

    public static UserPatient getUserPatient(UserPatientDb item) {
        return UserPatient.builder()
                .id(item.getId())
                .extid(item.getExtid())
                .userId(item.getUserId())
                .patientId(item.getPatientId())
                .accessLevel(item.getAccessLevel())
                .grantedByUserId(item.getGrantedByUserId())
                .grantedAt(item.getGrantedAt())
                .revokedAt(item.getRevokedAt())
                .note(item.getNote())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .deletedAt(item.getDeletedAt())
                .active(item.getActive())
                .build();
    }

    public static UserPatientDb getUserPatientDb() {
        return getUserPatientDb(null, null);
    }

    public static UserPatientDb getUserPatientDb(Long userId, Long patientId) {
        return getUserPatientDb(userId, patientId, null, null);
    }

    public static UserPatientDb getUserPatientDb(Long userId, Long patientId,
                                                 AccessLevel accessLevel, String extid) {
        UserPatientDb item = new UserPatientDb();
        item.setExtid(extid != null ? extid : UUID.randomUUID().toString());
        item.setUserId(userId != null ? userId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setPatientId(patientId != null ? patientId : ThreadLocalRandom.current().nextLong(1, 100000));
        item.setAccessLevel(accessLevel != null ? accessLevel : AccessLevel.VIEW_RECORD);
        item.setGrantedByUserId(ThreadLocalRandom.current().nextLong(1, 100000));
        item.setGrantedAt(LocalDateTime.now());
        // Active by default: revokedAt stays null unless a test sets it deliberately.
        item.setRevokedAt(null);
        item.setNote(getDescriptionRandom("Note_"));
        setBaseSyncFields(item);
        return item;
    }

}
