package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.UserPatient;
import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.PatientDb;
import com.seibel.cancer.database.db.entity.UserDb;
import com.seibel.cancer.database.db.repository.PatientRepository;
import com.seibel.cancer.database.db.repository.UserRepository;
import com.seibel.cancer.database.db.service.UserPatientDbService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * The authorisation rule, which is the reason this class exists.
 *
 * <p>Before it, every patient endpoint took its target from the URL and compared it to nobody -
 * any authenticated caller could read any patient's record by passing their extid. The negative
 * cases below are therefore the point of this suite, not an afterthought: each one is a way
 * that hole could reopen.
 *
 * <p><strong>Every refusal must be a 404.</strong> A 403 would confirm the extid names a real
 * person's record, which is a disclosure in itself.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CurrentUserServiceTest {

    private static final String USERNAME = "jeb";
    private static final Long USER_ID = 4001L;
    private static final Long PATIENT_ID = 5001L;
    private static final String PATIENT_EXTID = "patient-extid-1";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserPatientDbService userPatientDbService;

    private CurrentUserService service;

    @BeforeEach
    void setUp() {
        service = new CurrentUserService(userRepository, patientRepository, userPatientDbService);
        signIn(USERNAME);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void signIn(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "n/a",
                        AuthorityUtils.createAuthorityList("ROLE_USER")));
        lenient().when(userRepository.findByUsername(username)).thenReturn(Optional.of(user(username)));
    }

    private UserDb user(String username) {
        UserDb u = new UserDb();
        u.setId(USER_ID);
        u.setUsername(username);
        return u;
    }

    private PatientDb patient() {
        PatientDb p = new PatientDb();
        p.setId(PATIENT_ID);
        p.setExtid(PATIENT_EXTID);
        p.setDisplayName("Alex");
        return p;
    }

    private UserPatient grant(AccessLevel level) {
        return UserPatient.builder()
                .userId(USER_ID)
                .patientId(PATIENT_ID)
                .accessLevel(level)
                .build();
    }

    @Nested
    @DisplayName("resolving the caller")
    class ResolvingTheCaller {

        @Test
        void readsTheUsernameFromTheSecurityContext() {
            assertThat(service.currentUsername()).contains(USERNAME);
        }

        @Test
        void hasNoUsernameWhenNobodyIsSignedIn() {
            SecurityContextHolder.clearContext();

            assertThat(service.currentUsername()).isEmpty();
        }

        /** Spring treats the anonymous principal as "authenticated". It is not a user. */
        @Test
        void treatsTheAnonymousPrincipalAsNoUser() {
            SecurityContextHolder.getContext().setAuthentication(
                    new AnonymousAuthenticationToken("key", "anonymousUser",
                            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

            assertThat(service.currentUsername()).isEmpty();
        }

        @Test
        void refusesWhenThereIsNoAuthenticatedCaller() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> service.requireCurrentUser())
                    .isInstanceOf(ServiceException.class);
        }

        /** A valid token for a deleted account: stateless tokens cannot be recalled. */
        @Test
        void refusesWhenTheTokenNamesAUserThatNoLongerExists() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.requireCurrentUser())
                    .isInstanceOf(ServiceException.class);
        }
    }

    @Nested
    @DisplayName("requireAccess refuses with a 404")
    class RequireAccessRefusals {

        @Test
        void whenThePatientDoesNotExist() {
            when(patientRepository.findByExtid(PATIENT_EXTID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.requireAccess(PATIENT_EXTID, AccessLevel.VIEW_TRIALS))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void whenTheCallerHasNoGrant() {
            when(patientRepository.findByExtid(PATIENT_EXTID)).thenReturn(Optional.of(patient()));
            when(userPatientDbService.findActiveGrant(USER_ID, PATIENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.requireAccess(PATIENT_EXTID, AccessLevel.VIEW_TRIALS))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void whenTheGrantIsBelowTheRequiredLevel() {
            when(patientRepository.findByExtid(PATIENT_EXTID)).thenReturn(Optional.of(patient()));
            when(userPatientDbService.findActiveGrant(USER_ID, PATIENT_ID))
                    .thenReturn(Optional.of(grant(AccessLevel.VIEW_TRIALS)));

            assertThatThrownBy(() -> service.requireAccess(PATIENT_EXTID, AccessLevel.EDIT_RECORD))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        /**
         * The message must not distinguish "no such patient" from "not allowed". The 404 is
         * chosen so probing reveals nothing, and the handler puts this message in the body -
         * so a message that differed would leak exactly what the status code hides.
         */
        @Test
        void withAMessageThatCannotDistinguishMissingFromForbidden() {
            when(patientRepository.findByExtid(PATIENT_EXTID)).thenReturn(Optional.empty());
            String whenMissing = catchMessage();

            when(patientRepository.findByExtid(PATIENT_EXTID)).thenReturn(Optional.of(patient()));
            when(userPatientDbService.findActiveGrant(USER_ID, PATIENT_ID)).thenReturn(Optional.empty());
            String whenForbidden = catchMessage();

            assertThat(whenMissing).isEqualTo(whenForbidden);
            assertThat(whenMissing).doesNotContain(PATIENT_EXTID);
        }

        private String catchMessage() {
            try {
                service.requireAccess(PATIENT_EXTID, AccessLevel.VIEW_TRIALS);
                throw new AssertionError("expected a refusal");
            } catch (ResourceNotFoundException e) {
                return e.getMessage();
            }
        }
    }

    @Nested
    @DisplayName("requireAccess permits")
    class RequireAccessPermits {

        @Test
        void whenTheGrantMatchesTheRequiredLevel() {
            when(patientRepository.findByExtid(PATIENT_EXTID)).thenReturn(Optional.of(patient()));
            when(userPatientDbService.findActiveGrant(USER_ID, PATIENT_ID))
                    .thenReturn(Optional.of(grant(AccessLevel.VIEW_RECORD)));

            assertThat(service.requireAccess(PATIENT_EXTID, AccessLevel.VIEW_RECORD).getExtid())
                    .isEqualTo(PATIENT_EXTID);
        }

        /** Ranked, not equal: an OWNER passes every check without enumerating the levels. */
        @ParameterizedTest
        @EnumSource(AccessLevel.class)
        void whenTheCallerIsOwner_regardlessOfWhatIsRequired(AccessLevel required) {
            when(patientRepository.findByExtid(PATIENT_EXTID)).thenReturn(Optional.of(patient()));
            when(userPatientDbService.findActiveGrant(USER_ID, PATIENT_ID))
                    .thenReturn(Optional.of(grant(AccessLevel.OWNER)));

            assertThat(service.requireAccess(PATIENT_EXTID, required)).isNotNull();
        }

        @Test
        void andReturnsTheNumericIdForFkLookups() {
            when(patientRepository.findByExtid(PATIENT_EXTID)).thenReturn(Optional.of(patient()));
            when(userPatientDbService.findActiveGrant(USER_ID, PATIENT_ID))
                    .thenReturn(Optional.of(grant(AccessLevel.OWNER)));

            assertThat(service.requireAccessId(PATIENT_EXTID, AccessLevel.VIEW_TRIALS))
                    .isEqualTo(PATIENT_ID);
        }
    }

    @Nested
    @DisplayName("myPatients")
    class MyPatients {

        @Test
        void isEmptyWhenTheCallerHoldsNoGrants() {
            when(userPatientDbService.findActiveGrantsForUser(USER_ID)).thenReturn(List.of());

            assertThat(service.myPatients()).isEmpty();
        }

        @Test
        void returnsOnlyGrantedPatients_withTheLevelHeld() {
            when(userPatientDbService.findActiveGrantsForUser(USER_ID))
                    .thenReturn(List.of(grant(AccessLevel.VIEW_TRIALS)));
            when(patientRepository.findAllById(List.of(PATIENT_ID))).thenReturn(List.of(patient()));

            List<CurrentUserService.PatientAccess> result = service.myPatients();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).patient().getExtid()).isEqualTo(PATIENT_EXTID);
            assertThat(result.get(0).accessLevel()).isEqualTo(AccessLevel.VIEW_TRIALS);
        }

        /** Best access first, so a single-patient user lands on their own record. */
        @Test
        void ordersByAccessLevelDescending() {
            UserPatient owned = UserPatient.builder()
                    .userId(USER_ID).patientId(PATIENT_ID).accessLevel(AccessLevel.VIEW_TRIALS).build();
            UserPatient mine = UserPatient.builder()
                    .userId(USER_ID).patientId(9002L).accessLevel(AccessLevel.OWNER).build();

            PatientDb other = new PatientDb();
            other.setId(9002L);
            other.setExtid("patient-extid-2");
            other.setDisplayName("Me");

            when(userPatientDbService.findActiveGrantsForUser(USER_ID)).thenReturn(List.of(owned, mine));
            when(patientRepository.findAllById(List.of(PATIENT_ID, 9002L)))
                    .thenReturn(List.of(patient(), other));

            List<CurrentUserService.PatientAccess> result = service.myPatients();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).accessLevel()).isEqualTo(AccessLevel.OWNER);
        }

        /** One broken grant must not make the whole list unreadable. */
        @Test
        void skipsAGrantPointingAtAMissingPatient() {
            when(userPatientDbService.findActiveGrantsForUser(USER_ID))
                    .thenReturn(List.of(grant(AccessLevel.OWNER)));
            when(patientRepository.findAllById(List.of(PATIENT_ID))).thenReturn(List.of());

            assertThat(service.myPatients()).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasAccess")
    class HasAccess {

        @Test
        void isFalseWhenThereIsNoGrant() {
            when(patientRepository.findByExtid(PATIENT_EXTID)).thenReturn(Optional.of(patient()));
            when(userPatientDbService.findActiveGrant(USER_ID, PATIENT_ID)).thenReturn(Optional.empty());

            assertThat(service.hasAccess(PATIENT_EXTID, AccessLevel.VIEW_TRIALS)).isFalse();
        }

        @Test
        void isFalseWhenThePatientDoesNotExist() {
            when(patientRepository.findByExtid(PATIENT_EXTID)).thenReturn(Optional.empty());

            assertThat(service.hasAccess(PATIENT_EXTID, AccessLevel.VIEW_TRIALS)).isFalse();
        }

        @Test
        void isTrueWhenTheGrantCoversTheRequestedLevel() {
            when(patientRepository.findByExtid(PATIENT_EXTID)).thenReturn(Optional.of(patient()));
            when(userPatientDbService.findActiveGrant(USER_ID, PATIENT_ID))
                    .thenReturn(Optional.of(grant(AccessLevel.EDIT_RECORD)));

            assertThat(service.hasAccess(PATIENT_EXTID, AccessLevel.VIEW_RECORD)).isTrue();
        }

        /** Does not throw - it is for rendering decisions, not for guarding. */
        @Test
        void doesNotThrowWhenRefused() {
            when(patientRepository.findByExtid(PATIENT_EXTID)).thenReturn(Optional.empty());

            assertThat(service.hasAccess(PATIENT_EXTID, AccessLevel.OWNER)).isFalse();
        }
    }
}
