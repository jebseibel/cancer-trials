package com.seibel.cancer.datafetcher.normalization;

import com.seibel.cancer.common.domain.ArmGroup;
import com.seibel.cancer.common.domain.Condition;
import com.seibel.cancer.common.domain.Intervention;
import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.common.domain.Outcome;
import com.seibel.cancer.common.domain.OverallOfficial;
import com.seibel.cancer.common.domain.StagingRawTrial;
import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.domain.TrialSource;
import com.seibel.cancer.database.db.service.ArmGroupDbService;
import com.seibel.cancer.database.db.service.ConditionDbService;
import com.seibel.cancer.database.db.service.InterventionDbService;
import com.seibel.cancer.database.db.service.LocationDbService;
import com.seibel.cancer.database.db.service.OutcomeDbService;
import com.seibel.cancer.database.db.service.OverallOfficialDbService;
import com.seibel.cancer.database.db.service.SponsorDbService;
import com.seibel.cancer.database.db.service.StagingRawTrialDbService;
import com.seibel.cancer.database.db.service.TrialDbService;
import com.seibel.cancer.database.db.service.TrialSourceDbService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Normalizes a single staging row within its own transaction. Split out from
 * TrialNormalizationService (rather than a self-invoked @Transactional method there) so
 * Spring's transactional proxy is actually applied per row - a self-invoked call bypasses
 * the proxy entirely and @Transactional would be silently ignored.
 */
@Component
@RequiredArgsConstructor
class TrialRowNormalizer {

    private final List<TrialSourceParser> parsers;
    private final StagingRawTrialDbService stagingRawTrialDbService;
    private final TrialSourceDbService trialSourceDbService;
    private final TrialDbService trialDbService;
    private final LocationDbService locationDbService;
    private final ArmGroupDbService armGroupDbService;
    private final InterventionDbService interventionDbService;
    private final OutcomeDbService outcomeDbService;
    private final OverallOfficialDbService overallOfficialDbService;
    private final ConditionDbService conditionDbService;
    private final SponsorDbService sponsorDbService;

    @Transactional
    void normalize(StagingRawTrial staging) {
        normalize(staging, new NormalizationCache());
    }

    @Transactional
    void normalize(StagingRawTrial staging, NormalizationCache cache) {
        // Cached: the same TrialSource row for every trial in a run.
        TrialSource trialSource = cache.trialSource(
                staging.getTrialSourceId(), trialSourceDbService::findById);
        if (trialSource == null) {
            throw new IllegalStateException("Unknown trialSourceId: " + staging.getTrialSourceId());
        }

        TrialSourceParser parser = parsers.stream()
                .filter(p -> p.supports(trialSource.getCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No parser for trial source: " + trialSource.getCode()));

        NormalizedTrial normalized = parser.parse(staging.getRawPayload());
        Trial incoming = normalized.getTrial();
        incoming.setPrimaryTrialSourceId(trialSource.getId());

        Trial existing = incoming.getNctId() != null ? trialDbService.findByNctId(incoming.getNctId()) : null;
        Trial persisted = existing != null
                ? trialDbService.update(existing.getExtid(), incoming)
                : trialDbService.create(incoming);

        if (existing != null) {
            deleteExistingChildren(persisted.getId());
        }
        insertChildren(persisted.getId(), normalized);
        upsertConditionsAndSponsors(normalized, cache);

        stagingRawTrialDbService.update(staging.getExtid(), null, null, null, null, LocalDateTime.now(), null);
    }

    /** Runs in its own transaction so a failure elsewhere doesn't roll back the error write. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    void markFailed(StagingRawTrial staging, String errorMessage) {
        stagingRawTrialDbService.update(staging.getExtid(), null, null, null, null, null, errorMessage);
    }

    private void deleteExistingChildren(Long trialId) {
        locationDbService.findByTrialId(trialId).forEach(l -> locationDbService.delete(l.getExtid()));
        armGroupDbService.findByTrialId(trialId).forEach(a -> armGroupDbService.delete(a.getExtid()));
        interventionDbService.findByTrialId(trialId).forEach(i -> interventionDbService.delete(i.getExtid()));
        outcomeDbService.findByTrialId(trialId).forEach(o -> outcomeDbService.delete(o.getExtid()));
        overallOfficialDbService.findByTrialId(trialId).forEach(o -> overallOfficialDbService.delete(o.getExtid()));
    }

    private void insertChildren(Long trialId, NormalizedTrial normalized) {
        for (Location location : normalized.getLocations()) {
            location.setTrialId(trialId);
            locationDbService.create(location);
        }
        for (ArmGroup armGroup : normalized.getArmGroups()) {
            armGroup.setTrialId(trialId);
            armGroupDbService.create(armGroup);
        }
        for (Intervention intervention : normalized.getInterventions()) {
            intervention.setTrialId(trialId);
            interventionDbService.create(intervention);
        }
        for (Outcome outcome : normalized.getOutcomes()) {
            outcome.setTrialId(trialId);
            outcomeDbService.create(outcome);
        }
        for (OverallOfficial official : normalized.getOverallOfficials()) {
            official.setTrialId(trialId);
            overallOfficialDbService.create(official);
        }
    }

    private void upsertConditionsAndSponsors(NormalizedTrial normalized, NormalizationCache cache) {
        for (String name : normalized.getConditionNames()) {
            if (name == null || name.isBlank()) continue;
            Condition existing = cache.condition(name, conditionDbService::findByName);
            if (existing == null) {
                // Cache the created row so the next trial mentioning this condition does not
                // re-query and does not attempt a duplicate insert.
                cache.putCondition(name, conditionDbService.create(name));
            }
        }
        for (NormalizedTrial.NormalizedSponsor sponsor : normalized.getSponsors()) {
            if (sponsor.getName() == null || sponsor.getName().isBlank()) continue;
            var existingSponsor = cache.sponsor(sponsor.getName(), sponsorDbService::findByName);
            if (existingSponsor == null) {
                cache.putSponsor(sponsor.getName(),
                        sponsorDbService.create(sponsor.getName(), sponsor.getOrgClass()));
            }
        }
    }
}
