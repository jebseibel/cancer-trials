package com.seibel.cancer.common.domain;

import com.seibel.cancer.common.enums.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * A grant: which login may see which patient, and at what level.
 *
 * <p>This is the join that {@code user} and {@code app_user} never had - they were reconciled
 * by string-matching usernames, which is why a patient could not be shared and why a record
 * could not belong to someone who does not log in.
 *
 * <p><strong>Ownership is a grant, not a column on the patient.</strong> An {@code OWNER} row
 * here means "my own record" is not a special case in any query or check - creating a record
 * and being given access to someone else's are the same mechanism.
 *
 * <p><strong>Revoke by setting {@code revokedAt}, never by deleting the row.</strong> Who had
 * access to a medical record, and when, is exactly the history worth keeping.
 *
 * <p>Design and rationale in {@code .claude/access/PATIENT_ACCESS_PLAN.md}.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserPatient extends BaseDomain {

    /** FK -&gt; user.id, the login receiving access. */
    private Long userId;

    /** FK -&gt; patient.id, the record being shared. */
    private Long patientId;

    private AccessLevel accessLevel;

    /** FK -&gt; user.id, who granted it. Kept for audit rather than for authorisation. */
    private Long grantedByUserId;

    private LocalDateTime grantedAt;

    /** Null means active. Set to end access; the row itself is never deleted. */
    private LocalDateTime revokedAt;

    /** The owner's own words, e.g. "my sister". */
    private String note;

    /** True when this grant is currently in force. */
    public boolean isActiveGrant() {
        return revokedAt == null;
    }
}
