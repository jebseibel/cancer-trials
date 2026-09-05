import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { X, Loader2, Upload, Send, CheckCircle2 } from 'lucide-react';
import {
    diagnosisIntakeApi,
    patientDiagnosisApi,
    patientVariantApi,
    patientPriorTreatmentApi,
} from '../services/api';
import { inputClass } from './FormControls';
import type {
    DiagnosisIntakeSession,
    PatientDiagnosisRequest,
    PatientVariantRequest,
    PatientPriorTreatmentRequest,
} from '../types/api';

type Step = 'upload' | 'conversation' | 'complete';

interface TranscriptEntry {
    question: string;
    answer?: string;
}

interface Props {
    patientExtid: string;
    onClose: () => void;
    onApply: () => void;
}

type ApiError = { response?: { data?: { message?: string } } };

/** `Object.assign`-style overlay that skips `undefined` so a draft field the extraction never
 * touched can never blank out something already on file. */
function mergeDefined<T extends object>(base: T, draft: Partial<T>): T {
    const merged = { ...base };
    for (const key of Object.keys(draft) as (keyof T)[]) {
        if (draft[key] !== undefined) merged[key] = draft[key] as T[keyof T];
    }
    return merged;
}

/**
 * Upload/paste a document, get it screened locally for identifying information, then a
 * short back-and-forth for anything important the document didn't say - cancer type, stage,
 * ER/PR/HER2, ECOG. "Save to my record" writes the draft straight to the Diagnosis, Variants
 * and Prior Treatment tables itself - it does not merely prefill the tabs and leave the actual
 * save to the user, which read, in practice, as "this didn't work" the first time someone
 * pressed it and nothing appeared in the database.
 *
 * Identifying information is screened line by line, not document by document: a flagged line is
 * cut before anything reaches AI, but the rest of the document still goes through. The complete
 * step lists which lines were cut and why, so a false positive (a line that was actually fine)
 * can be caught and typed in by hand on the relevant tab, rather than silently missing.
 */
