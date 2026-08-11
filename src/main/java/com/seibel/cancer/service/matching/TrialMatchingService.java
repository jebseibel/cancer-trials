package com.seibel.cancer.service.matching;

import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.domain.matching.EligibilitySignal;
import com.seibel.cancer.common.domain.matching.TrialAssessment;
import com.seibel.cancer.database.db.service.LocationDbService;
import com.seibel.cancer.database.db.service.PatientDiagnosisDbService;
import com.seibel.cancer.database.db.service.PatientPriorTreatmentDbService;
import com.seibel.cancer.database.db.service.PatientVariantDbService;
import com.seibel.cancer.database.db.service.TrialDbService;
import com.seibel.cancer.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tier 2 matching: assesses trials against a patient's structured record.
 *
 * <p>Tier 1 (age, sex, recruiting status) lives in the frontend and stays there. This tier is
 * in the backend deliberately — it needs to be testable against the whole corpus in bulk, it
 * has to be able to inform ranking, and Tier 3 will reuse it. None of those are possible from
 * the browser.
 *
 * <p><b>Nothing here removes a trial.</b> Per the no-verdicts rule the output is signals a
 * reader can act on, each carrying the criteria text that produced it. A concern demotes and
 * flags; the judgement stays with the patient, her family, and her oncology team. Receptor
 * status can be re-tested and treatment history changes, so a snapshot of today's record must
 * not permanently hide an option.
 *
 * <p>The patient's record is read once per call and applied to every trial, rather than
 * re-fetched per trial — assessing a page of results otherwise costs a query per trial per
 * table.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class TrialMatchingService extends BaseService {

    private final TrialDbService trialDbService;
    private final LocationDbService locationDbService;
    private final PatientDiagnosisDbService diagnosisDbService;
    private final PatientVariantDbService variantDbService;
    private final PatientPriorTreatmentDbService treatmentDbService;
    private final CriteriaSignalEvaluator evaluator;

    public TrialMatchingService(TrialDbService trialDbService,
                                LocationDbService locationDbService,
                                PatientDiagnosisDbService diagnosisDbService,
                                PatientVariantDbService variantDbService,
                                PatientPriorTreatmentDbService treatmentDbService,
                                CriteriaSignalEvaluator evaluator) {
        super(TrialAssessment.class.getSimpleName());
        this.trialDbService = trialDbService;
        this.locationDbService = locationDbService;
        this.diagnosisDbService = diagnosisDbService;
        this.variantDbService = variantDbService;
        this.treatmentDbService = treatmentDbService;
        this.evaluator = evaluator;
    }

    /**
     * The patient-side inputs, read once and reused across trials.
     *
     * <p>Every field is nullable. A patient who has filled in the Diagnosis tab but not
     * Variants still gets receptor and location signals; the rest report UNKNOWN rather than
     * failing the call. Partial data producing partial answers is the correct behaviour for a
     * tool someone fills in over several days from different documents.
     */
    public record PatientRecord(PatientDiagnosis diagnosis,
                                PatientVariant variant,
                                PatientPriorTreatment treatment) {

        public boolean hasAnything() {
            return diagnosis != null || variant != null || treatment != null;
        }
    }

    /** Loads the most recent active row from each patient table for this user. */
    public PatientRecord loadPatientRecord(Long appUserId) {
        requireNonNull(appUserId, "appUserId");
        return new PatientRecord(
                firstOrNull(diagnosisDbService.findByAppUserId(appUserId)),
                firstOrNull(variantDbService.findByAppUserId(appUserId)),
                firstOrNull(treatmentDbService.findByAppUserId(appUserId)));
    }

    /**
     * Assesses one trial by extid.
     *
     * @return the assessment, or null when the trial does not exist
     */
    public TrialAssessment assess(String trialExtid, PatientRecord record) {
        requireNonBlank(trialExtid, "trialExtid");
        Trial trial = trialDbService.findByExtid(trialExtid);
        if (trial == null) {
            log.info("assess: no trial for extid={}", trialExtid);
            return null;
        }
        return assess(trial, record);
    }

    /** Assesses an already-loaded trial, so a caller iterating results avoids a re-fetch. */
    public TrialAssessment assess(Trial trial, PatientRecord record) {
        return assess(trial, record, null);
    }

    /**
     * @param locationsByTrial pre-fetched locations, or null to look this trial's up on demand.
     *                         Batch callers pass a map so one query serves the whole run.
     */
    private TrialAssessment assess(Trial trial, PatientRecord record,
                                   Map<Long, List<Location>> locationsByTrial) {
        requireNonNull(trial, "trial");
        requireNonNull(record, "record");

        List<EligibilitySignal> signals = new ArrayList<>();

        // First, because it is the one signal that does not depend on the patient record and
        // the one that decides whether the rest are worth reading. The corpus is only 45.7%
        // breast, so without it a ranked list is more than half trials for another disease.
        signals.add(evaluator.diseaseTypeSignal(trial));

        if (record.diagnosis() != null) {
            signals.add(evaluator.receptorSignal(trial, record.diagnosis()));
        } else {
            signals.add(EligibilitySignal.unknown("Receptor status",
                    "No diagnosis is recorded yet, so this could not be checked. Fill in the "
                            + "Diagnosis tab to enable it."));
        }

        signals.add(evaluator.treatmentLineSignal(trial, record.treatment()));
        signals.add(evaluator.pi3kSignal(trial, record.variant(), record.treatment()));

        // Locations are a separate query and only needed for this signal, so they are fetched
        // here rather than being carried on the Trial domain object. A batch caller supplies
        // them up front; a single-trial caller pays one query.
        List<Location> locations;
        if (trial.getId() == null) {
            locations = List.of();
        } else if (locationsByTrial != null) {
            locations = locationsByTrial.getOrDefault(trial.getId(), List.of());
        } else {
            locations = locationDbService.findByTrialId(trial.getId());
        }
        signals.add(evaluator.locationSignal(locations));

        // Locations are carried on the assessment as structured data, not only inside the
        // signal's sentence. Travel decides whether a trial is reachable at all, so the cities
        // have to be renderable on a result card rather than parsed back out of prose.
        List<String> usSites = evaluator.siteLabels(locations, true);
        boolean hasUsSite = !usSites.isEmpty();
        List<String> sites = hasUsSite ? usSites : evaluator.siteLabels(locations, false);

        return new TrialAssessment(trial.getExtid(), trial.getNctId(), signals,
                sites, sites.size(), hasUsSite);
    }

    /**
     * Assesses several trials with one read of the patient record.
     *
     * <p>Order is preserved: the caller's ranking (usually retrieval score) is the input, and
     * demotion is the caller's decision to make from {@code concernCount}. Re-sorting here
     * would bury a highly-relevant trial behind an unparsed location field.
     */
    public List<TrialAssessment> assessAll(List<Trial> trials, Long appUserId) {
        requireNonNull(trials, "trials");
        PatientRecord record = loadPatientRecord(appUserId);
        if (!record.hasAnything()) {
            log.info("assessAll: appUserId={} has no patient record; signals will be unknown",
                    appUserId);
        }

        // Locations for every trial up front, in batches. Fetching them per trial made ranking
        // the breast corpus take 43 seconds on ~2,000 trials - one round trip each. This is the
        // same N+1 shape that makes normalization 99.3% of an ingestion run.
        Map<Long, List<Location>> locationsByTrial = locationDbService.findByTrialIds(
                trials.stream().map(Trial::getId).filter(Objects::nonNull).toList());

        return trials.stream().map(t -> assess(t, record, locationsByTrial)).toList();
    }

    /**
     * Most recent active row, or null.
     *
     * <p>Both repositories order by createdAt desc and filter soft-deleted rows, so the head is
     * the current record. Taking the head of an unfiltered list is what caused the Diagnosis
     * page to edit a deleted row on 2026-08-08.
     */
    private <T> T firstOrNull(List<T> rows) {
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }
}
