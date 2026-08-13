package com.seibel.cancer.web.response;

import com.seibel.cancer.common.enums.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * A patient the caller may see, together with the level they hold on it.
 *
 * <p>The level is on the response deliberately: without it the frontend cannot know whether to
 * render an editable form or a read-only view, and would have to discover a refusal by
 * attempting a save and failing.
 */
@Data
@Builder
public class ResponsePatientAccess {

    private String extid;
    private String displayName;
    private String fullName;
    private LocalDate dateOfBirth;
    private String sex;
    private String notes;

    /** What this caller may do with this record. Never another user's level. */
    private AccessLevel accessLevel;
}
