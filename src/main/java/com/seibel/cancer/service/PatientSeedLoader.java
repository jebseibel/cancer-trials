package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.Patient;
import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.database.db.repository.PatientRepository;
import com.seibel.cancer.database.db.repository.UserRepository;
import com.seibel.cancer.database.db.service.PatientDbService;
import com.seibel.cancer.database.db.service.UserPatientDbService;
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
 * Recreates the hand-entered patient rows on startup: Patient, PatientDiagnosis,
 * PatientVariant, PatientPriorTreatment.
 *
 * <p><b>Why this exists.</b> Those four are the only rows in this schema created through the
 * API rather than by Liquibase, so a database rebuild silently deletes them - twice now,
 * costing a manual re-entry of a real medical record each time. They are not committed as
 * Liquibase seed data because the diagnosis is real patient data and changesets go to git.
 * The source CSVs live gitignored under {@code .claude/_archive/patient-data/}.
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
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PatientDbService patientDbService;
    private final UserPatientDbService userPatientDbService;
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

            // These four describe the person, not the diagnosis, so they land on `patient` -
            // but the diagnosis CSV is where they are recorded, making this the only seed
            // that can supply them.
            Long patientId = resolveOrCreatePatient(username, new PersonFields(
                    get(row, "displayName"),
                    get(row, "fullName"),
                    date(row, "dateOfBirth"),
                    get(row, "sex")));
            if (hasExisting(diagnosisDbService.findByPatientId(patientId).size(),
                    "PatientDiagnosis", username)) {
                continue;
            }

            diagnosisDbService.create(PatientDiagnosis.builder()
                    .patientId(patientId)
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

            Long patientId = resolveOrCreatePatient(username);
            if (hasExisting(variantDbService.findByPatientId(patientId).size(),
                    "PatientVariant", username)) {
                continue;
            }

            variantDbService.create(PatientVariant.builder()
                    .patientId(patientId)
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

            Long patientId = resolveOrCreatePatient(username);
            if (hasExisting(priorTreatmentDbService.findByPatientId(patientId).size(),
                    "PatientPriorTreatment", username)) {
                continue;
            }

            priorTreatmentDbService.create(PatientPriorTreatment.builder()
                    .patientId(patientId)
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
     * The seed
     * files key on username for the same reason: extids regenerate on every rebuild.
     */
    /** The person fields carried by the diagnosis CSV, all optional. */
    private record PersonFields(String displayName, String fullName, LocalDate dateOfBirth, String sex) {
        static PersonFields none() {
            return new PersonFields(null, null, null, null);
        }

        boolean isEmpty() {
            return displayName == null && fullName == null && dateOfBirth == null && sex == null;
        }
    }

    /**
     * Resolve the patient this login owns, creating it and the OWNER grant if absent.
     *
     * <p>The CSV's {@code username} column names the <em>owning login</em>, not the patient -
     * the person entering the data is often not the person the data is about.
     *
     * <p>Only the diagnosis CSV carries the person fields, so the variant and prior-treatment
     * seeds call this overload and resolve the same patient through its OWNER grant.
     */
    private Long resolveOrCreatePatient(String username) {
        return resolveOrCreatePatient(username, PersonFields.none());
    }

    /**
     * Resolve the patient this login owns, creating it and the OWNER grant if absent.
     *
     * <p>{@code displayName}, {@code fullName}, {@code dateOfBirth} and {@code sex} are
     * properties of the <em>person</em> and live on {@code patient} - but they are recorded in
     * the diagnosis CSV, so this is the only seed that can supply them.
     *
     * <p><strong>An existing patient is backfilled, not left alone.</strong> This is the one
     * deliberate exception to the loader's seed-if-absent rule, and it exists because these
     * columns arrived through schema changes: a patient created earlier has them null, and
     * Tier 1 matching needs the date of birth and sex. <strong>Only null fields are filled</strong>
     * - anything edited through the UI is never overwritten.
     *
     * <p>Writes the grant as well as the patient: a patient with no grant is unreachable by
     * everyone including its owner, since every read goes through a grant lookup.
     */
    private Long resolveOrCreatePatient(String username, PersonFields person) {
        Long userId = userRepository.findByUsername(username)
                .map(u -> u.getId())
                .orElse(null);
        if (userId == null) {
            log.warn("Seed row names username={} with no user row - patient will have no owner", username);
        }

        if (userId != null) {
            Optional<Long> owned = userPatientDbService.findActiveGrantsForUser(userId).stream()
                    .filter(g -> g.getAccessLevel() == AccessLevel.OWNER)
                    .map(g -> g.getPatientId())
                    .findFirst();
            if (owned.isPresent()) {
                backfillPersonFields(owned.get(), person);
                return owned.get();
            }
        }

        // Falls back to the username only when the CSV gives no displayName - the column is
        // not null, so something has to be there, but a login handle is a poor name for a
        // person and should be treated as a missing value to fix rather than a default.
        String displayName = person.displayName() != null ? person.displayName() : username;
        if (person.displayName() == null) {
            log.warn("Seed row for username={} has no displayName - falling back to the username",
                    username);
        }

        Patient created = patientDbService.create(Patient.builder()
                .displayName(displayName)
                .fullName(person.fullName())
                .dateOfBirth(person.dateOfBirth())
                .sex(person.sex())
                .build());

        if (userId != null) {
            userPatientDbService.grant(userId, created.getId(), AccessLevel.OWNER, userId,
                    "seeded from patient CSV");
        }

        log.info("Seeded Patient displayName={} owned by username={}", displayName, username);
        return created.getId();
    }

    /** Fill person fields on an existing patient, but only where they are still null. */
    private void backfillPersonFields(Long patientId, PersonFields person) {
        if (person.isEmpty()) return;

        patientRepository.findById(patientId).ifPresent(existing -> {
            // displayName is not null in the schema, so "still null" for it means "still the
            // username fallback" - that is the value the CSV should be allowed to correct.
            boolean displayNameIsFallback = existing.getDisplayName() == null
                    || existing.getDisplayName().equals(existing.getFullName())
                    || isUsernameFallback(existing.getDisplayName());

            String display = displayNameIsFallback ? person.displayName() : null;
            String full = existing.getFullName() == null ? person.fullName() : null;
            LocalDate dob = existing.getDateOfBirth() == null ? person.dateOfBirth() : null;
            String sex = existing.getSex() == null ? person.sex() : null;

            if (display == null && full == null && dob == null && sex == null) return;

            patientDbService.update(existing.getExtid(), Patient.builder()
                    .displayName(display)
                    .fullName(full)
                    .dateOfBirth(dob)
                    .sex(sex)
                    .build());
            log.info("Backfilled patient extid={} from the diagnosis CSV", existing.getExtid());
        });
    }

    /** True when displayName still holds an owning login's username rather than a person's name. */
    private boolean isUsernameFallback(String displayName) {
        return userRepository.findByUsername(displayName).isPresent();
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
