package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.domain.matching.EligibilitySignal;
import com.seibel.cancer.common.domain.matching.SignalOutcome;
import com.seibel.cancer.common.domain.matching.TrialAssessment;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.database.db.repository.AppUserRepository;
import com.seibel.cancer.service.TrialService;
import com.seibel.cancer.service.matching.CriteriaSignalEvaluator;
import com.seibel.cancer.service.matching.TrialMatchingService;
import com.seibel.cancer.web.response.ResponseEligibilitySignal;
import com.seibel.cancer.web.response.ResponseTrialAssessment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Ranks trials against a patient's structured record — the "Rank Trials for Me" endpoint.
 *
 * <p>The user should not have to compose a search. Trial Search asks someone to know what to
 * type; this asks nothing and uses the record they already filled in across the three Patient
 * Record tabs.
 *
 * <p><b>Nothing here removes a trial.</b> Per the no-verdicts rule, a concern demotes and flags.
 * Receptor status can be re-tested and treatment history changes, so a snapshot of today's
 * record must not permanently hide an option. {@code breastOnly} is offered as an explicit
 * caller choice rather than a default, and it is the one filter that removes rather than ranks.
 */
@RestController
@RequestMapping("/api/matching")
@Validated
@Tag(name = "TrialMatching", description = "Rank trials against a patient's record")
@RequiredArgsConstructor
public class TrialMatchingController {

    private final TrialMatchingService matchingService;
    private final TrialService trialService;
    private final TrialMatchingConverter converter;

    /**
     * Ranks the corpus for one patient, best first.
     *
     * @param appUserExtid whose record to match against
     * @param breastOnly   drop trials that do not appear to be about breast cancer. Default
     *                     false, so the caller opts in to the one filter that hides anything.
     * @param limit        how many assessments to return after ranking
     */
    @GetMapping("/rank/{appUserExtid}")
    @Operation(summary = "Rank trials against a patient's record, best first")
    public List<ResponseTrialAssessment> rank(
            @PathVariable String appUserExtid,
            @RequestParam(required = false, defaultValue = "false") boolean breastOnly,
            @RequestParam(required = false, defaultValue = "50") int limit
    ) {
        Long appUserId = converter.resolveAppUserId(appUserExtid);
        List<Trial> trials = trialService.findByActive(ActiveEnum.ACTIVE);

        // Narrow before assessing, not after. assess() runs one location query per trial, so
        // assessing the whole corpus costs ~4,600 round trips per request - the same
        // per-record-query shape that makes normalization 99.3% of an ingestion run. The
        // disease gate reads only title and summary, which are already in hand, so applying it
        // first drops ~54% of that cost for free.
        List<Trial> candidates = breastOnly
                ? trials.stream().filter(converter::looksLikeBreastTrial).toList()
                : trials;

        List<TrialAssessment> assessments = matchingService.assessAll(candidates, appUserId);

        return assessments.stream()
                .sorted(converter.ranking())
                .limit(Math.max(1, limit))
                .map(a -> converter.toResponse(a, candidates))
                .toList();
    }

    /** Assesses a single trial, for the Trial Detail page. */
    @GetMapping("/trial/{trialExtid}/for/{appUserExtid}")
    @Operation(summary = "Assess one trial against a patient's record")
    public ResponseTrialAssessment assessOne(
            @PathVariable String trialExtid,
            @PathVariable String appUserExtid
    ) {
        Long appUserId = converter.resolveAppUserId(appUserExtid);
        TrialAssessment assessment = matchingService.assess(
                trialExtid, matchingService.loadPatientRecord(appUserId));
        if (assessment == null) {
            throw new ResourceNotFoundException("Trial", trialExtid);
        }
        return converter.toResponse(assessment, List.of());
    }
}

@Component
@RequiredArgsConstructor
class TrialMatchingConverter {

    private static final String DISEASE_TYPE_SIGNAL = "Disease type";

    private final AppUserRepository appUserRepository;
    private final TrialService trialService;
    private final CriteriaSignalEvaluator evaluator;

    Long resolveAppUserId(String appUserExtid) {
        return appUserRepository.findByExtid(appUserExtid)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", appUserExtid))
                .getId();
    }

    /**
     * Ranking order, and the reasoning behind each tier.
     *
     * <p>There is no score to sort on by design, so ordering is lexicographic over the counts
     * that are honest. Breast-cancer trials first, because the corpus is only 45.7% breast and
     * anything else is a trial for a disease the patient does not have. Then fewest concerns,
     * which is the demotion key the whole tier is built around. Then most signals actually
     * passed, so a trial that affirmatively matches outranks one that merely said nothing.
     * Finally most signals applicable, preferring trials the tool could say something about
     * over trials it was silent on — silence is not a pass.
     */
    Comparator<TrialAssessment> ranking() {
        return Comparator
                .comparing((TrialAssessment a) -> isBreastCancer(a) ? 0 : 1)
                .thenComparingLong(TrialAssessment::concernCount)
                .thenComparing(Comparator.comparingLong(TrialAssessment::passCount).reversed())
                .thenComparing(Comparator.comparingLong(TrialAssessment::applicableCount).reversed())
                .thenComparing(a -> a.nctId() == null ? "" : a.nctId());
    }

    /** True when the disease-type signal passed. A basket trial is UNKNOWN, so it is not. */
    boolean isBreastCancer(TrialAssessment assessment) {
        return assessment.signals().stream()
                .filter(s -> DISEASE_TYPE_SIGNAL.equals(s.name()))
                .anyMatch(s -> s.outcome() == SignalOutcome.PASS);
    }

    /**
     * The same disease test, applied before assessment so the expensive per-trial work is
     * skipped entirely. Calls the evaluator rather than re-implementing the check, so the
     * pre-filter and the reported signal can never disagree.
     */
    boolean looksLikeBreastTrial(Trial trial) {
        return evaluator.diseaseTypeSignal(trial).outcome() == SignalOutcome.PASS;
    }

    /**
     * @param loaded trials already in hand, so a ranked page does not re-fetch each one. Falls
     *               back to a lookup by extid when the caller has none (single-trial assessment).
     */
    ResponseTrialAssessment toResponse(TrialAssessment assessment, List<Trial> loaded) {
        Trial trial = loaded.stream()
                .filter(t -> assessment.trialExtid() != null
                        && assessment.trialExtid().equals(t.getExtid()))
                .findFirst()
                .orElseGet(() -> trialService.findByExtid(assessment.trialExtid()));

        return ResponseTrialAssessment.builder()
                .trialExtid(assessment.trialExtid())
                .nctId(assessment.nctId())
                .briefTitle(trial == null ? null : trial.getBriefTitle())
                .overallStatus(trial == null ? null : trial.getOverallStatus())
                .signals(assessment.signals().stream().map(this::toResponse).toList())
                .concernCount(assessment.concernCount())
                .unknownCount(assessment.unknownCount())
                .passCount(assessment.passCount())
                .applicableCount(assessment.applicableCount())
                .breastCancer(isBreastCancer(assessment))
                .siteCities(assessment.siteCities())
                .siteCount(assessment.siteCount())
                .hasUnitedStatesSite(assessment.hasUnitedStatesSite())
                .build();
    }

    ResponseEligibilitySignal toResponse(EligibilitySignal signal) {
        return ResponseEligibilitySignal.builder()
                .name(signal.name())
                .outcome(signal.outcome().name())
                .detail(signal.detail())
                .evidence(signal.evidence())
                .build();
    }
}
