package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.service.CurrentUserService;
import com.seibel.cancer.service.ai.PhiLineScanResult;
import com.seibel.cancer.service.ai.intake.DiagnosisIntakeClarification;
import com.seibel.cancer.service.ai.intake.DiagnosisIntakeExtraction;
import com.seibel.cancer.service.ai.intake.DiagnosisIntakeExtractionService;
import com.seibel.cancer.service.ai.intake.DiagnosisIntakeSession;
import com.seibel.cancer.service.ai.intake.DiagnosisIntakeSessionStore;
import com.seibel.cancer.service.ai.intake.DiagnosisIntakeUpload;
import com.seibel.cancer.web.request.RequestDiagnosisIntakeAnswer;
import com.seibel.cancer.web.request.RequestDiagnosisIntakeStart;
import com.seibel.cancer.web.response.ResponseDiagnosisIntakeSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * AI-assisted document intake: upload/paste text, get a prefilled diagnosis/variant/prior
 * treatment draft, answer a short back-and-forth for anything important that is missing.
 *
 * <p>Nothing here is persisted. See {@link DiagnosisIntakeSession}'s javadoc for what that
 * means concretely, and {@link DiagnosisIntakeExtractionService} for the PHI gate every
 * uploaded document passes through before any AI call is made.
 */
@RestController
@RequestMapping("/api/diagnosisintake")
@Validated
@Tag(name = "DiagnosisIntake", description = "AI-assisted document intake for diagnosis/variant/prior-treatment drafts")
@RequiredArgsConstructor
public class DiagnosisIntakeController {

    private final DiagnosisIntakeExtractionService extractionService;
    private final DiagnosisIntakeSessionStore sessionStore;
    private final CurrentUserService currentUserService;

    @PostMapping("/start")
    @Operation(summary = "Start a diagnosis intake session from uploaded/pasted text")
    public ResponseEntity<ResponseDiagnosisIntakeSession> start(
            @Valid @RequestBody RequestDiagnosisIntakeStart request) {
        currentUserService.requireAccessId(request.getPatientExtid(), AccessLevel.EDIT_RECORD);
        Long userId = currentUserService.requireCurrentUser().getId();

        DiagnosisIntakeUpload upload = extractionService.extract(request.getDocumentText());
        List<String> missing = extractionService.missingRequired(upload.draft());

        DiagnosisIntakeSession session = sessionStore.create(
                userId, request.getPatientExtid(), upload.draft(), missing);

        return ResponseEntity.ok(
                toResponse(session, firstQuestionFor(missing), upload.excludedLines()));
    }

    @PostMapping("/{sessionId}/answer")
    @Operation(summary = "Submit the user's answer to the current clarifying question")
    public ResponseDiagnosisIntakeSession answer(
            @PathVariable String sessionId,
            @Valid @RequestBody RequestDiagnosisIntakeAnswer request) {
        Long userId = currentUserService.requireCurrentUser().getId();
        DiagnosisIntakeSession session = requireSession(sessionId, userId);

        String latestQuestion = session.getTranscript().isEmpty()
                ? firstQuestionFor(session.getMissingRequiredFields())
                : session.getTranscript().get(session.getTranscript().size() - 1).question();

        DiagnosisIntakeClarification result = extractionService.clarify(
                session.getDraft(), session.getMissingRequiredFields(),
                latestQuestion, request.getAnswerText());

        List<String> stillMissing = extractionService.missingRequired(result.getUpdatedDraft());

        session.setDraft(result.getUpdatedDraft());
        session.setMissingRequiredFields(stillMissing);
        session.getTranscript().add(new DiagnosisIntakeSession.ConversationTurn(
                latestQuestion, request.getAnswerText(), Instant.now()));
        session.setTurnCount(session.getTurnCount() + 1);
        session.setStatus(Boolean.TRUE.equals(result.getDone()) || stillMissing.isEmpty()
                ? DiagnosisIntakeSession.SessionStatus.COMPLETE
                : DiagnosisIntakeSession.SessionStatus.AWAITING_ANSWER);

        sessionStore.save(session);
        return toResponse(session, result.getNextQuestion());
    }

