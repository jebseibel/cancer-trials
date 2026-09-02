package com.seibel.cancer.service.ai;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.database.db.service.TrialDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Rewrites one trial's scientific title into a plain-language, four-part summary a
 * non-technical reader can scan: cancer stage, treatment goal, what the trial is trying, and
 * what markers or mutations it needs.
 *
 * <p><b>Why AI rather than a pattern, unlike {@code treatment_goal}/{@code disease_stage}.</b>
 * Those two columns are a closed classification - three or four fixed values a regex can
 * reliably assign. A friendly title is a rewrite: it has to compress a trial's own prose into a
 * few new words, which is a judgment call a keyword rule cannot make.
 *
 * <p><b>Trial-only. No patient data is read or sent.</b> Unlike {@link TrialDiagnosisMatchService}
 * this is not the AI trial check - it never touches {@code patient_diagnosis} or any other
 * patient table, so none of that service's de-identification allowlist applies here. What is
 * sent is exactly what {@code trial.eligibility_criteria} and its sibling columns already hold,
 * which ClinicalTrials.gov publishes for anyone to read.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialFriendlyTitleService {

    private static final String SYSTEM_PROMPT_PATH = "prompts/friendly-title-system.txt";

    private final AiService aiService;
    private final TrialDbService trialDbService;

    /** Loaded once. A prompt change needs a restart, which is the right cadence for it. */
    private volatile String systemPrompt;

    /**
     * Generates a friendly title for one trial and stores it, always overwriting whatever is
     * there - a deliberate single press, the same contract as the AI trial check's "Check
     * again".
     *
     * @throws AiGenerationException when AI is unconfigured, the call fails, or the trial has no
     *                                text to work from
     */
    public Trial generate(Trial trial) {
        if (trial == null) {
            throw new AiGenerationException("No trial to generate a friendly title for.");
        }
        if (isBlank(trial.getBriefTitle()) && isBlank(trial.getOfficialTitle())) {
            throw new AiGenerationException(
                    "This trial has no title recorded, so there is nothing to rewrite.");
        }

        String userPrompt = buildPrompt(trial);
        // DEBUG: the corpus-wide backfill calls this once per trial with its own ProgressTicker
        // running, and an INFO line here fires mid-record and shreds the bar. The bar's own
        // glyph plus gutter already shows which trial is current; nothing is lost.
        log.debug("Friendly title generation: nctId={}", trial.getNctId());
        TrialFriendlyTitleAssessment assessment = aiService.generateStructured(
                loadSystemPrompt(), userPrompt, TrialFriendlyTitleAssessment.class);

        String friendlyTitle = assessment.toFriendlyTitle();

        Trial patch = new Trial();
        patch.setFriendlyTitle(friendlyTitle);
        return trialDbService.update(trial.getExtid(), patch);
    }

    private String buildPrompt(Trial trial) {
        StringBuilder sb = new StringBuilder();
        sb.append("## The trial\n\n");
        appendField(sb, "Title", trial.getBriefTitle());
        appendField(sb, "Official title", trial.getOfficialTitle());
        appendField(sb, "Summary", trial.getBriefSummary());
        appendField(sb, "Description", trial.getDetailedDescription());
        // Already-classified columns, reused rather than re-derived - the deterministic
        // TrialTextClassifier already answered "curative or not" for this same trial, and
        // disagreeing with it here would leave two conflicting answers on the same page.
        appendField(sb, "Known treatment goal classification", trial.getTreatmentGoal());
        appendField(sb, "Known disease stage classification", trial.getDiseaseStage());

        if (!isBlank(trial.getEligibilityCriteria())) {
            sb.append("\n### Eligibility criteria, verbatim\n\n")
                    .append(trial.getEligibilityCriteria().strip())
                    .append("\n");
        }

        sb.append("\nWrite the four-part friendly title now.\n");
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
                        throw new AiGenerationException(
                                "The AI instructions could not be loaded from " + SYSTEM_PROMPT_PATH, e);
                    }
                }
            }
        }
        return systemPrompt;
    }

    private void appendField(StringBuilder sb, String label, String value) {
        if (isBlank(value)) {
            return;
        }
        sb.append("- **").append(label).append("**: ").append(value.strip()).append('\n');
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
