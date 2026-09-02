// A small ephemeral handoff between DiagnosisIntakeModal (mounted once, above the tabs, from
// PatientRecord) and the three tab pages (Diagnosis, Variants, PriorTreatment), which mount and
// unmount independently as the user switches tabs and each own their form state locally.
//
// Deliberately not React Context: this is a one-shot value consumed once per tab after an
// intake completes, not an ongoing subscription, and the three tabs' form-state ownership is
// otherwise untouched by this feature. Each tab reads its own slice on mount; a slice is left
// available until read, so visiting the tabs in any order still picks up the draft.
import type {
    DiagnosisIntakeDraftDiagnosis,
    DiagnosisIntakeDraftPriorTreatment,
    DiagnosisIntakeDraftVariant,
} from '../types/api';

export interface PendingIntakeDraft {
    diagnosis: DiagnosisIntakeDraftDiagnosis;
    variant: DiagnosisIntakeDraftVariant;
    priorTreatment: DiagnosisIntakeDraftPriorTreatment;
}

let pendingDraft: PendingIntakeDraft | null = null;

export function setPendingIntakeDraft(draft: PendingIntakeDraft): void {
    pendingDraft = draft;
}

export function takePendingDiagnosisDraft(): DiagnosisIntakeDraftDiagnosis | null {
    return pendingDraft?.diagnosis ?? null;
}

export function takePendingVariantDraft(): DiagnosisIntakeDraftVariant | null {
    return pendingDraft?.variant ?? null;
}

export function takePendingPriorTreatmentDraft(): DiagnosisIntakeDraftPriorTreatment | null {
    return pendingDraft?.priorTreatment ?? null;
}

/** Called when switching to a different patient, so a draft from one record can never bleed
 * into another's forms. */
export function clearPendingIntakeDraft(): void {
    pendingDraft = null;
}
