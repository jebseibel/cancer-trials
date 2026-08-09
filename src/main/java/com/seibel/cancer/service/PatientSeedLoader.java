package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.AppUser;
import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.database.db.repository.AppUserRepository;
import com.seibel.cancer.database.db.service.AppUserDbService;
import com.seibel.cancer.database.db.service.PatientDiagnosisDbService;
import com.seibel.cancer.database.db.service.PatientPriorTreatmentDbService;
import com.seibel.cancer.database.db.service.PatientSeedProperties;
import com.seibel.cancer.database.db.service.PatientVariantDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Recreates the hand-entered patient rows on startup: AppUser, PatientDiagnosis,
 * PatientVariant, PatientPriorTreatment.
 *
 * <p><b>Why this exists.</b> Those four are the only rows in this schema created through the
 * API rather than by Liquibase, so a database rebuild silently deletes them - twice now,
 * costing a manual re-entry of a real medical record each time. They are not committed as
 * Liquibase seed data because the diagnosis is real patient data and changesets go to git.
 * The source CSVs live gitignored under {@code .claude/patient-data/}.
 *
 * <p><b>Seed if absent, never sync.</b> An existing row is left completely alone. Re-reading
 * the file over a row edited through the UI would silently revert those edits on every
 * restart, which is worse than the problem being solved. The CSV is a floor, not a source of
 * truth.
 *
 * <p>Missing files are not an error - the app must still boot on a machine that has no
 * patient data at all.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientSeedLoader implements CommandLineRunner {

    private final PatientSeedProperties properties;
    private final AppUserRepository appUserRepository;
    private final AppUserDbService appUserDbService;
    private final PatientDiagnosisDbService diagnosisDbService;
    private final PatientVariantDbService variantDbService;
    private final PatientPriorTreatmentDbService priorTreatmentDbService;

    @Override
    public void run(String... args) {
        if (!properties.isEnabled()) {
            log.info("Patient seed loader disabled (cancer.seed.patient.enabled=false)");
            return;
        }

        Path dir = Path.of(properties.getDirectory());
        if (!Files.isDirectory(dir)) {
            log.info("Patient seed directory not found at {} - nothing to seed", dir.toAbsolutePath());
            return;
        }

        try {
            seedDiagnosis(dir.resolve(properties.getDiagnosisFile()));
            seedVariant(dir.resolve(properties.getVariantFile()));
            seedPriorTreatment(dir.resolve(properties.getPriorTreatmentFile()));
        } catch (Exception e) {
            // Never prevent startup. A malformed seed file is a data problem to fix, not a
            // reason the application cannot run.
            log.error("Patient seed failed - continuing startup", e);
        }
    }

    // ------------------------------------------------------------------ diagnosis

    private void seedDiagnosis(Path file) throws Exception {
        List<CSVRecord> rows = read(file);
        if (rows.isEmpty()) return;

        for (CSVRecord row : rows) {
            String username = get(row, "username");
            if (username == null) {
                log.warn("Seed diagnosis row has no username - skipped");
                continue;
            }

            Long appUserId = resolveOrCreateAppUser(username);
            if (hasExisting(diagnosisDbService.findByAppUserId(appUserId).size(),
                    "PatientDiagnosis", username)) {
                continue;
            }

            diagnosisDbService.create(PatientDiagnosis.builder()
                    .appUserId(appUserId)
                    .cancerType(get(row, "cancerType"))
                    .stage(get(row, "stage"))
                    .stageSystem(get(row, "stageSystem"))
                    .isMetastatic(bool(row, "isMetastatic"))
                    .metastasisSites(get(row, "metastasisSites"))
                    .receptorSubtype(get(row, "receptorSubtype"))
                    .erStatus(get(row, "erStatus"))
                    .prStatus(get(row, "prStatus"))
                    .her2Status(get(row, "her2Status"))
                    .biomarkers(get(row, "biomarkers"))
                    .ecogStatus(integer(row, "ecogStatus"))
                    .priorChemoRegimens(integer(row, "priorChemoRegimens"))
                    .lastChemoEndDate(date(row, "lastChemoEndDate"))
                    .priorTreatments(get(row, "priorTreatments"))
                    .hasMeasurableDisease(bool(row, "hasMeasurableDisease"))
                    .menopausalStatus(get(row, "menopausalStatus"))
                    .dateOfBirth(date(row, "dateOfBirth"))
                    .sex(get(row, "sex"))
                    .diagnosisDate(date(row, "diagnosisDate"))
                    .notes(get(row, "notes"))
                    .build());

            log.info("Seeded PatientDiagnosis for username={}", username);
        }
    }

    // ------------------------------------------------------------------ variant

    private void seedVariant(Path file) throws Exception {
        List<CSVRecord> rows = read(file);
        if (rows.isEmpty()) return;

        for (CSVRecord row : rows) {
            String username = get(row, "username");
            if (username == null) {
                log.warn("Seed variant row has no username - skipped");
                continue;
            }

            Long appUserId = resolveOrCreateAppUser(username);
            if (hasExisting(variantDbService.findByAppUserId(appUserId).size(),
                    "PatientVariant", username)) {
                continue;
            }

            variantDbService.create(PatientVariant.builder()
                    .appUserId(appUserId)
                    .pik3caStatus(get(row, "pik3caStatus"))
                    .esr1Status(get(row, "esr1Status"))
                    .tp53Status(get(row, "tp53Status"))
                    .akt1Status(get(row, "akt1Status"))
                    .ptenStatus(get(row, "ptenStatus"))
                    .erbb2SomaticStatus(get(row, "erbb2SomaticStatus"))
                    .brca1Status(get(row, "brca1Status"))
                    .brca2Status(get(row, "brca2Status"))
                    .palb2Status(get(row, "palb2Status"))
                    .atmStatus(get(row, "atmStatus"))
                    .chek2Status(get(row, "chek2Status"))
                    .hrdStatus(get(row, "hrdStatus"))
                    .pdl1Status(get(row, "pdl1Status"))
                    .ki67Percent(integer(row, "ki67Percent"))
                    .germlineTestDone(get(row, "germlineTestDone"))
                    .somaticTestDone(get(row, "somaticTestDone"))
                    .testDate(date(row, "testDate"))
                    .testLab(get(row, "testLab"))
                    .otherVariants(get(row, "otherVariants"))
                    .notes(get(row, "notes"))
                    .build());

            log.info("Seeded PatientVariant for username={}", username);
        }
    }

    // ------------------------------------------------------------------ prior treatment

    private void seedPriorTreatment(Path file) throws Exception {
        List<CSVRecord> rows = read(file);
        if (rows.isEmpty()) return;

        for (CSVRecord row : rows) {
            String username = get(row, "username");
            if (username == null) {
                log.warn("Seed prior-treatment row has no username - skipped");
                continue;
            }

            Long appUserId = resolveOrCreateAppUser(username);
            if (hasExisting(priorTreatmentDbService.findByAppUserId(appUserId).size(),
                    "PatientPriorTreatment", username)) {
                continue;
            }

            priorTreatmentDbService.create(PatientPriorTreatment.builder()
                    .appUserId(appUserId)
                    .cdk46Status(get(row, "cdk46Status"))
                    .endocrineStatus(get(row, "endocrineStatus"))
                    .serdStatus(get(row, "serdStatus"))
                    .chemoStatus(get(row, "chemoStatus"))
                    .her2TherapyStatus(get(row, "her2TherapyStatus"))
                    .her2AdcStatus(get(row, "her2AdcStatus"))
                    .trop2AdcStatus(get(row, "trop2AdcStatus"))
                    .parpStatus(get(row, "parpStatus"))
                    .pi3kAktMtorStatus(get(row, "pi3kAktMtorStatus"))
                    .immunotherapyStatus(get(row, "immunotherapyStatus"))
                    .taxaneStatus(get(row, "taxaneStatus"))
                    .anthracyclineStatus(get(row, "anthracyclineStatus"))
                    .platinumStatus(get(row, "platinumStatus"))
                    .currentDrugNames(get(row, "currentDrugNames"))
                    .priorDrugNames(get(row, "priorDrugNames"))
                    .linesOfTherapyMetastatic(integer(row, "linesOfTherapyMetastatic"))
                    .hadNeoadjuvant(bool(row, "hadNeoadjuvant"))
                    .hadAdjuvant(bool(row, "hadAdjuvant"))
                    .hadRadiation(bool(row, "hadRadiation"))
                    .hadSurgery(bool(row, "hadSurgery"))
                    .lastTreatmentEndDate(date(row, "lastTreatmentEndDate"))
                    .currentlyOnTreatment(bool(row, "currentlyOnTreatment"))
                    .otherTreatments(get(row, "otherTreatments"))
                    .notes(get(row, "notes"))
                    .build());

            log.info("Seeded PatientPriorTreatment for username={}", username);
        }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * AppUser and User (login) are separate tables matched by username, with no FK. The seed
     * files key on username for the same reason: extids regenerate on every rebuild.
     */
    private Long resolveOrCreateAppUser(String username) {
        Optional<Long> existing = appUserRepository.findByUsername(username)
                .map(u -> u.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        // passwordHash is required by the entity even though AppUser never authenticates -
        // a known design flaw, documented in PATIENT_MODEL_PLAN.md.
        AppUser created = appUserDbService.create(AppUser.builder()
                .username(username)
                .passwordHash("seeded-app-user-does-not-authenticate")
                .displayName(username)
                .build());

        log.info("Seeded AppUser username={}", username);
        return created.getId();
    }

    private boolean hasExisting(int count, String type, String username) {
        if (count > 0) {
            log.debug("{} already present for username={} - left untouched", type, username);
            return true;
        }
        return false;
    }

    private List<CSVRecord> read(Path file) throws Exception {
        if (!Files.isRegularFile(file)) {
            log.info("No seed file at {} - skipped", file);
            return List.of();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            return CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreSurroundingSpaces(true)
                    .build()
                    .parse(reader)
                    .getRecords();
        }
    }

    /** Blank cells become null, never "" - the same rule the frontend forms apply on save. */
    private String get(CSVRecord row, String column) {
        if (!row.isMapped(column)) return null;
        String value = row.get(column);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer integer(CSVRecord row, String column) {
        String value = get(row, column);
        if (value == null) return null;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("Seed column {} is not a number: {}", column, value);
            return null;
        }
    }

    private Boolean bool(CSVRecord row, String column) {
        String value = get(row, column);
        return value == null ? null : Boolean.valueOf(value);
    }

    private LocalDate date(CSVRecord row, String column) {
        String value = get(row, column);
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            log.warn("Seed column {} is not an ISO date: {}", column, value);
            return null;
        }
    }
}
