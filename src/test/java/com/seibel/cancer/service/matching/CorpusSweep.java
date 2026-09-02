package com.seibel.cancer.service.matching;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.domain.matching.EligibilitySignal;
import com.seibel.cancer.common.domain.matching.SignalOutcome;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Measures {@link CriteriaSignalEvaluator} against the real corpus over the REST API.
 *
 * <p><b>This is a measurement harness, not an assertion test.</b> It exists because 41 unit
 * tests prove only that the evaluator handles phrasings that were written by the same person
 * who wrote the patterns — circular in the way that matters. The corpus is the only thing that
 * can say whether those patterns describe how trials actually write criteria.
 *
 * <p>Read-only: pages {@code /api/trial}, writes nothing, touches no vector store. Run it with
 * {@code -Dsweep.enabled=true}; it is skipped by default so a normal build does not depend on a
 * running backend. Size with {@code -Dsweep.limit=N}.
 *
 * <p>Never connects to the database. The REST API is the only supported route into this data.
 */
class CorpusSweep {

    private static final String BASE = System.getProperty("sweep.base", "http://localhost:8080");
    private static final int LIMIT = Integer.getInteger("sweep.limit", 500);
    private static final int PAGE_SIZE = Integer.getInteger("sweep.pageSize", 200);
    private static final int SAMPLE = Integer.getInteger("sweep.sample", 12);

    private final CriteriaSignalEvaluator evaluator = new CriteriaSignalEvaluator();
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    /** Bearer token, fetched once per run. */
    private String cachedToken;

    /** A realistic profile this sweep is measured against: ER+, PR-, HER2-, PIK3CA mutant. */
    private PatientDiagnosis diagnosis() {
        PatientDiagnosis d = new PatientDiagnosis();
        d.setErStatus("POSITIVE");
        d.setPrStatus("NEGATIVE");
        d.setHer2Status("NEGATIVE");
        return d;
    }

    /** PIK3CA detected; germline panel negative. */
    private PatientVariant variant() {
        PatientVariant v = new PatientVariant();
        v.setPik3caStatus("DETECTED");
        return v;
    }

    /** On a CDK4/6 inhibitor now without progression; PI3K-inhibitor naive; no cytotoxic chemo ever. */
    private PatientPriorTreatment treatment() {
        PatientPriorTreatment t = new PatientPriorTreatment();
        t.setCdk46Status("CURRENT");
        t.setPi3kAktMtorStatus("NEVER");
        return t;
    }

    @Test
    void sweepCorpusAndReportDistribution() throws Exception {
        if (!Boolean.getBoolean("sweep.enabled")) {
            System.out.println("CorpusSweep skipped. Enable with -Dsweep.enabled=true");
            return;
        }

        List<JsonNode> trials = fetchTrials();
        System.out.printf("%nSWEPT %d trials from %s%n", trials.size(), BASE);

        // signal name -> outcome -> count
        Map<String, Map<SignalOutcome, Integer>> tally = new LinkedHashMap<>();
        Map<String, Map<SignalOutcome, Integer>> gated = new LinkedHashMap<>();
        Map<String, List<String[]>> samples = new LinkedHashMap<>();
        int blankCriteria = 0;
        int unparsed = 0;
        int gatedTotal = 0;

        PatientDiagnosis dx = diagnosis();
        PatientVariant var = variant();
        PatientPriorTreatment tx = treatment();

        for (JsonNode node : trials) {
            String criteria = node.path("eligibilityCriteria").asText(null);
            if (criteria == null || criteria.isBlank()) {
                blankCriteria++;
                continue;
            }
            if (!hasSectionHeader(criteria)) {
                unparsed++;
            }

            Trial trial = new Trial();
            trial.setNctId(node.path("nctId").asText());
            trial.setEligibilityCriteria(criteria);

            trial.setBriefTitle(node.path("briefTitle").asText(null));
            trial.setOfficialTitle(node.path("officialTitle").asText(null));
            trial.setBriefSummary(node.path("briefSummary").asText(null));

            List<EligibilitySignal> signals = List.of(
                    evaluator.diseaseTypeSignal(trial),
                    evaluator.receptorSignal(trial, dx),
                    evaluator.treatmentLineSignal(trial, tx),
                    evaluator.pi3kSignal(trial, var, tx));

            boolean isBreast = signals.get(0).outcome() == SignalOutcome.PASS;
            if (isBreast) {
                gatedTotal++;
            }

            for (EligibilitySignal s : signals) {
                tally.computeIfAbsent(s.name(), k -> new TreeMap<>())
                        .merge(s.outcome(), 1, Integer::sum);
                if (isBreast) {
                    gated.computeIfAbsent(s.name(), k -> new TreeMap<>())
                            .merge(s.outcome(), 1, Integer::sum);
                }

                if (s.outcome() == SignalOutcome.CONCERN || s.outcome() == SignalOutcome.PASS) {
                    String key = s.name() + " / " + s.outcome();
                    List<String[]> bucket = samples.computeIfAbsent(key, k -> new ArrayList<>());
                    if (bucket.size() < SAMPLE) {
                        bucket.add(new String[]{trial.getNctId(), s.evidence(), s.detail()});
                    }
                }
            }
        }

        int assessed = trials.size() - blankCriteria;
        System.out.printf("blank criteria: %d    no inclusion/exclusion header: %d (%.1f%% of assessed)%n",
                blankCriteria, unparsed, pct(unparsed, assessed));

        System.out.println("\n=== OUTCOME DISTRIBUTION (" + assessed + " trials assessed) ===");
        System.out.printf("%-18s %10s %10s %10s %14s%n",
                "SIGNAL", "PASS", "CONCERN", "UNKNOWN", "NOT_APPLICABLE");
        for (var e : tally.entrySet()) {
            Map<SignalOutcome, Integer> m = e.getValue();
            System.out.printf("%-18s %10s %10s %10s %14s%n", e.getKey(),
                    fmt(m.get(SignalOutcome.PASS), assessed),
                    fmt(m.get(SignalOutcome.CONCERN), assessed),
                    fmt(m.get(SignalOutcome.UNKNOWN), assessed),
                    fmt(m.get(SignalOutcome.NOT_APPLICABLE), assessed));
        }

        // The distribution that matters once the gate is applied: how the other signals behave
        // on the trials actually worth ranking, rather than diluted by 54% other diseases.
        System.out.println("\n=== AFTER THE DISEASE GATE (breast-passing trials only) ===");
        System.out.printf("%-18s %10s %10s %10s %14s%n",
                "SIGNAL", "PASS", "CONCERN", "UNKNOWN", "NOT_APPLICABLE");
        for (var e : gated.entrySet()) {
            Map<SignalOutcome, Integer> m = e.getValue();
            System.out.printf("%-18s %10s %10s %10s %14s%n", e.getKey(),
                    fmt(m.get(SignalOutcome.PASS), gatedTotal),
                    fmt(m.get(SignalOutcome.CONCERN), gatedTotal),
                    fmt(m.get(SignalOutcome.UNKNOWN), gatedTotal),
                    fmt(m.get(SignalOutcome.NOT_APPLICABLE), gatedTotal));
        }

        System.out.println("\n=== SAMPLES FOR HAND-CHECKING ===");
        for (var e : samples.entrySet()) {
            System.out.println("\n--- " + e.getKey() + " ---");
            for (String[] row : e.getValue()) {
                System.out.printf("  %s%n      evidence: %s%n", row[0], trim(row[1]));
            }
        }
    }

