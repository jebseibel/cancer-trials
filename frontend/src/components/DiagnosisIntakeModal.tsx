import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { X, Loader2, Upload, Send } from 'lucide-react';
import { diagnosisIntakeApi } from '../services/api';
import { inputClass } from './FormControls';
import { setPendingIntakeDraft } from '../lib/diagnosisIntakeDraft';
import type { DiagnosisIntakeSession } from '../types/api';

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

/**
 * Upload/paste a document, get it screened locally for identifying information, then a
 * short back-and-forth for anything important the document didn't say - cancer type, stage,
 * ER/PR/HER2, ECOG. Nothing here is saved until the user reviews the result and hits Save on
 * the Diagnosis/Variants/Prior Treatment tabs themselves; this modal only hands off a draft.
 *
 * A document flagged as containing identifying information is rejected with a 422 whose
 * message renders here inline - manual edit-and-retry is the only recourse, by design: there
 * is no override, since the point is that flagged text never reaches the AI at all.
 */
export default function DiagnosisIntakeModal({ patientExtid, onClose, onApply }: Props) {
    const [step, setStep] = useState<Step>('upload');
    const [documentText, setDocumentText] = useState('');
    const [session, setSession] = useState<DiagnosisIntakeSession | null>(null);
    const [answerText, setAnswerText] = useState('');
    const [transcript, setTranscript] = useState<TranscriptEntry[]>([]);

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

    const handleApply = () => {
        if (!session) return;
        setPendingIntakeDraft({
            diagnosis: session.draftDiagnosis,
            variant: session.draftVariant,
            priorTreatment: session.draftPriorTreatment,
        });
        onApply();
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
                                plain text (.txt) file. It's checked for identifying details
                                before anything is read by AI — if any are found, the upload is
                                rejected and nothing is sent anywhere.
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
                                Here's what was found. Review it on the Diagnosis, Variants, and
                                Prior Treatment tabs and save each one yourself — nothing has
                                been saved yet.
                            </p>
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

                            <div className="flex justify-end gap-3">
                                <button
                                    type="button"
                                    onClick={handleCancel}
                                    className="px-4 py-2 text-sm font-medium text-stone-600 hover:text-stone-800"
                                >
                                    Discard
                                </button>
                                <button
                                    type="button"
                                    onClick={handleApply}
                                    className="inline-flex items-center px-4 py-2 rounded-md bg-brand-green text-white text-sm font-medium hover:bg-brand-green-hover"
                                >
                                    Apply to forms
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
