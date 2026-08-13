package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.Patient;
import com.seibel.cancer.common.domain.UserPatient;
import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.PatientDb;
import com.seibel.cancer.database.db.entity.UserDb;
import com.seibel.cancer.database.db.repository.PatientRepository;
import com.seibel.cancer.database.db.repository.UserRepository;
import com.seibel.cancer.database.db.service.UserPatientDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves who is calling, and what they are allowed to see.
 *
 * <p><strong>The rule this class exists to enforce: the patient is never taken from the
 * request. It is resolved from the JWT, then authorised.</strong> Before this class, every
 * patient endpoint took its target from the URL and compared it to nobody - any authenticated
 * caller could read any patient's record by passing their extid.
 *
 * <p>This is the <em>only</em> place that authorisation decision is made. Scattering
 * {@code SecurityContextHolder} reads across controllers is how the sixth endpoint ends up
 * missing the check.
 *
 * <p>Design and rationale in {@code .claude/access/PATIENT_ACCESS_PLAN.md}.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class CurrentUserService extends BaseService {

    /**
     * The message returned whenever a patient cannot be reached - whether it does not exist,
     * or exists and the caller has no grant to it.
     *
     * <p><strong>It must stay identical across both cases.</strong> The 404 is chosen over a
     * 403 precisely so that probing an extid reveals nothing, and
     * {@code GlobalExceptionHandler} puts the exception message in the response body - so a
     * message that distinguished the two would leak exactly what the status code hides.
     */
    private static final String NOT_FOUND_IDENTIFIER = "not found";

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final UserPatientDbService userPatientDbService;

    public CurrentUserService(UserRepository userRepository,
                              PatientRepository patientRepository,
                              UserPatientDbService userPatientDbService) {
        super(CurrentUserService.class.getSimpleName());
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.userPatientDbService = userPatientDbService;
    }

    /**
     * The username on the current request, from the token.
     *
     * <p>Empty when there is no authenticated caller. In normal operation the filter chain
     * rejects those with a 401 long before a service runs, so an empty result here means
     * either a permitted endpoint or a misconfiguration - never a caller to be trusted.
     */
    public Optional<String> currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return Optional.empty();
        }
        // The anonymous principal is "authenticated" in Spring's terms. It is not a user.
        if ("anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }
        return Optional.of(auth.getName());
    }

    /** The calling user, or a failure if there is no authenticated caller. */
    public UserDb requireCurrentUser() {
        String username = currentUsername()
                .orElseThrow(() -> new ServiceException("No authenticated user on this request"));

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    // A valid token for a username with no row: the account was removed after
                    // the token was issued. Tokens are stateless and cannot be recalled.
                    log.warn("Authenticated username={} has no user row", username);
                    return new ServiceException("Authenticated user no longer exists");
                });
    }

    /** Every patient the calling user may currently see, with the level they hold on each. */
    public List<PatientAccess> myPatients() {
        UserDb user = requireCurrentUser();
        return patientsFor(user);
    }

    /**
     * Every patient one user may see, best access first.
     *
     * <p>Ordered by level descending so a caller's own record - the OWNER grant - leads the
     * list, which is what a single-patient user should land on without choosing.
     */
    public List<PatientAccess> patientsFor(UserDb user) {
        requireNonNull(user, "user");

        List<UserPatient> grants = userPatientDbService.findActiveGrantsForUser(user.getId());
        if (grants.isEmpty()) {
            return List.of();
        }

        // One query for every granted patient rather than one per grant.
        Map<Long, PatientDb> byId = patientRepository.findAllById(
                        grants.stream().map(UserPatient::getPatientId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(PatientDb::getId, Function.identity()));

        return grants.stream()
                .map(g -> {
                    PatientDb p = byId.get(g.getPatientId());
                    if (p == null) {
                        // A grant pointing at a missing patient. Skipped rather than thrown:
                        // one broken grant must not make the whole list unreadable.
                        log.warn("Grant extid={} points at missing patientId={}",
                                g.getExtid(), g.getPatientId());
                        return null;
                    }
                    return new PatientAccess(toDomain(p), g.getAccessLevel());
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(
                        (PatientAccess pa) -> pa.accessLevel().rank).reversed())
                .toList();
    }

    /**
     * The level the calling user holds on one patient, if any.
     *
     * <p>Empty covers both "no such patient" and "no grant" - the caller cannot tell them
     * apart, which is the point.
     */
    public Optional<AccessLevel> accessLevelFor(String patientExtid) {
        requireNonBlank(patientExtid, "patientExtid");

        UserDb user = requireCurrentUser();
        Optional<PatientDb> patient = patientRepository.findByExtid(patientExtid);
        if (patient.isEmpty()) {
            return Optional.empty();
        }

        return userPatientDbService.findActiveGrant(user.getId(), patient.get().getId())
                .map(UserPatient::getAccessLevel);
    }

    /**
     * Assert the calling user may act on this patient at {@code required} or above, and
     * return the patient.
     *
     * <p><strong>Throws {@link ResourceNotFoundException} - a 404 - in every failure case:</strong>
     * no such patient, no grant, or a grant at too low a level. A 403 would confirm the extid
     * names a real person's record, which is a disclosure in itself.
     */
    public Patient requireAccess(String patientExtid, AccessLevel required) {
        requireNonBlank(patientExtid, "patientExtid");
        requireNonNull(required, "required");

        UserDb user = requireCurrentUser();

        PatientDb patient = patientRepository.findByExtid(patientExtid)
                .orElseThrow(() -> {
                    log.info("Access denied: user={} requested unknown patient", user.getUsername());
                    return notFound();
                });

        AccessLevel held = userPatientDbService
                .findActiveGrant(user.getId(), patient.getId())
                .map(UserPatient::getAccessLevel)
                .orElseThrow(() -> {
                    log.info("Access denied: user={} has no grant to patientId={}",
                            user.getUsername(), patient.getId());
                    return notFound();
                });

        if (!held.covers(required)) {
            log.info("Access denied: user={} holds {} on patientId={}, needs {}",
                    user.getUsername(), held, patient.getId(), required);
            throw notFound();
        }

        return toDomain(patient);
    }

    /**
     * The numeric id of a patient the caller may act on - what the repointed FK finders in
     * steps 4-5 need, so a controller never resolves an extid to an id unauthorised.
     */
    public Long requireAccessId(String patientExtid, AccessLevel required) {
        return requireAccess(patientExtid, required).getId();
    }

    /** True when the caller may act at this level, without throwing. For rendering decisions. */
    public boolean hasAccess(String patientExtid, AccessLevel required) {
        return accessLevelFor(patientExtid).map(held -> held.covers(required)).orElse(false);
    }

    private ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Patient", NOT_FOUND_IDENTIFIER);
    }

    private Patient toDomain(PatientDb item) {
        return Patient.builder()
                .id(item.getId())
                .extid(item.getExtid())
                .displayName(item.getDisplayName())
                .fullName(item.getFullName())
                .dateOfBirth(item.getDateOfBirth())
                .sex(item.getSex())
                .notes(item.getNotes())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .deletedAt(item.getDeletedAt())
                .active(item.getActive())
                .build();
    }

    /** A patient together with the level the calling user holds on it. */
    public record PatientAccess(Patient patient, AccessLevel accessLevel) {
    }
}