    @PostMapping("/{sessionId}/skip")
    @Operation(summary = "Give up on remaining required fields and finalize the draft as-is")
    public ResponseDiagnosisIntakeSession skip(@PathVariable String sessionId) {
        Long userId = currentUserService.requireCurrentUser().getId();
        DiagnosisIntakeSession session = requireSession(sessionId, userId);

        session.setStatus(DiagnosisIntakeSession.SessionStatus.COMPLETE);
        sessionStore.save(session);
        return toResponse(session, null);
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Cancel/abandon a diagnosis intake session")
    public ResponseEntity<Void> cancel(@PathVariable String sessionId) {
        Long userId = currentUserService.requireCurrentUser().getId();
        sessionStore.remove(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    private DiagnosisIntakeSession requireSession(String sessionId, Long userId) {
        return sessionStore.get(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("DiagnosisIntakeSession", "not found"));
    }

    /** Deterministic phrasing for the very first question, so /start needs only one AI call. */
    private String firstQuestionFor(List<String> missing) {
        if (missing.isEmpty()) return null;
        return switch (missing.get(0)) {
            case "cancerType" -> "What type of cancer does this describe (e.g. invasive ductal carcinoma of the breast)?";
            case "stage" -> "What stage is it? (e.g. Stage II, Stage IV)";
            case "erStatus" -> "Is the tumor ER (estrogen receptor) positive, negative, or not tested?";
            case "prStatus" -> "Is the tumor PR (progesterone receptor) positive, negative, or not tested?";
            case "her2Status" -> "Is the tumor HER2 positive, negative, or not tested?";
            case "ecogStatus" -> "What is the ECOG performance status, 0 through 4, if known?";
            default -> "Can you provide more detail on " + missing.get(0) + "?";
        };
    }

    /** Used by {@code /answer} and {@code /skip} - neither turn goes through the PHI line scan,
     * so there is never anything to report and the excluded-lines list stays at its empty
     * default. */
    private ResponseDiagnosisIntakeSession toResponse(DiagnosisIntakeSession session, String nextQuestion) {
        return toResponse(session, nextQuestion, List.of());
    }

    private ResponseDiagnosisIntakeSession toResponse(DiagnosisIntakeSession session,
            String nextQuestion, List<PhiLineScanResult.ExcludedLine> excludedLines) {
        DiagnosisIntakeExtraction d = session.getDraft();
        return ResponseDiagnosisIntakeSession.builder()
                .sessionId(session.getSessionId())
                .status(session.getStatus().name())
                .draftDiagnosis(ResponseDiagnosisIntakeSession.DraftDiagnosis.builder()
                        .cancerType(d.getCancerType())
                        .stage(d.getStage())
                        .stageSystem(d.getStageSystem())
                        .isMetastatic(d.getIsMetastatic())
                        .metastasisSites(d.getMetastasisSites())
                        .erStatus(d.getErStatus())
                        .prStatus(d.getPrStatus())
                        .her2Status(d.getHer2Status())
                        .biomarkers(d.getBiomarkers())
                        .ecogStatus(d.getEcogStatus())
                        .priorChemoRegimens(d.getPriorChemoRegimens())
                        .lastChemoEndDate(d.getLastChemoEndDate())
                        .priorTreatments(d.getPriorTreatments())
                        .hasMeasurableDisease(d.getHasMeasurableDisease())
                        .menopausalStatus(d.getMenopausalStatus())
                        .diagnosisDate(d.getDiagnosisDate())
                        .build())
                .draftVariant(ResponseDiagnosisIntakeSession.DraftVariant.builder()
                        .pik3caStatus(d.getPik3caStatus())
                        .esr1Status(d.getEsr1Status())
                        .tp53Status(d.getTp53Status())
                        .akt1Status(d.getAkt1Status())
                        .ptenStatus(d.getPtenStatus())
                        .erbb2SomaticStatus(d.getErbb2SomaticStatus())
                        .brca1Status(d.getBrca1Status())
                        .brca2Status(d.getBrca2Status())
                        .palb2Status(d.getPalb2Status())
                        .atmStatus(d.getAtmStatus())
                        .chek2Status(d.getChek2Status())
                        .hrdStatus(d.getHrdStatus())
                        .pdl1Status(d.getPdl1Status())
                        .ki67Percent(d.getKi67Percent())
                        .germlineTestDone(d.getGermlineTestDone())
                        .somaticTestDone(d.getSomaticTestDone())
                        .testDate(d.getTestDate())
                        .otherVariants(d.getOtherVariants())
                        .build())
                .draftPriorTreatment(ResponseDiagnosisIntakeSession.DraftPriorTreatment.builder()
                        .cdk46Status(d.getCdk46Status())
                        .endocrineStatus(d.getEndocrineStatus())
                        .serdStatus(d.getSerdStatus())
                        .chemoStatus(d.getChemoStatus())
                        .her2TherapyStatus(d.getHer2TherapyStatus())
                        .her2AdcStatus(d.getHer2AdcStatus())
                        .trop2AdcStatus(d.getTrop2AdcStatus())
                        .parpStatus(d.getParpStatus())
                        .pi3kAktMtorStatus(d.getPi3kAktMtorStatus())
                        .immunotherapyStatus(d.getImmunotherapyStatus())
                        .taxaneStatus(d.getTaxaneStatus())
                        .anthracyclineStatus(d.getAnthracyclineStatus())
                        .platinumStatus(d.getPlatinumStatus())
                        .currentDrugNames(d.getCurrentDrugNames())
                        .priorDrugNames(d.getPriorDrugNames())
                        .linesOfTherapyMetastatic(d.getLinesOfTherapyMetastatic())
                        .hadNeoadjuvant(d.getHadNeoadjuvant())
                        .hadAdjuvant(d.getHadAdjuvant())
                        .hadRadiation(d.getHadRadiation())
                        .hadSurgery(d.getHadSurgery())
                        .currentlyOnTreatment(d.getCurrentlyOnTreatment())
                        .lastTreatmentEndDate(d.getLastTreatmentEndDate())
                        .otherTreatments(d.getOtherTreatments())
                        .build())
                .missingRequiredFields(session.getMissingRequiredFields())
                .nextQuestion(nextQuestion)
                .turnCount(session.getTurnCount())
                .excludedLines(excludedLines.stream()
                        .map(l -> ResponseDiagnosisIntakeSession.ExcludedLine.builder()
                                .lineNumber(l.lineNumber())
                                .reasons(l.reasons())
                                .build())
                        .toList())
                .build();
    }
}
