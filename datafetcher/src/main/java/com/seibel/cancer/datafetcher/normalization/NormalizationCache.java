package com.seibel.cancer.datafetcher.normalization;

import com.seibel.cancer.common.domain.Condition;
import com.seibel.cancer.common.domain.Sponsor;
import com.seibel.cancer.common.domain.TrialSource;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Per-run lookup cache for normalization, holding the values that were being re-fetched on every
 * staging row.
 *
 * <p>Measured on a 736-row run at 349ms per trial and roughly 38 database round trips each:
 * conditions and sponsors accounted for ~3.9 {@code findByName} calls per trial, and the
 * {@code TrialSource} was fetched once per row despite being the same row throughout. After the
 * first hundred trials almost every name lookup is a repeat hit on the same small set of values.
 *
 * <p><b>Deliberately not a Spring {@code @Cacheable} bean.</b> Scoped to a single
 * {@code normalizePending()} call, so it cannot go stale between runs and nothing outside the loop
 * can observe it. Not thread-safe, and does not need to be - normalization is sequential.
 *
 * <p>A negative result is cached too (as null), so a name that does not exist is only looked up
 * once per run rather than on every trial that mentions it.
 */
class NormalizationCache {

    private final Map<Long, TrialSource> trialSources = new HashMap<>();
    private final Map<String, Condition> conditionsByName = new HashMap<>();
    private final Map<String, Sponsor> sponsorsByName = new HashMap<>();

    private int hits;
    private int misses;

    TrialSource trialSource(Long id, Function<Long, TrialSource> loader) {
        return cached(trialSources, id, () -> loader.apply(id));
    }

    Condition condition(String name, Function<String, Condition> loader) {
        return cached(conditionsByName, name, () -> loader.apply(name));
    }

    /** Records a condition created during this run so the next trial mentioning it does not re-query. */
    void putCondition(String name, Condition condition) {
        conditionsByName.put(name, condition);
    }

    Sponsor sponsor(String name, Function<String, Sponsor> loader) {
        return cached(sponsorsByName, name, () -> loader.apply(name));
    }

    /** Records a sponsor created during this run so the next trial mentioning it does not re-query. */
    void putSponsor(String name, Sponsor sponsor) {
        sponsorsByName.put(name, sponsor);
    }

    private <K, V> V cached(Map<K, V> map, K key, Supplier<V> loader) {
        if (map.containsKey(key)) {
            hits++;
            return map.get(key);
        }
        misses++;
        V value = loader.get();
        // Cache negative results too - a name that does not exist should be queried once per run,
        // not once per trial that mentions it.
        map.put(key, value);
        return value;
    }

    /** For logging how much work the cache avoided. */
    String stats() {
        return "cacheHits=" + hits + ", cacheMisses=" + misses;
    }
}
