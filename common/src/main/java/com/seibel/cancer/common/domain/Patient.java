package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * A person with a medical record - the subject a trial search is <em>for</em>.
 *
 * <p><strong>A patient does not log in.</strong> There are deliberately no credential fields
 * here: logging in is what {@code User} is for, and which logins may see which patients is
 * recorded in {@code user_patient} grants. That separation is what lets a record be created
 * for someone who never signs in - the common case, since the person entering the data is
 * often not the person the data is about.
 *
 * <p>This replaces the former {@code AppUser}, which carried its own {@code username} and
 * {@code password_hash} and was therefore a second login table wearing a patient's clothes.
 *
 * <p>{@code dateOfBirth} and {@code sex} live here rather than on {@code PatientDiagnosis}
 * because they are properties of a person, not of a diagnosis - and a diagnosis that ever
 * becomes append-only history would duplicate them per row.
 *
 * <p>Design and rationale in {@code .claude/access/PATIENT_ACCESS_PLAN.md}.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Patient extends BaseDomain {
    /** What appears in the patient switcher - short, e.g. "Alex". */
    private String displayName;

    /**
     * The patient's full legal name, as it appears on medical records.
     *
     * <p>Separate from {@code displayName} because they serve different jobs: a switcher and
     * a page header want something short, while matching a trial site's records, or an
     * enquiry to a study coordinator, needs the name the clinic holds.
     */
    private String fullName;
    private LocalDate dateOfBirth;
    private String sex;
    /** Non-clinical context - relationship to the owner, and similar. */
    private String notes;
}
