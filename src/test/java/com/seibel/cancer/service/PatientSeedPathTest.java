package com.seibel.cancer.service;

import com.seibel.cancer.database.db.service.PatientSeedProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the one path whose breakage is silent.
 *
 * <p>{@code PatientSeedLoader} treats a missing directory as normal - the app must boot on a
 * machine with no patient data - so a wrong path produces no error, no warning and no failed
 * test. The symptom appears only after the next database rebuild, as a patient record that did
 * not come back, and the fix is re-entering a real medical record by hand.
 *
 * <p>That already happened when the seed directory was moved under {@code _archive/} and the
 * configured default was left pointing at the old location. 956 tests passed throughout, because
 * nothing covered this.
 */
class PatientSeedPathTest {

    /** Repo root, resolved from the working directory the test runs in. */
    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        // Gradle runs root-module tests from the repo root; tolerate a module subdirectory too.
        return Files.exists(cwd.resolve("settings.gradle")) ? cwd : cwd.getParent();
    }

    private static String seedDirFromYaml() {
        try (InputStream in = PatientSeedPathTest.class.getResourceAsStream("/application.yml")) {
            Map<String, Object> root = new Yaml().load(in);
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) ((Map<String, Object>)
                    ((Map<String, Object>) root.get("cancer")).get("seed")).get("patient");
            String raw = String.valueOf(patient.get("directory"));

            // "${PATIENT_SEED_DIR:.claude/_archive/patient-data}" - we want the default, since
            // that is what a developer machine with no override actually uses.
            Matcher m = Pattern.compile("\\$\\{[^:}]+:([^}]*)}").matcher(raw);
            return m.matches() ? m.group(1) : raw;
        } catch (Exception e) {
            throw new IllegalStateException("could not read cancer.seed.patient.directory", e);
        }
    }

    @Test
    @DisplayName("the yaml default and the properties default name the same directory")
    void yamlAndPropertiesAgree() {
        assertThat(seedDirFromYaml())
                .as("application.yml default vs PatientSeedProperties field default - two "
                        + "defaults for one path, so both have to move together")
                .isEqualTo(new PatientSeedProperties().getDirectory());
    }

    /**
     * Only meaningful on a machine that has the real seed data. CI and a fresh clone do not,
     * and must not fail for it - the files are gitignored on purpose.
     */
    static boolean seedDirectoryPresent() {
        return Files.isDirectory(repoRoot().resolve(seedDirFromYaml()));
    }

    @Test
    @EnabledIf("seedDirectoryPresent")
    @DisplayName("every configured CSV exists where the config says it does")
    void configuredFilesExistOnDisk() {
        PatientSeedProperties props = new PatientSeedProperties();
        Path dir = repoRoot().resolve(seedDirFromYaml());

        assertThat(dir.resolve(props.getDiagnosisFile())).exists();
        assertThat(dir.resolve(props.getVariantFile())).exists();
        assertThat(dir.resolve(props.getPriorTreatmentFile())).exists();
    }

    /**
     * The directory holds a real medical record. It is gitignored at directory level so a new
     * file is protected the moment it lands, and this asserts the rule still names the place the
     * files actually are - a move that updated one without the other would expose them.
     */
    @Test
    @DisplayName("the seed directory is covered by gitignore")
    void seedDirectoryIsGitignored() throws Exception {
        String ignored = Files.readString(repoRoot().resolve(".gitignore"));
        String dir = seedDirFromYaml();

        assertThat(ignored)
                .as(".gitignore must name %s, or the real patient CSVs become committable", dir)
                .contains(dir);
    }
}
