package com.seibel.cancer.service.ai;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.progress.ProgressTicker;
import com.seibel.cancer.config.MatchingProperties;
import com.seibel.cancer.database.db.service.TrialDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates {@code trial.friendly_title} across every trial that does not have one yet.
 *
 * <p><b>Skips trials that already have a value, unlike the treatment-goal backfill.</b> That
 * backfill re-derives a free, deterministic classification and only writes on change; this one
 * is a paid AI call per trial, so re-running it against a corpus that already has titles would
 * re-spend money for no new answer. There is no "regenerate everything" mode here on purpose -
 * see {@link TrialFriendlyTitleService#generate} for the per-trial always-overwrite action,
 * which is a deliberate single press rather than something that can be triggered corpus-wide.
 *
 * <p><b>A corpus-wide run against ~2,500 trials is a real, non-trivial cost.</b> ADMIN-only, and
 * the caller should expect this to take a while and to be run deliberately, not routinely - the
 * same caution the AI trial check's "no cost ceiling" gap already flags for this project.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialFriendlyTitleBackfillService {

    private final TrialDbService trialDbService;
    private final TrialFriendlyTitleService friendlyTitleService;
    private final MatchingProperties matchingProperties;

    /**
     * What one backfill run did.
     *
     * <p>{@code alreadyPresent} is reported separately from {@code generated} for the same
     * reason ingestion splits "unchanged" from "already waiting": a run that paid for nothing is
     * a different outcome from a run that had nothing left to do.
     */
    public record Result(int trialsRead, int generated, int alreadyPresent, List<String> errors) {
    }

    public Result backfillAll() {
        List<Trial> trials = trialDbService.findByActive(ActiveEnum.ACTIVE);
        List<String> errors = new ArrayList<>();
        int generated = 0;
        int alreadyPresent = 0;

        // A bar earns its place here more than almost anywhere else in this project: every
        // record is a real network round-trip to the AI provider, not a local computation, so a
        // corpus-wide run is long and otherwise silent until the single summary line at the end.
        MatchingProperties.Progress progressConfig = matchingProperties.getProgress();
        try (ProgressTicker ticker = new ProgressTicker(
                "GENERATING FRIENDLY TITLES",
                progressConfig.getLineWidth(),
                progressConfig.getFlushInterval(),
                progressConfig.resolveEnabled(System.console() != null),
                trials.size())) {

            for (Trial trial : trials) {
                if (trial.getFriendlyTitle() != null && !trial.getFriendlyTitle().isBlank()) {
                    alreadyPresent++;
                    // A skip glyph, not a tick: an already-titled trial cost nothing this run,
                    // and a re-run against a mostly-titled corpus is mostly these.
                    ticker.skip();
                    continue;
                }
                try {
                    friendlyTitleService.generate(trial);
                    generated++;
                    ticker.tick();
                } catch (Exception e) {
                    // One trial's AI call failing - a timeout, a rate limit - must not abandon a
                    // corpus-wide run that has already paid for everything before it.
                    log.debug("friendly title backfill failed for extid={}", trial.getExtid(), e);
                    errors.add("Failed for " + trial.getNctId() + ": " + e.getMessage());
                    ticker.error();
                }
            }
        }

        log.info("backfillAll(): read={} generated={} alreadyPresent={} errors={}",
                trials.size(), generated, alreadyPresent, errors.size());
        return new Result(trials.size(), generated, alreadyPresent, errors.size() > 0 ? errors : List.of());
    }
}
