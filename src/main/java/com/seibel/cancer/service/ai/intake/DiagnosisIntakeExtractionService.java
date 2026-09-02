package com.seibel.cancer.service.ai.intake;

import com.seibel.cancer.service.ai.AiGenerationException;
import com.seibel.cancer.service.ai.AiService;
import com.seibel.cancer.service.ai.PhiDetectedException;
import com.seibel.cancer.service.ai.PhiHeuristicScanner;
import com.seibel.cancer.service.ai.PhiScanResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates one document-intake conversation: the PHI gate, the extraction call, and the
 * clarifying-turn call.
 *
 * <p><b>The control here is gate-then-send, not {@code TrialDiagnosisMatchService}'s
 * allowlist-then-send.</b> That service reconstructs a payload from the app's own already-clean
 * domain fields, field by field, so an allowlist is the natural control. Here the payload
 * <em>is</em> the raw text a user just pasted in - there is no set of fields to allow, so the
 * control has to be a scan of the content itself, run to completion before {@link AiService} is
 * ever touched. See {@link PhiHeuristicScanner}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisIntakeExtractionService {

    private static final String EXTRACT_SYSTEM_PROMPT_PATH =
            "prompts/diagnosis-intake-extract-system.txt";
    private static final String CLARIFY_SYSTEM_PROMPT_PATH =
            "prompts/diagnosis-intake-clarify-system.txt";

    private static final List<String> REQUIRED_FIELDS = List.of(
            "cancerType", "stage", "erStatus", "prStatus", "her2Status", "ecogStatus");

    private final AiService aiService;
    private final PhiHeuristicScanner phiHeuristicScanner;

    /** Loaded once each. A prompt change needs a restart, which is the right cadence for it. */
    private volatile String extractSystemPrompt;
    private volatile String clarifySystemPrompt;

    /**
     * Gate-then-extract. The scanner runs unconditionally first; on a flagged result this
     * throws before {@link AiService} is touched at all, and the raw text is discarded with it -
     * never logged, never carried into the exception message.
     */
    public DiagnosisIntakeExtraction extract(String documentText) {
        PhiScanResult scan = phiHeuristicScanner.scan(documentText);
        if (scan.flagged()) {
            log.info("Diagnosis intake upload rejected by PHI gate: categories={}",
                    scan.reasons());
            throw new PhiDetectedException(
                    "This document appears to contain identifying information (such as a name, "
                            + "date of birth, contact details, or a medical record number). "
                            + "Please remove any identifying details and re-upload or re-paste "
                            + "the text.");
        }

        String userPrompt = "## Uploaded document\n\n" + documentText.strip()
                + "\n\nExtract every field you can from this document into the given shape. "
                + "Leave a field null if the document does not state it - do not guess.";

        log.info("Diagnosis intake extraction: document chars={}", documentText.length());
        return aiService.generateStructured(
                loadExtractSystemPrompt(), userPrompt, DiagnosisIntakeExtraction.class);
    }

    /**
     * One clarifying turn. No PHI gate here - the input is the user's own short answer to a
     * question this service asked, already once removed from the original document, not a new
     * document upload.
     */
    public DiagnosisIntakeClarification clarify(DiagnosisIntakeExtraction draftSoFar,
                                                List<String> missingFields,
                                                String latestQuestion,
                                                String userAnswer) {
        String userPrompt = buildClarifyPrompt(draftSoFar, missingFields, latestQuestion, userAnswer);
        return aiService.generateStructured(
                loadClarifySystemPrompt(), userPrompt, DiagnosisIntakeClarification.class);
    }

    /** Pure Java, not AI - the "keep asking or stop" decision must be deterministic. */
    public List<String> missingRequired(DiagnosisIntakeExtraction draft) {
        List<String> missing = new ArrayList<>();
        if (isBlank(draft.getCancerType())) missing.add("cancerType");
        if (isBlank(draft.getStage())) missing.add("stage");
        if (isUnknownOrBlank(draft.getErStatus())) missing.add("erStatus");
        if (isUnknownOrBlank(draft.getPrStatus())) missing.add("prStatus");
        if (isUnknownOrBlank(draft.getHer2Status())) missing.add("her2Status");
        if (draft.getEcogStatus() == null) missing.add("ecogStatus");
        return missing;
    }

    public List<String> requiredFields() {
        return REQUIRED_FIELDS;
    }

    private String buildClarifyPrompt(DiagnosisIntakeExtraction draft, List<String> missingFields,
                                      String latestQuestion, String userAnswer) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Draft so far\n\n");
        appendField(sb, "Cancer type", draft.getCancerType());
        appendField(sb, "Stage", draft.getStage());
        appendField(sb, "Staging system", draft.getStageSystem());
        appendField(sb, "Metastatic", draft.getIsMetastatic());
        appendField(sb, "Metastasis sites", draft.getMetastasisSites());
        appendField(sb, "ER status", draft.getErStatus());
        appendField(sb, "PR status", draft.getPrStatus());
        appendField(sb, "HER2 status", draft.getHer2Status());
        appendField(sb, "Biomarkers", draft.getBiomarkers());
        appendField(sb, "ECOG status", draft.getEcogStatus());
        appendField(sb, "Prior chemo regimens", draft.getPriorChemoRegimens());
        appendField(sb, "Menopausal status", draft.getMenopausalStatus());

        sb.append("\n## Still missing\n\n").append(String.join(", ", missingFields)).append("\n");
        sb.append("\n## The question that was asked\n\n").append(latestQuestion).append("\n");
        sb.append("\n## The user's answer\n\n").append(userAnswer).append("\n");
        sb.append("\nMerge the answer into the draft and return the whole updated draft. ");
        sb.append("If any required field is still unanswered, ask one clear follow-up question. ");
        sb.append("If all required fields are resolved (the user may explicitly decline to "
                + "answer one), set done=true and leave nextQuestion blank.\n");
        return sb.toString();
    }

    private String loadExtractSystemPrompt() {
        if (extractSystemPrompt == null) {
            synchronized (this) {
                if (extractSystemPrompt == null) {
                    extractSystemPrompt = loadPrompt(EXTRACT_SYSTEM_PROMPT_PATH);
                }
            }
        }
        return extractSystemPrompt;
    }

    private String loadClarifySystemPrompt() {
        if (clarifySystemPrompt == null) {
            synchronized (this) {
                if (clarifySystemPrompt == null) {
                    clarifySystemPrompt = loadPrompt(CLARIFY_SYSTEM_PROMPT_PATH);
                }
            }
        }
        return clarifySystemPrompt;
    }

    private String loadPrompt(String path) {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AiGenerationException(
                    "The AI instructions could not be loaded from " + path, e);
        }
    }

    /** A field is written only when present, so an absent one contributes nothing to read. */
    private void appendField(StringBuilder sb, String label, Object value) {
        if (value == null) return;
        String text = String.valueOf(value).strip();
        if (text.isEmpty()) return;
        sb.append("- **").append(label).append("**: ").append(text).append('\n');
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean isUnknownOrBlank(String s) {
        return isBlank(s) || "UNKNOWN".equalsIgnoreCase(s.trim());
    }
}
