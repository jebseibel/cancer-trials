package com.seibel.cancer.service.ai.intake;

import com.seibel.cancer.service.ai.AiGenerationException;
import com.seibel.cancer.service.ai.AiService;
import com.seibel.cancer.service.ai.PhiHeuristicScanner;
import com.seibel.cancer.service.ai.PhiLineScanResult;
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
 * Orchestrates one document-intake conversation: the PHI line scan, the extraction call, and the
 * clarifying-turn call.
 *
 * <p><b>The control here is scrub-then-send, not {@code TrialDiagnosisMatchService}'s
 * allowlist-then-send.</b> That service reconstructs a payload from the app's own already-clean
 * domain fields, field by field, so an allowlist is the natural control. Here the payload
 * <em>is</em> the raw text a user just pasted in - there is no set of fields to allow, so the
 * control is a per-line scan of the content itself: every flagged line is cut before
 * {@link AiService} is ever touched, and the document that survives is what gets sent, not the
 * original. See {@link PhiHeuristicScanner#scanLines}.
 *
 * <p><b>Not whole-document gate-then-send, on purpose - superseded 2026-09-03.</b> The original
 * design rejected the entire document on a single flagged line anywhere in it. That was safer in
 * one sense (nothing partial ever reached the model) but failed in practice against this app's
 * own multi-field record export: a false positive on one line - twice found and fixed in the
 * underlying patterns already, a third time the same day this changed - made the whole document,
 * including a dozen genuinely clean lines, unusable, with no way to recover anything short of
 * editing the pasted text by hand and guessing which line offended. Per-line scrubbing keeps the
 * scanner's same per-line precision/recall trade (biased to over-flag) while no longer letting
 * one bad line cost the whole upload.
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
     * Scrub-then-extract. The scanner runs unconditionally first and removes every flagged line;
     * extraction (if it runs at all) only ever sees the survivors, and the excluded lines are
     * reported back by number and category - never by content, same rule as the scanner itself.
     *
     * <p>If nothing survives the scrub, {@link AiService} is never called at all - there is
     * nothing left to extract from, and no reason to spend a paid call finding that out.
     */
    public DiagnosisIntakeUpload extract(String documentText) {
        PhiLineScanResult scan = phiHeuristicScanner.scanLines(documentText);
        if (scan.anyExcluded()) {
            log.info("Diagnosis intake upload: {} line(s) excluded by PHI line scan, categories={}",
                    scan.excludedLines().size(),
                    scan.excludedLines().stream()
                            .flatMap(l -> l.reasons().stream())
                            .distinct()
                            .toList());
        }

        if (scan.cleanedText() == null || scan.cleanedText().isBlank()) {
            log.info("Diagnosis intake upload: nothing survived the PHI line scan, skipping AI call");
            return new DiagnosisIntakeUpload(new DiagnosisIntakeExtraction(), scan.excludedLines());
        }

        String userPrompt = "## Uploaded document\n\n" + scan.cleanedText().strip()
                + "\n\nExtract every field you can from this document into the given shape. "
                + "Leave a field null if the document does not state it - do not guess. Some "
                + "lines may have been removed before this document reached you; do not treat a "
                + "gap in the narrative as meaningful, and do not guess at what a removed line "
                + "might have said.";

        log.info("Diagnosis intake extraction: document chars={}", scan.cleanedText().length());
        DiagnosisIntakeExtraction draft = aiService.generateStructured(
                loadExtractSystemPrompt(), userPrompt, DiagnosisIntakeExtraction.class);
        return new DiagnosisIntakeUpload(draft, scan.excludedLines());
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