    /** Mirrors the chunker's header test, for reporting the unparsed rate only. */
    private boolean hasSectionHeader(String criteria) {
        return criteria.lines().anyMatch(l ->
                l.strip().replaceAll("^\\**|\\**$", "").matches(
                        "(?i)\\s*(inclusion|exclusion)\\s+criteria\\s*:?\\s*"));
    }

    private String fmt(Integer n, int total) {
        int v = n == null ? 0 : n;
        return String.format("%d (%.1f%%)", v, pct(v, total));
    }

    private double pct(int n, int total) {
        return total == 0 ? 0 : (100.0 * n) / total;
    }

    private String trim(String s) {
        if (s == null) return "(none)";
        String one = s.replaceAll("\\s+", " ").strip();
        return one.length() <= 170 ? one : one.substring(0, 170) + "...";
    }

    /**
     * Logs in and caches the JWT.
     *
     * <p>Endpoint security is on, so an unauthenticated sweep now gets 401 on every page — which
     * would look like an empty corpus rather than a missing token. Credentials come from
     * {@code -Dsweep.username=} / {@code -Dsweep.password=}; nothing is committed.
     *
     * @return the token, or null when no password was supplied
     */
    private String token() {
        if (cachedToken != null) {
            return cachedToken;
        }
        String password = System.getProperty("sweep.password", "");
        if (password.isBlank()) {
            return null;
        }
        try {
            String username = System.getProperty("sweep.username", "jeb");
            String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
            HttpResponse<String> r = http.send(
                    HttpRequest.newBuilder(URI.create(BASE + "/api/auth/login"))
                            .header("Content-Type", "application/json")
                            .timeout(Duration.ofSeconds(30))
                            .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) {
                return null;
            }
            cachedToken = mapper.readTree(r.body()).path("token").asText(null);
            return cachedToken;
        } catch (Exception e) {
            return null;
        }
    }

    private List<JsonNode> fetchTrials() throws Exception {
        List<JsonNode> out = new ArrayList<>();
        int page = 0;
        while (out.size() < LIMIT) {
            String url = BASE + "/api/trial?page=" + page + "&size=" + PAGE_SIZE;
            HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60));
            String t = token();
            if (t != null) {
                req.header("Authorization", "Bearer " + t);
            }
            HttpResponse<String> res = http.send(
                    req.GET().build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 401 || res.statusCode() == 403) {
                throw new IllegalStateException("GET " + url + " returned " + res.statusCode()
                        + " - endpoint security is on and this sweep has no token. Pass"
                        + " -Dsweep.password=... (and -Dsweep.username= if not 'jeb').");
            }
            if (res.statusCode() != 200) {
                throw new IllegalStateException("GET " + url + " returned " + res.statusCode());
            }
            JsonNode content = mapper.readTree(res.body()).path("content");
            if (!content.isArray() || content.isEmpty()) {
                break;
            }
            content.forEach(out::add);
            page++;
            if (page % 5 == 0) {
                System.out.printf("  fetched %d...%n", out.size());
            }
        }
        return out.size() > LIMIT ? out.subList(0, LIMIT) : out;
    }
}
