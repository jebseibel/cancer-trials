package com.seibel.cancer.rag.eval;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the evaluation set in {@code rag/src/test/resources/eval/retrieval-queries.md} against
 * the live stack, turning retrieval quality into a pass/fail number.
 *
 * <p>This is what makes an embedding-model change assessable: run it before and after, compare.
 * Without it, "the new model feels better" is the only available judgement.
 *
 * <p>Hits the running backend over HTTP rather than wiring a Spring context, because the thing
 * worth measuring is the whole real pipeline - query -> embed -> Qdrant filter -> MySQL
 * hydration. A mocked version would measure none of it.
 *
 * <p><b>Skips (does not fail) when the backend is not running</b>, so a normal {@code ./gradlew
 * build} is unaffected. The user starts and stops the backend.
 */
class RetrievalEvaluation {

    private static final String BASE = "http://localhost:8080/api/rag/search";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    /**
     * @param query        the search text
     * @param minTopScore  best match must score at least this - catches "found nothing useful"
     * @param expectSource best match must come from this field - catches "right words, wrong place"
     */
    private record EvalCase(String query, double minTopScore, String expectSource) {
    }

    /** Asserted cases. Expectations are labeled from what the text means, not from model output. */
    private static final List<EvalCase> CASES = List.of(
            new EvalCase("ECOG performance status 0 or 1", 0.90, "INCLUSION_CRITERION"),
            new EvalCase("pregnant or breastfeeding", 0.85, "EXCLUSION_CRITERION"),
            new EvalCase("brain metastases", 0.80, "EXCLUSION_CRITERION"),
            new EvalCase("triple negative breast cancer", 0.75, "INCLUSION_CRITERION"),
            new EvalCase("HER2 positive", 0.60, "INCLUSION_CRITERION"),
            new EvalCase("measurable disease RECIST", 0.60, "INCLUSION_CRITERION"),
            new EvalCase("adequate liver function", 0.60, "INCLUSION_CRITERION"),
            // Expected source is EXCLUSION: prior-chemo limits are written as exclusions, so the
            // correct answer here is a disqualifying match. Guards the exclusion-flag logic.
            new EvalCase("no prior chemotherapy", 0.60, "EXCLUSION_CRITERION"));

    /**
     * Known-weak queries. Reported but NOT asserted, so a model upgrade can be measured against
     * them. Deliberately not given lowered thresholds - that would hide the weakness.
     */
    private static final List<String> TRACKED = List.of(
            "trials studying a BRCA mutation",
            "recruiting trials I could join now");

    @Test
    @DisplayName("retrieval evaluation set: every asserted query meets its score and source bar")
    void evaluationSetPasses() {
        requireBackendRunning();

        List<String> failures = new ArrayList<>();
        System.out.println("\n=== ASSERTED ===");
        for (EvalCase c : CASES) {
            String body = search(c.query());
            double score = firstDouble(body, "\"topScore\":([0-9.]+)");
            String source = firstString(body, "\"source\":\"([A-Z_]+)\"");

            boolean scoreOk = score >= c.minTopScore();
            boolean sourceOk = c.expectSource().equals(source);
            System.out.printf("  %-34s %.3f (min %.2f) %-20s %s%n",
                    c.query(), score, c.minTopScore(), source,
                    (scoreOk && sourceOk) ? "PASS" : "FAIL");

            if (!scoreOk) {
                failures.add("%s: score %.3f < %.2f".formatted(c.query(), score, c.minTopScore()));
            }
            if (!sourceOk) {
                failures.add("%s: source %s != %s".formatted(c.query(), source, c.expectSource()));
            }
        }

        System.out.println("=== TRACKED (not asserted) ===");
        for (String q : TRACKED) {
            String body = search(q);
            System.out.printf("  %-34s %.3f  %s%n", q,
                    firstDouble(body, "\"topScore\":([0-9.]+)"),
                    firstString(body, "\"text\":\"([^\"]{0,70})"));
        }

        assertThat(failures).as("evaluation set failures").isEmpty();
    }

    /**
     * Confirms the backend is up, failing rather than skipping when it is not.
     *
     * <p><b>This deliberately does not use {@code Assumptions.abort}.</b> It did, and the skip
     * rendered as {@code BUILD SUCCESSFUL} — so on 2026-08-10 a down backend was read as this
     * evaluation passing, twice, and a corpus was reported as validated when nothing had run.
     * A test whose failure mode is indistinguishable from success is worse than no test.
     *
     * <p>The cost of failing instead is that {@code ./gradlew build} needs a running backend.
     * That is the right trade for an evaluation whose entire purpose is measuring the live
     * system: there is no useful "skipped" outcome here, only "measured" or "did not measure".
     * Set {@code -Deval.skipWithoutBackend=true} to restore the old behaviour for a CI run that
     * genuinely has no backend.
     */
    private void requireBackendRunning() {
        String failure;
        try {
            HttpResponse<String> r = HTTP.send(
                    HttpRequest.newBuilder(URI.create(BASE + "?query=ping&maxTrials=1"))
                            .timeout(Duration.ofSeconds(30)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 200) {
                return;
            }
            failure = "backend returned HTTP " + r.statusCode();
        } catch (Exception e) {
            failure = "backend unreachable on :8080 (" + e.getClass().getSimpleName()
                    + ": " + e.getMessage() + ")";
        }

        if (Boolean.getBoolean("eval.skipWithoutBackend")) {
            Assumptions.abort(failure + " - skipping (eval.skipWithoutBackend=true)");
        }
        throw new AssertionError(failure
                + ". The retrieval evaluation measures the live system, so it cannot run without"
                + " one - start the backend and re-run. Pass -Deval.skipWithoutBackend=true to"
                + " skip instead.");
    }

    private String search(String query) {
        try {
            String url = BASE + "?maxTrials=1&query="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpResponse<String> r = HTTP.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            return r.body();
        } catch (Exception e) {
            throw new IllegalStateException("search failed for: " + query, e);
        }
    }

    private double firstDouble(String body, String regex) {
        Matcher m = Pattern.compile(regex).matcher(body);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }

    private String firstString(String body, String regex) {
        Matcher m = Pattern.compile(regex).matcher(body);
        return m.find() ? m.group(1) : "(none)";
    }
}
