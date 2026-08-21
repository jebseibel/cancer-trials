package com.seibel.cancer.service.matching;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.enums.TreatmentGoal;
import com.seibel.cancer.common.util.TreatmentGoalClassifier;
import com.seibel.cancer.database.db.service.TrialDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Re-derives {@code trial.treatment_goal} for trials already in the database.
 *
 * <p><b>Why this exists rather than relying on ingestion.</b> The value is stamped at
 * normalization, and ingestion skips trials whose {@code payload_hash} is unchanged — correctly,
 * since re-normalizing costs ~349ms and 99% of a run for no new data. So a re-pull will not
 * populate this column for trials already loaded: CT.gov's payload has not changed, only the
 * code reading it has.
 *
 * <p><b>And the code reading it will keep changing.</b> The patterns moved twice during the
 * corpus measurement that produced them — {@code metastases} was not matching {@code metastatic},
 * and {@code resectable} was matching inside {@code unresectable}. Each change makes every stored
 * value stale with nothing to announce it. This is the same reasoning that put the
 * already-indexed probe in {@code TrialIndexService}: a cached inference needs a way to be
 * recomputed, or it quietly lies.
 *
 * <p>Cheap by comparison with re-normalizing. The classifier reads title, summary and
 * description, which are columns on the row already in hand, so this writes one field per trial
 * and touches no child rows, no vector store, and nothing on ClinicalTrials.gov.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TreatmentGoalBackfillService {

    private final TrialDbService trialDbService;

    /**
     * What one backfill run did.
     *
     * <p>{@code unchanged} is reported separately from {@code updated} for the same reason the
     * ingestion result splits "unchanged" from "already waiting": a run that rewrites nothing is
     * a different outcome from a run that had nothing to look at, and collapsing them hides
     * whether a pattern change actually moved anything.
     */
    public record Result(int trialsRead, int updated, int unchanged, List<String> errors) {
    }

    /**
     * Classifies every active trial and writes the value where it differs.
     *
     * <p>Writes only on change. Most runs follow a pattern edit that moves a handful of trials,
     * and rewriting all of them would touch {@code updated_at} on the whole corpus for nothing.
     *
     * <p>A trial that fails is recorded and the run continues. One malformed row must not
     * abandon a corpus-wide backfill — the same contract the RAG backfill follows.
     */
    public Result backfillAll() {
        List<Trial> trials = trialDbService.findByActive(ActiveEnum.ACTIVE);
        List<String> errors = new ArrayList<>();
        int updated = 0;
        int unchanged = 0;

        for (Trial trial : trials) {
            try {
                TreatmentGoal goal = TreatmentGoalClassifier.classify(describableText(trial));
                if (goal.name().equals(trial.getTreatmentGoal())) {
                    unchanged++;
                    continue;
                }

                Trial patch = new Trial();
                patch.setTreatmentGoal(goal.name());
                trialDbService.update(trial.getExtid(), patch);
                updated++;
            } catch (Exception e) {
                log.debug("treatment-goal backfill failed for extid={}", trial.getExtid(), e);
                errors.add("Failed for " + trial.getNctId() + ": " + e.getMessage());
            }
        }

        log.info("backfillAll(): read={} updated={} unchanged={} errors={}",
                trials.size(), updated, unchanged, errors.size());
        return new Result(trials.size(), updated, unchanged, errors);
    }

    /**
     * The trial's own descriptive text.
     *
     * <p>Title, summary and description — never the eligibility criteria. Criteria state who may
     * enrol and routinely describe a patient's treatment history, where "curative intent" means
     * therapy someone already received. Reading intent from there inverts the meaning on exactly
     * the phrases that matter most.
     */
    private String describableText(Trial trial) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, trial.getBriefTitle());
        appendIfPresent(sb, trial.getOfficialTitle());
        appendIfPresent(sb, trial.getBriefSummary());
        appendIfPresent(sb, trial.getDetailedDescription());
        return sb.toString().strip();
    }

    private void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append(' ');
        }
    }
}
