package com.seibel.cancer.database.db.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where the hand-maintained patient seed CSVs live, and whether to read them at all.
 *
 * <p>These files hold a real medical record, so they are gitignored rather than committed as
 * Liquibase seed data - which is the whole reason this loader exists instead of another
 * changeset. See {@code .claude/patient-data/}.
 */
@Data
@ConfigurationProperties(prefix = "cancer.seed.patient")
public class PatientSeedProperties {

    /** Set false to skip seeding entirely - e.g. on a shared or deployed instance. */
    private boolean enabled = true;

    /** Directory holding the three CSVs. Relative paths resolve against the working directory. */
    private String directory = ".claude/patient-data";

    private String diagnosisFile = "patient-diagnosis.csv";
    private String variantFile = "patient-variant.csv";
    private String priorTreatmentFile = "patient-prior-treatment.csv";
}