export default function DiagnosisIntakeModal({ patientExtid, onClose, onApply }: Props) {
    const queryClient = useQueryClient();
    const [step, setStep] = useState<Step>('upload');
    const [documentText, setDocumentText] = useState('');
    const [session, setSession] = useState<DiagnosisIntakeSession | null>(null);
    const [answerText, setAnswerText] = useState('');
    const [transcript, setTranscript] = useState<TranscriptEntry[]>([]);
    const [applyError, setApplyError] = useState<string | null>(null);

    const startMutation = useMutation({
        mutationFn: () => diagnosisIntakeApi.start({ patientExtid, documentText }),
        onSuccess: ({ data }) => {
            setSession(data);
            if (data.nextQuestion) {
                setTranscript([{ question: data.nextQuestion }]);
                setStep('conversation');
            } else {
                setStep('complete');
            }
        },
    });

    const answerMutation = useMutation({
        mutationFn: () => diagnosisIntakeApi.answer(session!.sessionId, { answerText }),
        onSuccess: ({ data }) => {
            setSession(data);
            setTranscript((prev) => {
                const updated = [...prev];
                if (updated.length > 0) {
                    updated[updated.length - 1] = { ...updated[updated.length - 1], answer: answerText };
                }
                if (data.nextQuestion) updated.push({ question: data.nextQuestion });
                return updated;
            });
            setAnswerText('');
            if (data.status === 'COMPLETE') setStep('complete');
        },
    });

    const skipMutation = useMutation({
        mutationFn: () => diagnosisIntakeApi.skip(session!.sessionId),
        onSuccess: ({ data }) => {
            setSession(data);
            setStep('complete');
        },
    });

    const handleFile = async (file: File) => {
        setDocumentText(await file.text());
    };

    const handleCancel = () => {
        if (session) diagnosisIntakeApi.cancel(session.sessionId);
        onClose();
    };

    // Applying saves directly rather than only prefilling the three tabs' forms - a hand-off
    // with nothing but a form full of unsaved values read, to more than one live user, as "this
    // didn't work". Each table gets its own existing-record check: an update merges the draft's
    // extracted fields onto whatever is already on file (a field the draft never touched keeps
    // its existing value, never gets blanked), and a create is used untouched when there is
    // nothing on file yet. Three independent calls, not one transaction - a failure on one table
    // must not silently discard what the other two already saved.
    const applyMutation = useMutation({
        mutationFn: async () => {
            if (!session) throw new Error('No completed draft to apply.');
            const { draftDiagnosis, draftVariant, draftPriorTreatment } = session;
            const skipped: string[] = [];
            const saved: string[] = [];

            const [existingDiagnoses, existingVariants, existingTreatments] = await Promise.all([
                patientDiagnosisApi.getByPatientExtid(patientExtid).then((r) => r.data),
                patientVariantApi.getByPatientExtid(patientExtid).then((r) => r.data),
                patientPriorTreatmentApi.getByPatientExtid(patientExtid).then((r) => r.data),
            ]);

            const existingDiagnosis = existingDiagnoses[0];
            if (existingDiagnosis) {
                const merged = mergeDefined(existingDiagnosis, draftDiagnosis);
                await patientDiagnosisApi.update(existingDiagnosis.extid, merged);
                saved.push('Diagnosis');
            } else if (draftDiagnosis.cancerType) {
                const request: PatientDiagnosisRequest = { patientExtid, ...draftDiagnosis, cancerType: draftDiagnosis.cancerType };
                await patientDiagnosisApi.create(request);
                saved.push('Diagnosis');
            } else if (Object.keys(draftDiagnosis).length > 0) {
                // Cancer type is the one required field on this table (@NotEmpty server-side).
                // The extraction found other diagnosis fields but not that one, and there is no
                // existing row to merge onto - creating would fail, so this table is skipped
                // rather than silently dropping the other fields it did find.
                skipped.push('Diagnosis (no cancer type found — add it on the Diagnosis tab, then save)');
            }

            const existingVariant = existingVariants[0];
            if (Object.keys(draftVariant).length > 0 || existingVariant) {
                if (existingVariant) {
                    const merged = mergeDefined(existingVariant, draftVariant);
                    await patientVariantApi.update(existingVariant.extid, merged);
                } else {
                    const request: PatientVariantRequest = { patientExtid, ...draftVariant };
                    await patientVariantApi.create(request);
                }
                if (Object.keys(draftVariant).length > 0) saved.push('Variants');
            }

            const existingTreatment = existingTreatments[0];
            if (Object.keys(draftPriorTreatment).length > 0 || existingTreatment) {
                if (existingTreatment) {
                    const merged = mergeDefined(existingTreatment, draftPriorTreatment);
                    await patientPriorTreatmentApi.update(existingTreatment.extid, merged);
                } else {
                    const request: PatientPriorTreatmentRequest = { patientExtid, ...draftPriorTreatment };
                    await patientPriorTreatmentApi.create(request);
                }
                if (Object.keys(draftPriorTreatment).length > 0) saved.push('Prior Treatment');
            }

            return { saved, skipped };
        },
        onSuccess: async ({ saved, skipped }) => {
            await Promise.all([
                queryClient.invalidateQueries({ queryKey: ['patientDiagnosis', patientExtid] }),
                queryClient.invalidateQueries({ queryKey: ['patientVariant', patientExtid] }),
                queryClient.invalidateQueries({ queryKey: ['patientPriorTreatment', patientExtid] }),
            ]);
            if (skipped.length > 0) {
                setApplyError(
                    `Saved: ${saved.length > 0 ? saved.join(', ') : 'nothing else'}. Not saved — ${skipped.join('; ')}.`,
                );
                return;
            }
            onApply();
        },
    });

    const handleApply = () => {
        setApplyError(null);
        applyMutation.mutate();
    };

    const startError = startMutation.error as ApiError | null;
    const answerError = answerMutation.error as ApiError | null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
            <div className="w-full max-w-lg rounded-lg bg-white shadow-xl">
                <div className="flex items-center justify-between border-b border-stone-200 px-5 py-4">
                    <h2 className="text-lg font-medium text-stone-900">Upload a document to prefill</h2>
                    <button
                        type="button"
                        onClick={handleCancel}
                        className="text-stone-400 hover:text-stone-600"
                        aria-label="Close"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                <div className="max-h-[70vh] overflow-y-auto px-5 py-4">
                    {step === 'upload' && (
                        <div>
                            <p className="mb-3 text-base leading-normal text-stone-600">
                                Paste the text of a pathology report or clinic note, or upload a
                                plain text (.txt) file. Each line is checked for identifying
                                details before anything is read by AI — a line that looks like it
                                names a person, a date of birth, or contact details is left out,
                                and the rest of the document is still read.
                            </p>

                            <label className="mb-3 inline-flex cursor-pointer items-center gap-2 text-sm font-medium text-brand-green-hover">
                                <Upload className="h-4 w-4" />
                                Choose a .txt file
                                <input
                                    type="file"
                                    accept=".txt,text/plain"
                                    className="hidden"
                                    onChange={(e) => {
                                        const file = e.target.files?.[0];
                                        if (file) void handleFile(file);
                                    }}
                                />
                            </label>

                            <textarea
                                rows={10}
                                value={documentText}
                                onChange={(e) => setDocumentText(e.target.value)}
                                placeholder="Paste document text here..."
                                className={inputClass}
                            />

                            {startMutation.isError && (
                                <div className="mt-3 rounded-md bg-red-50 border border-red-200 px-4 py-3 text-base leading-normal text-red-700">
                                    {startError?.response?.data?.message ??
                                        'Could not process this document.'}
                                </div>
                            )}

                            <div className="mt-4 flex justify-end gap-3">
                                <button
                                    type="button"
                                    onClick={handleCancel}
                                    className="px-4 py-2 text-sm font-medium text-stone-600 hover:text-stone-800"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="button"
                                    disabled={!documentText.trim() || startMutation.isPending}
                                    onClick={() => startMutation.mutate()}
                                    className="inline-flex items-center px-4 py-2 rounded-md bg-brand-green text-white text-sm font-medium hover:bg-brand-green-hover disabled:opacity-50"
                                >
                                    {startMutation.isPending && (
                                        <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                    )}
                                    {startMutation.isPending ? 'Reading...' : 'Read document'}
                                </button>
                            </div>
                        </div>
                    )}

                    {step === 'conversation' && (
                        <div>
                            <p className="mb-3 text-base leading-normal text-stone-600">
                                A few things weren't clear from the document. Answer what you
                                can — you can skip the rest and fill them in yourself later.
                            </p>

                            <div className="mb-4 space-y-3">
                                {transcript.map((entry, i) => (
                                    <div key={i} className="rounded-md bg-stone-50 border border-stone-200 px-3 py-2">
                                        <p className="text-base leading-normal text-stone-900">{entry.question}</p>
                                        {entry.answer !== undefined && (
                                            <p className="mt-1 text-base leading-normal text-brand-green-hover">
                                                You: {entry.answer}
                                            </p>
                                        )}
                                    </div>
                                ))}
                            </div>

                            {answerMutation.isError && (
                                <div className="mb-3 rounded-md bg-red-50 border border-red-200 px-4 py-3 text-base leading-normal text-red-700">
                                    {answerError?.response?.data?.message ?? 'Could not send that answer.'}
                                </div>
                            )}

                            <div className="flex gap-2">
                                <input
                                    type="text"
                                    value={answerText}
                                    onChange={(e) => setAnswerText(e.target.value)}
                                    onKeyDown={(e) => {
                                        if (e.key === 'Enter' && answerText.trim() && !answerMutation.isPending) {
                                            answerMutation.mutate();
                                        }
                                    }}
                                    placeholder="Type your answer..."
                                    className={inputClass}
                                />
                                <button
                                    type="button"
                                    disabled={!answerText.trim() || answerMutation.isPending}
                                    onClick={() => answerMutation.mutate()}
                                    className="inline-flex items-center px-3 py-2 rounded-md bg-brand-green text-white text-sm font-medium hover:bg-brand-green-hover disabled:opacity-50"
                                    aria-label="Send answer"
                                >
                                    {answerMutation.isPending ? (
                                        <Loader2 className="h-4 w-4 animate-spin" />
                                    ) : (
                                        <Send className="h-4 w-4" />
                                    )}
                                </button>
                            </div>

                            <div className="mt-4 flex justify-between">
                                <button
                                    type="button"
                                    disabled={skipMutation.isPending}
                                    onClick={() => skipMutation.mutate()}
                                    className="px-4 py-2 text-sm font-medium text-stone-600 hover:text-stone-800"
                                >
                                    Skip the rest
                                </button>
                                <button
                                    type="button"
                                    onClick={handleCancel}
                                    className="px-4 py-2 text-sm font-medium text-stone-600 hover:text-stone-800"
                                >
                                    Cancel
                                </button>
                            </div>
                        </div>
                    )}

                    {step === 'complete' && (
                        <div>
                            <p className="mb-4 text-base leading-normal text-stone-600">
                                Here's what was found. Applying saves it to your Diagnosis,
                                Variants, and Prior Treatment record directly — a field the
                                document didn't mention is left exactly as it is; nothing already
                                on file is cleared out.
                            </p>

                            {session && session.excludedLines.length > 0 && (
                                <div className="mb-4 rounded-md bg-amber-50 border border-amber-200 px-4 py-3 text-base leading-normal text-amber-900">
                                    <p className="font-medium">
                                        {session.excludedLines.length === 1
                                            ? 'One line was left out before this document was read'
                                            : `${session.excludedLines.length} lines were left out before this document was read`}
                                        , because it looked like it might contain identifying
                                        information (line number and reason only — the text
                                        itself is never kept):
                                    </p>
                                    <ul className="mt-2 list-disc pl-5">
                                        {session.excludedLines.map((line) => (
                                            <li key={line.lineNumber}>
                                                Line {line.lineNumber} ({line.reasons.join(', ')})
                                            </li>
                                        ))}
                                    </ul>
                                    <p className="mt-2">
                                        If any of these were actually fine, add that detail
                                        yourself on the relevant tab below.
                                    </p>
                                </div>
                            )}

                            <dl className="mb-4 space-y-1 text-base leading-normal text-stone-900">
                                {session?.draftDiagnosis.cancerType && (
                                    <div><dt className="inline font-medium">Cancer type: </dt><dd className="inline">{session.draftDiagnosis.cancerType}</dd></div>
                                )}
                                {session?.draftDiagnosis.stage && (
                                    <div><dt className="inline font-medium">Stage: </dt><dd className="inline">{session.draftDiagnosis.stage}</dd></div>
                                )}
                                {session?.draftDiagnosis.erStatus && (
                                    <div><dt className="inline font-medium">ER status: </dt><dd className="inline">{session.draftDiagnosis.erStatus}</dd></div>
                                )}
                                {session?.draftDiagnosis.prStatus && (
                                    <div><dt className="inline font-medium">PR status: </dt><dd className="inline">{session.draftDiagnosis.prStatus}</dd></div>
                                )}
                                {session?.draftDiagnosis.her2Status && (
                                    <div><dt className="inline font-medium">HER2 status: </dt><dd className="inline">{session.draftDiagnosis.her2Status}</dd></div>
                                )}
                            </dl>

                            {applyError && (
                                <div className="mb-4 rounded-md bg-amber-50 border border-amber-200 px-4 py-3 text-base leading-normal text-amber-900">
                                    {applyError}
                                </div>
                            )}

                            {applyMutation.isError && (
                                <div className="mb-4 rounded-md bg-red-50 border border-red-200 px-4 py-3 text-base leading-normal text-red-700">
                                    Could not save this to your record. Nothing was changed —
                                    try again, or add the details yourself on the relevant tab.
                                </div>
                            )}

                            <div className="flex justify-end gap-3">
                                <button
                                    type="button"
                                    onClick={handleCancel}
                                    disabled={applyMutation.isPending}
                                    className="px-4 py-2 text-sm font-medium text-stone-600 hover:text-stone-800 disabled:opacity-50"
                                >
                                    Discard
                                </button>
                                <button
                                    type="button"
                                    disabled={applyMutation.isPending}
                                    onClick={handleApply}
                                    className="inline-flex items-center px-4 py-2 rounded-md bg-brand-green text-white text-sm font-medium hover:bg-brand-green-hover disabled:opacity-50"
                                >
                                    {applyMutation.isPending ? (
                                        <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                    ) : (
                                        <CheckCircle2 className="h-4 w-4 mr-2" />
                                    )}
                                    {applyMutation.isPending ? 'Saving...' : 'Save to my record'}
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
