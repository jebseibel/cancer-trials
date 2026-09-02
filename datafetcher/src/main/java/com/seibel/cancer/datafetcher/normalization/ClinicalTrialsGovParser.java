package com.seibel.cancer.datafetcher.normalization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seibel.cancer.common.domain.ArmGroup;
import com.seibel.cancer.common.domain.Intervention;
import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.common.domain.Outcome;
import com.seibel.cancer.common.domain.OverallOfficial;
import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.util.TrialTextClassifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.seibel.cancer.datafetcher.clinicaltrials.ClinicalTrialsGovIngestJob.TRIAL_SOURCE_CODE;

/**
 * Parses ClinicalTrials.gov v2's per-study JSON shape into a NormalizedTrial, per the
 * field-mapping table in clinical-trials-tables.md.
 */
@Component
public class ClinicalTrialsGovParser implements TrialSourceParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(String trialSourceCode) {
        return TRIAL_SOURCE_CODE.equals(trialSourceCode);
    }

    @Override
    public NormalizedTrial parse(String rawPayloadJson) {
        JsonNode study = readTree(rawPayloadJson);
        JsonNode protocolSection = study.path("protocolSection");
        JsonNode identification = protocolSection.path("identificationModule");
        JsonNode status = protocolSection.path("statusModule");
        JsonNode description = protocolSection.path("descriptionModule");
        JsonNode design = protocolSection.path("designModule");
        JsonNode eligibility = protocolSection.path("eligibilityModule");
        JsonNode armsInterventions = protocolSection.path("armsInterventionsModule");
        JsonNode outcomesModule = protocolSection.path("outcomesModule");
        JsonNode sponsorCollaborators = protocolSection.path("sponsorCollaboratorsModule");
        JsonNode contactsLocations = protocolSection.path("contactsLocationsModule");
        JsonNode conditionsModule = protocolSection.path("conditionsModule");

        Trial trial = Trial.builder()
                .nctId(text(identification, "nctId"))
                .briefTitle(text(identification, "briefTitle"))
                .officialTitle(text(identification, "officialTitle"))
                .overallStatus(text(status, "overallStatus"))
                .studyType(text(design, "studyType"))
                .briefSummary(text(description, "briefSummary"))
                .detailedDescription(text(description, "detailedDescription"))
                .startDate(dateStruct(status, "startDateStruct"))
                .primaryCompletionDate(dateStruct(status, "primaryCompletionDateStruct"))
                .completionDate(dateStruct(status, "completionDateStruct"))
                .lastUpdatePostedDate(dateStruct(status, "lastUpdatePostDateStruct"))
                .enrollmentCount(intValue(design.path("enrollmentInfo"), "count"))
                .enrollmentType(text(design.path("enrollmentInfo"), "type"))
                .healthyVolunteers(boolValue(eligibility, "healthyVolunteers"))
                .sex(text(eligibility, "sex"))
                .minimumAge(text(eligibility, "minimumAge"))
                .maximumAge(text(eligibility, "maximumAge"))
                .eligibilityCriteria(text(eligibility, "eligibilityCriteria"))
                .build();

        // What the trial is trying to achieve, inferred from its own words - CT.gov publishes
        // no field for it. Stamped here so curative-intent trials can be queried directly
        // rather than only surfacing inside a ranking run; they are ~1.5% of the corpus and
        // finding them is the point of the tool.
        //
        // Read from title, summary and description, never the eligibility criteria: criteria
        // describe a patient's treatment history, where "curative intent" means therapy someone
        // already had, which inverts the meaning.
        //
        // A cached inference. Change the patterns and every stored value is stale, so re-derive
        // by re-normalizing rather than trusting an old one.
        String describable = joinForGoal(trial.getBriefTitle(), trial.getOfficialTitle(),
                trial.getBriefSummary(), trial.getDetailedDescription());
        trial.setTreatmentGoal(TrialTextClassifier.classify(describable).name());
        // The other half: what a trial is trying to do and who it is for are different
        // questions that can disagree, so they are stored separately.
        trial.setDiseaseStage(TrialTextClassifier.classifyStage(describable).name());

        List<ArmGroup> armGroups = new ArrayList<>();
        for (JsonNode node : armsInterventions.path("armGroups")) {
            armGroups.add(ArmGroup.builder()
                    .label(text(node, "label"))
                    .type(text(node, "type"))
                    .description(text(node, "description"))
                    .build());
        }

        List<Intervention> interventions = new ArrayList<>();
        for (JsonNode node : armsInterventions.path("interventions")) {
            interventions.add(Intervention.builder()
                    .type(text(node, "type"))
                    .name(text(node, "name"))
                    .description(text(node, "description"))
                    .build());
        }

        List<Outcome> outcomes = new ArrayList<>();
        for (JsonNode node : outcomesModule.path("primaryOutcomes")) {
            outcomes.add(outcome(node, "PRIMARY"));
        }
        for (JsonNode node : outcomesModule.path("secondaryOutcomes")) {
            outcomes.add(outcome(node, "SECONDARY"));
        }

        List<Location> locations = new ArrayList<>();
        for (JsonNode node : contactsLocations.path("locations")) {
            locations.add(Location.builder()
                    .facility(text(node, "facility"))
                    .city(text(node, "city"))
                    .state(text(node, "state"))
                    .zip(text(node, "zip"))
                    .country(text(node, "country"))
                    .status(text(node, "status"))
                    .latitude(decimalValue(node, "geoPoint", "lat"))
                    .longitude(decimalValue(node, "geoPoint", "lon"))
                    .build());
        }

        List<OverallOfficial> overallOfficials = new ArrayList<>();
        for (JsonNode node : contactsLocations.path("overallOfficials")) {
            overallOfficials.add(OverallOfficial.builder()
                    .name(text(node, "name"))
                    .affiliation(text(node, "affiliation"))
                    .role(text(node, "role"))
                    .build());
        }

        List<String> conditionNames = new ArrayList<>();
        for (JsonNode node : conditionsModule.path("conditions")) {
            conditionNames.add(node.asText());
        }

        List<NormalizedTrial.NormalizedSponsor> sponsors = new ArrayList<>();
        JsonNode leadSponsor = sponsorCollaborators.path("leadSponsor");
        if (!leadSponsor.isMissingNode()) {
            sponsors.add(NormalizedTrial.NormalizedSponsor.builder()
                    .name(text(leadSponsor, "name"))
                    .orgClass(text(leadSponsor, "class"))
                    .build());
        }
        for (JsonNode node : sponsorCollaborators.path("collaborators")) {
            sponsors.add(NormalizedTrial.NormalizedSponsor.builder()
                    .name(text(node, "name"))
                    .orgClass(text(node, "class"))
                    .build());
        }

        return NormalizedTrial.builder()
                .trial(trial)
                .locations(locations)
                .armGroups(armGroups)
                .interventions(interventions)
                .outcomes(outcomes)
                .overallOfficials(overallOfficials)
                .conditionNames(conditionNames)
                .sponsors(sponsors)
                .build();
    }

    private Outcome outcome(JsonNode node, String outcomeType) {
        return Outcome.builder()
                .outcomeType(outcomeType)
                .measure(text(node, "measure"))
                .description(text(node, "description"))
                .timeFrame(text(node, "timeFrame"))
                .build();
    }

    private JsonNode readTree(String rawPayloadJson) {
        try {
            return objectMapper.readTree(rawPayloadJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse ClinicalTrials.gov payload JSON", e);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Integer intValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private Boolean boolValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asBoolean();
    }

    private BigDecimal decimalValue(JsonNode node, String parentField, String field) {
        JsonNode parent = node.path(parentField);
        JsonNode value = parent.path(field);
        return value.isMissingNode() || value.isNull() ? null : new BigDecimal(value.asText());
    }

    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * CT.gov date structs sometimes give only year-month precision (e.g. "2024-03"
     * instead of "2024-03-15") when the day isn't known - default to the 1st in that
     * case rather than dropping the date entirely.
     */
    private LocalDate dateStruct(JsonNode node, String structField) {
        String raw = text(node.path(structField), "date");
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw, FULL_DATE);
        } catch (DateTimeParseException fullDateFailed) {
            try {
                return java.time.YearMonth.parse(raw, YEAR_MONTH).atDay(1);
            } catch (DateTimeParseException yearMonthFailed) {
                return null;
            }
        }
    }

    /** The trial's own descriptive text, for treatment-goal classification. Nulls skipped. */
    private static String joinForGoal(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                sb.append(p).append(' ');
            }
        }
        return sb.toString().strip();
    }
}
