package com.seibel.cancer.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seibel.cancer.common.domain.AiTrialAssessment;
import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.database.db.service.AiTrialAssessmentDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * Reads one trial's criteria against one patient's record, using a model.
 *
 * <p><b>Why this exists alongside the deterministic signals.</b> {@code CriteriaSignalEvaluator}
 * answers seven specific questions with patterns, and is right about them in a way a model
 * cannot be relied on to be. What it cannot do is read a criterion nobody anticipated — a
 * carve-out inside an exclusion, an unusual phrasing, a requirement that only makes sense in
 * context. Roughly 15% of CDK4/6 exclusions in this corpus contain a permission rather than a
 * bar, and no keyword rule reaches them. That gap is what this fills.
 *
 * <p><b>It cannot report eligibility.</b> {@link TrialMatchAssessment} has no such field, and the
 * system prompt says so explicitly. The model can find something that rules the patient out —
 * a checkable claim backed by a quotation — but absence of an exclusion is reported as absence,
 * never as a match. Same rule the rest of this application follows, enforced here by the shape of
 * the response type rather than by hoping the prompt holds.
 *
 * <h2>What leaves the machine</h2>
 *
 * <p>This is the only place in the application where clinical text is sent anywhere. Embeddings
 * run locally in ONNX precisely so trial and patient text stays here, and this is a deliberate
 * exception rather than a drift.
 *
 * <p><b>The payload is built from an explicit allowlist, never a serialized object.</b> That
 * direction matters: adding a column to {@code patient_diagnosis} must not silently start
 * sending it. Two exclusions are deliberate:
 *
 * <ul>
 *   <li><b>{@code notes} is never sent.</b> It is free text, and free text cannot be guaranteed
 *       identifier-free — it already holds a Ki-67 discrepancy and a drug-date conflict, and a
 *       clinician's name could land there tomorrow.
 *   <li><b>Dates are coarsened to a year.</b> A date more precise than a year is one of HIPAA's
 *       eighteen identifiers, and a year is enough for every criterion that turns on timing.
 * </ul>
 *
 * <p>No name, no date of birth, no external identifier is included. Those live on {@code Patient}
 * and this service never reads it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialDiagnosisMatchService {

    private static final String SYSTEM_PROMPT_PATH = "prompts/trial-match-system.txt";

    private final AiService aiService;
    private final AiTrialAssessmentDbService assessmentDbService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Loaded once. A prompt change needs a restart, which is the right cadence for it. */
    private volatile String systemPrompt;

    /**
     * Assesses one trial against one patient's record.
     *
     * @param trial     the trial, whose criteria and description are read
     * @param patientId whose record this was read against, for the stored row
     * @param diagnosis the patient's diagnosis; may be null, in which case there is nothing to
     *                  compare against and the caller is told so
     * @param variant   the genomic panel row, or null
     * @param treatment prior-treatment record, or null
     * @throws AiGenerationException when AI is unconfigured or the call fails
     */
    public TrialMatchAssessment assess(Trial trial,
                                       Long patientId,
                                       PatientDiagnosis diagnosis,
                                       PatientVariant variant,
                                       PatientPriorTreatment treatment) {
        if (trial == null) {
            throw new AiGenerationException("No trial to assess.");
        }
        if (diagnosis == null) {
            throw new AiGenerationException(
                    "There is no diagnosis on file to compare this trial against. Fill in the "
                            + "Diagnosis tab first.");
        }
        if (isBlank(trial.getEligibilityCriteria())) {
            throw new AiGenerationException(
                    "This trial has no eligibility criteria recorded, so there is nothing to "
                            + "read against the record.");
        }

        String userPrompt = buildPrompt(trial, diagnosis, variant, treatment);
        log.info("AI trial check: nctId={}", trial.getNctId());
        TrialMatchAssessment assessment = aiService.generateStructured(
                loadSystemPrompt(), userPrompt, TrialMatchAssessment.class);

        persist(trial, diagnosis, patientId, assessment);
        return assessment;
    }

    /**
     * Records what was said, and what it was said about.
     *
     * <p>A failure here must not lose the answer. The reading already cost money and the reader
     * is waiting for it, so a storage problem is logged and the assessment is still returned —
     * losing the row is bad, throwing away a paid-for answer the reader can see no reason for is
     * worse.
     */
    private void persist(Trial trial, PatientDiagnosis diagnosis, Long patientId,
                         TrialMatchAssessment assessment) {
        if (patientId == null || trial.getId() == null) {
            return;
        }
        try {
            assessmentDbService.create(AiTrialAssessment.builder()
                    .trialId(trial.getId())
                    .patientId(patientId)
                    .rulesPatientOut(assessment.getRulesPatientOut())
                    .exclusionCriterion(assessment.getExclusionCriterion())
                    .summary(assessment.getSummary())
                    .openQuestions(toJson(assessment.getOpenQuestions()))
                    .concerns(toJson(assessment.getConcerns()))
                    .criteriaMet(toJson(assessment.getCriteriaSheAppearsToMeet()))
                    .model(aiService.getModelName())
                    .promptHash(promptHash())
                    // Snapshot: patient_diagnosis is one row updated in place, so without this
                    // a stored reading has no record of what it was reading.
                    .snapshotStage(diagnosis.getStage())
                    .snapshotErStatus(diagnosis.getErStatus())
                    .snapshotPrStatus(diagnosis.getPrStatus())
                    .snapshotHer2Status(diagnosis.getHer2Status())
                    .assessedAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Could not store the AI assessment for nctId={} - returning it anyway",
                    trial.getNctId(), e);
        }
    }

    private String toJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialise a list for storage: {}", e.getMessage());
            return null;
        }
    }

    /**
     * A hash of the instructions this reading used.
     *
     * <p>Two runs months apart may differ because the prompt changed rather than because
     * anything clinical did, and without this there is no way to tell those apart.
     */
    private String promptHash() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(loadSystemPrompt().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Builds the request.
     *
     * <p>Section headers rather than JSON: the model reads prose better than a serialized object,
     * and a human reviewing what was sent can check it at a glance — which matters when the thing
     * being sent is a medical record.
     */
    private String buildPrompt(Trial trial,
                               PatientDiagnosis diagnosis,
                               PatientVariant variant,
                               PatientPriorTreatment treatment) {
        StringBuilder sb = new StringBuilder();

        sb.append("## The trial\n\n");
        appendField(sb, "Title", trial.getBriefTitle());
        appendField(sb, "Official title", trial.getOfficialTitle());
        appendField(sb, "Status", trial.getOverallStatus());
        appendField(sb, "Summary", trial.getBriefSummary());
        appendField(sb, "Accepts", trial.getSex());
        appendField(sb, "Age range", ageRange(trial));

        sb.append("\n### Eligibility criteria, verbatim\n\n")
                .append(trial.getEligibilityCriteria().strip())
                .append("\n");

        sb.append("\n## The patient's record\n\n");
        sb.append("This is a de-identified summary. Where a field is absent, the record does not "
                + "state it — do not assume a value.\n\n");

        appendField(sb, "Cancer type", diagnosis.getCancerType());
        appendField(sb, "Stage", diagnosis.getStage());
        appendField(sb, "Staging system", diagnosis.getStageSystem());
        appendField(sb, "Has spread", yesNo(diagnosis.getIsMetastatic()));
        appendField(sb, "Sites of spread", diagnosis.getMetastasisSites());
        appendField(sb, "Receptor subtype", diagnosis.getReceptorSubtype());
        appendField(sb, "ER status", diagnosis.getErStatus());
        appendField(sb, "PR status", diagnosis.getPrStatus());
        appendField(sb, "HER2 status", diagnosis.getHer2Status());
        appendField(sb, "Biomarkers", diagnosis.getBiomarkers());
        appendField(sb, "ECOG performance status", diagnosis.getEcogStatus());
        appendField(sb, "Menopausal status", diagnosis.getMenopausalStatus());
        appendField(sb, "Measurable disease", yesNo(diagnosis.getHasMeasurableDisease()));
        appendField(sb, "Prior chemotherapy regimens", diagnosis.getPriorChemoRegimens());
        appendField(sb, "Prior treatments", diagnosis.getPriorTreatments());
        // Year only. A more precise date is a HIPAA identifier and adds nothing a criterion needs.
        appendField(sb, "Year of diagnosis", year(diagnosis.getDiagnosisDate()));
        appendField(sb, "Year chemotherapy ended", year(diagnosis.getLastChemoEndDate()));
        // notes is deliberately absent - see the class javadoc.

        if (variant != null) {
            sb.append("\n### Genomic findings\n\n");
            sb.append("Each is one of DETECTED, NOT_DETECTED, VUS, NOT_TESTED or UNKNOWN. "
                    + "NOT_TESTED and NOT_DETECTED are different answers and must not be "
                    + "treated alike.\n\n");
            appendField(sb, "PIK3CA", variant.getPik3caStatus());
            appendField(sb, "ESR1", variant.getEsr1Status());
            appendField(sb, "TP53", variant.getTp53Status());
            appendField(sb, "AKT1", variant.getAkt1Status());
            appendField(sb, "PTEN", variant.getPtenStatus());
            appendField(sb, "ERBB2 (somatic)", variant.getErbb2SomaticStatus());
            appendField(sb, "BRCA1", variant.getBrca1Status());
            appendField(sb, "BRCA2", variant.getBrca2Status());
            appendField(sb, "PALB2", variant.getPalb2Status());
            appendField(sb, "ATM", variant.getAtmStatus());
            appendField(sb, "CHEK2", variant.getChek2Status());
            appendField(sb, "HRD", variant.getHrdStatus());
            appendField(sb, "PD-L1", variant.getPdl1Status());
            appendField(sb, "Ki-67 percent", variant.getKi67Percent());
            appendField(sb, "Germline testing done", variant.getGermlineTestDone());
            appendField(sb, "Somatic testing done", variant.getSomaticTestDone());
            appendField(sb, "Year of testing", year(variant.getTestDate()));
            appendField(sb, "Other variants", variant.getOtherVariants());
            // testLab names an institution and notes is free text - neither is sent.
        }

        if (treatment != null) {
            sb.append("\n### Prior treatment\n\n");
            sb.append("Each is one of NEVER, CURRENT, PROGRESSED, STOPPED_OTHER or UNKNOWN. "
                    + "CURRENT and PROGRESSED are different situations for a trial that asks "
                    + "about prior therapy.\n\n");
            appendField(sb, "Endocrine therapy", treatment.getEndocrineStatus());
            appendField(sb, "SERD", treatment.getSerdStatus());
            appendField(sb, "CDK4/6 inhibitor", treatment.getCdk46Status());
            appendField(sb, "PI3K/AKT/mTOR inhibitor", treatment.getPi3kAktMtorStatus());
            appendField(sb, "Chemotherapy", treatment.getChemoStatus());
            appendField(sb, "Taxane", treatment.getTaxaneStatus());
            appendField(sb, "Anthracycline", treatment.getAnthracyclineStatus());
            appendField(sb, "Platinum", treatment.getPlatinumStatus());
            appendField(sb, "HER2-targeted therapy", treatment.getHer2TherapyStatus());
            appendField(sb, "HER2 antibody-drug conjugate", treatment.getHer2AdcStatus());
            appendField(sb, "TROP2 antibody-drug conjugate", treatment.getTrop2AdcStatus());
            appendField(sb, "PARP inhibitor", treatment.getParpStatus());
            appendField(sb, "Immunotherapy", treatment.getImmunotherapyStatus());
            appendField(sb, "Currently on treatment", yesNo(treatment.getCurrentlyOnTreatment()));
            appendField(sb, "Current drugs", treatment.getCurrentDrugNames());
            appendField(sb, "Prior drugs", treatment.getPriorDrugNames());
            appendField(sb, "Lines of therapy for metastatic disease",
                    treatment.getLinesOfTherapyMetastatic());
            appendField(sb, "Had neoadjuvant therapy", yesNo(treatment.getHadNeoadjuvant()));
            appendField(sb, "Had adjuvant therapy", yesNo(treatment.getHadAdjuvant()));
            appendField(sb, "Had radiation", yesNo(treatment.getHadRadiation()));
            appendField(sb, "Had surgery", yesNo(treatment.getHadSurgery()));
            appendField(sb, "Year treatment last ended", year(treatment.getLastTreatmentEndDate()));
            appendField(sb, "Other treatments", treatment.getOtherTreatments());
            // notes is free text and is not sent.
        }

        sb.append("\nRead the criteria against this record and report what you find.\n");
        return sb.toString();
    }

    private String loadSystemPrompt() {
        if (systemPrompt == null) {
            synchronized (this) {
                if (systemPrompt == null) {
                    try {
                        systemPrompt = StreamUtils.copyToString(
                                new ClassPathResource(SYSTEM_PROMPT_PATH).getInputStream(),
                                StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        // A missing prompt is a packaging fault, not an AI failure - but it
                        // still must not read as the application being broken.
                        throw new AiGenerationException(
                                "The AI instructions could not be loaded from " + SYSTEM_PROMPT_PATH, e);
                    }
                }
            }
        }
        return systemPrompt;
    }

    /** A field is written only when present, so an absent one contributes nothing to read. */
    private void appendField(StringBuilder sb, String label, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).strip();
        if (text.isEmpty()) {
            return;
        }
        sb.append("- **").append(label).append("**: ").append(text).append('\n');
    }

    /** Year only, never the full date. */
    private String year(LocalDate date) {
        return date == null ? null : String.valueOf(date.getYear());
    }

    /** Spelled out, because "true" reads as a data type and "yes" reads as an answer. */
    private String yesNo(Boolean value) {
        return value == null ? null : (value ? "yes" : "no");
    }

    private String ageRange(Trial trial) {
        String min = trial.getMinimumAge();
        String max = trial.getMaximumAge();
        if (isBlank(min) && isBlank(max)) {
            return null;
        }
        return (isBlank(min) ? "any" : min.strip()) + " to " + (isBlank(max) ? "any" : max.strip());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
