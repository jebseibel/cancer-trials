package com.seibel.cancer.common.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * What one login may do with one patient's record.
 *
 * <p>An enum rather than a bare string because these values are compared in authorisation
 * logic, where a typo in a string comparison fails <em>open</em>. The database column stays
 * {@code varchar(24)}, consistent with the rest of this schema.
 *
 * <p><strong>The levels are ranked, and {@link #covers} is the only correct way to compare
 * them.</strong> A grant satisfies a requirement when it ranks at or above it, so an OWNER
 * passes a VIEW_TRIALS check without needing every call site to enumerate the levels that
 * qualify.
 *
 * <p>{@code VIEW_TRIALS} exists because it is the level most family members actually want:
 * someone can help hunt for trials without reading a diagnosis or a genomic report. That
 * distinction is the reason this is not a boolean.
 *
 * <p>Design and rationale in {@code .claude/access/PATIENT_ACCESS_PLAN.md}.
 */
public enum AccessLevel {

    /** Ranked trial results and saved trials. NOT the diagnosis, variants or treatment history. */
    VIEW_TRIALS(10),

    /** The full clinical record, read-only. */
    VIEW_RECORD(20),

    /** The full clinical record, and may change it. */
    EDIT_RECORD(30),

    /** Everything, plus granting and revoking access. Created alongside the patient. */
    OWNER(40);

    public final int rank;

    AccessLevel(int rank) {
        this.rank = rank;
    }

    /**
     * True when a grant at this level satisfies a requirement for {@code required}.
     *
     * <p>Ranked comparison, not equality: an OWNER covers VIEW_TRIALS.
     */
    public boolean covers(AccessLevel required) {
        return required != null && this.rank >= required.rank;
    }

    public boolean isOwner() {
        return this == OWNER;
    }

    /** True when this level permits changing the record, as opposed to only reading it. */
    public boolean canEdit() {
        return this.rank >= EDIT_RECORD.rank;
    }

    public static final String ALLOWED_VALUES =
            Arrays.stream(values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
}
