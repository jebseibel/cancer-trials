import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Stethoscope, Dna, Pill, Upload, Download, UserPlus, Loader2 } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import Diagnosis from './Diagnosis';
import Variants from './Variants';
import PriorTreatment from './PriorTreatment';
import DiagnosisIntakeModal from '../components/DiagnosisIntakeModal';
import {
    patientApi,
    patientDiagnosisApi,
    patientVariantApi,
    patientPriorTreatmentApi,
    matchingApi,
} from '../services/api';
import { Field, Select, inputClass } from '../components/FormControls';
import { useCurrentPatient } from '../lib/PatientContext';
import { buildDiagnosisSummary } from '../lib/diagnosisSummary';
import { buildPatientRecordText } from '../lib/patientRecordExport';
import { SEX_VALUES } from '../types/api';
import diagnosisImage from '../assets/images/diagnosis-cancer-lightbrown.png';

// Three tables, three endpoints, three Save buttons - one per tab, unchanged from when these
// were separate pages. Tabs group them without merging the writes, so there is still no
// partial-write problem to invent a solution for.
type TabKey = 'diagnosis' | 'variants' | 'treatment';

const TABS: { key: TabKey; label: string; icon: LucideIcon }[] = [
    { key: 'diagnosis', label: 'Diagnosis', icon: Stethoscope },
    { key: 'variants', label: 'Variants', icon: Dna },
    { key: 'treatment', label: 'Prior Treatment', icon: Pill },
];

/**
 * Shown instead of the summary/buttons/tabs when the signed-in user has no patient record yet -
 * a brand new self-registered account, most commonly. `POST /api/patient` already existed and
 * already auto-grants OWNER in the same transaction (PatientService.createOwnedByCurrentUser),
 * but nothing in the frontend ever called it, so a new user's Patient Record page previously
 * rendered a header and tabs with the actual controls silently missing - this closes that gap.
 *
 * Only `displayName` is required, matching RequestPatientCreate's own @NotEmpty - the form
 * should not demand more than the API does. `notes` is deliberately left off this quick-create
 * form; it is free text and not needed to unblock the rest of the page.
 */
function CreatePatientPrompt() {
    const queryClient = useQueryClient();
    const { selectPatient } = useCurrentPatient();
    const [displayName, setDisplayName] = useState('');
    const [fullName, setFullName] = useState('');
    const [dateOfBirth, setDateOfBirth] = useState('');
    const [sex, setSex] = useState('');

    const createMutation = useMutation({
        mutationFn: async () =>
            (
                await patientApi.create({
                    displayName: displayName.trim(),
                    fullName: fullName.trim() || undefined,
                    dateOfBirth: dateOfBirth || undefined,
                    sex: sex || undefined,
                })
            ).data,
        onSuccess: async (created) => {
            await queryClient.invalidateQueries({ queryKey: ['patients', 'mine'] });
            // The stale-selection-recovery effect in PatientContext would eventually pick this
            // up on its own once the list refetches, but selecting it directly is deterministic
            // rather than relying on that fallback's timing.
            selectPatient(created.extid);
        },
    });

    const createError = createMutation.error as { response?: { data?: { message?: string } } } | null;

    return (
        <div className="mb-6 rounded-lg border border-stone-200 bg-brand-beige-card p-6">
            <div className="flex items-center gap-2 mb-2">
                <UserPlus className="h-5 w-5 text-brand-green" />
                <h2 className="font-heading text-lg font-bold text-stone-900">
                    Create your patient record
                </h2>
            </div>
            <p className="text-base text-stone-600 leading-normal mb-4">
                You don't have a patient record yet — create one to start recording diagnosis,
                variants and treatment history. A record can be for you or for someone you are
                helping.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
                <Field label="Name" required>
                    <input
                        type="text"
                        value={displayName}
                        onChange={(e) => setDisplayName(e.target.value)}
                        placeholder="e.g. Tina"
                        className={inputClass}
                    />
                </Field>
                <Field label="Full name">
                    <input
                        type="text"
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        className={inputClass}
                    />
                </Field>
                <Field label="Date of birth">
                    <input
                        type="date"
                        value={dateOfBirth}
                        onChange={(e) => setDateOfBirth(e.target.value)}
                        className={inputClass}
                    />
                </Field>
                <Field label="Sex">
                    <Select value={sex} onChange={setSex} options={SEX_VALUES} />
                </Field>
            </div>

            {createMutation.isError && (
                <div className="mb-4 rounded-md bg-red-50 border border-red-200 p-4 text-base leading-normal text-red-700">
                    {createError?.response?.data?.message ?? 'Could not create the record.'}
                </div>
            )}

            <button
                type="button"
                onClick={() => createMutation.mutate()}
                disabled={displayName.trim() === '' || createMutation.isPending}
                className="inline-flex items-center px-4 py-2 rounded-md border border-transparent bg-brand-green text-sm font-medium text-white hover:bg-brand-green-hover disabled:opacity-50 disabled:cursor-not-allowed"
            >
                {createMutation.isPending ? (
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                ) : (
                    <UserPlus className="h-4 w-4 mr-2" />
                )}
                Create my record
            </button>
        </div>
    );
}

export default function PatientRecord() {
    const [tab, setTab] = useState<TabKey>('diagnosis');
    const [showIntakeModal, setShowIntakeModal] = useState(false);
    const { patient } = useCurrentPatient();

    // Same status source TrialDetail uses to hide its AI button - "not configured" and
    // "failed" are different problems, and a reader should not have to tell them apart.
    const { data: aiStatus } = useQuery({
        queryKey: ['ai', 'status'],
        queryFn: async () => (await matchingApi.aiStatus()).data,
    });

    // Read here rather than inside a tab: the tabs mount only while selected, and the summary
    // draws on two of the three tables. Shared query keys with the tabs, so saving in a tab
    // refreshes the line above it rather than leaving it stale.
    const { data: diagnoses, isLoading: diagnosesLoading } = useQuery({
        queryKey: ['patientDiagnosis', patient?.extid],
        queryFn: async () => (await patientDiagnosisApi.getByPatientExtid(patient!.extid)).data,
        enabled: !!patient?.extid,
    });
    const { data: variants, isLoading: variantsLoading } = useQuery({
        queryKey: ['patientVariant', patient?.extid],
        queryFn: async () => (await patientVariantApi.getByPatientExtid(patient!.extid)).data,
        enabled: !!patient?.extid,
    });

    // Fetched here too, not just inside the Prior Treatment tab, so the download button below
    // works whichever tab is open - same reasoning as diagnoses/variants above.
    const { data: priorTreatments, isLoading: priorTreatmentsLoading } = useQuery({
        queryKey: ['patientPriorTreatment', patient?.extid],
        queryFn: async () =>
            (await patientPriorTreatmentApi.getByPatientExtid(patient!.extid)).data,
        enabled: !!patient?.extid,
    });

    // Guards the Download button, not just a loading spinner: buildPatientRecordText treats an
    // undefined row as "nothing recorded" and silently omits the whole section, identically to a
    // genuinely empty table. Downloading before all three queries resolve produced a record
    // missing entire sections (e.g. Diagnosis, with receptor status and biomarkers) with no error
    // - found live. This makes "still loading" and "nothing on file" impossible to confuse.
    const recordStillLoading = diagnosesLoading || variantsLoading || priorTreatmentsLoading;

    const summary = buildDiagnosisSummary(diagnoses?.[0], variants?.[0]);

    // Client-side only: builds the text from data already fetched above and hands the browser a
    // file to save. No request goes out - see patientRecordExport.ts for why this withholds
    // nothing, unlike the AI trial check's allowlist.
    const handleDownload = () => {
        const text = buildPatientRecordText(
            patient,
            diagnoses?.[0],
            variants?.[0],
            priorTreatments?.[0],
        );
        const blob = new Blob([text], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'patient-record.txt';
        a.click();
        URL.revokeObjectURL(url);
    };

    return (
        <div>
            <div className="flex items-center gap-2 mb-4">
                <Stethoscope className="h-6 w-6 text-brand-green" />
                <h1 className="font-heading text-2xl font-bold text-stone-900">Patient Diagnosis</h1>
            </div>

            {/* Free stock photography with no person in frame (a stethoscope), so it needs no
                identifiable-person check - see UI_DESIGN.md §2. In the shared shell rather than
                repeated in each of the three tabs, so it stays visible whichever one is open
                without three copies to keep in sync. */}
            <img
                src={diagnosisImage}
                alt=""
                className="mb-6 w-full max-w-4xl h-32 sm:h-40 rounded-lg object-cover shadow-md"
            />

            {/* Chrome above is patient-agnostic and stays visible either way. Everything below
                depends on there being a record to act on, so a brand new user with none sees a
                create-record prompt here instead of a summary/buttons/tabs row with the real
                controls silently missing. */}
            {!patient ? (
                <CreatePatientPrompt />
            ) : (
                <>
                    {/* The clinical picture in one line, above the tabs so it is there whichever
                        one is being edited. Nothing here is stored - it is the same record read
                        in the order a clinician thinks about it, so it cannot drift from the
                        forms below. */}
                    {summary && (
                        <div className="mb-6 rounded-lg border border-stone-200 bg-brand-beige-card px-4 py-3">
                            <p className="text-base leading-relaxed text-stone-900">{summary}</p>
                        </div>
                    )}

                    <div className="flex flex-wrap items-center gap-3 mb-4">
                        {aiStatus?.available && (
                            <button
                                type="button"
                                onClick={() => setShowIntakeModal(true)}
                                className="inline-flex items-center px-3 py-2 rounded-md border border-transparent bg-brand-green text-sm font-medium text-white hover:bg-brand-green-hover"
                            >
                                <Upload className="h-4 w-4 mr-2" />
                                Upload document to prefill
                            </button>
                        )}

                        {/* Needs no AI, so it stays visible even when the upload button above is
                            hidden. Secondary styling on purpose - upload stays the one
                            solid-green action on this page, see DOCUMENT_INTAKE_STATUS.md.
                            Disabled rather than hidden while any of the three tables are still
                            loading - a click before they resolve silently omitted whole sections
                            (undefined reads the same as "nothing on file" to the export), with
                            no error to say why. */}
                        <button
                            type="button"
                            onClick={handleDownload}
                            disabled={recordStillLoading}
                            title={recordStillLoading ? 'Still loading your record...' : undefined}
                            className="inline-flex items-center px-3 py-2 rounded-md border border-stone-300 bg-brand-beige-card text-sm font-medium text-stone-700 hover:bg-stone-100 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-brand-beige-card"
                        >
                            <Download className="h-4 w-4 mr-2" />
                            Download my record
                        </button>
                    </div>

                    {showIntakeModal && (
                        <DiagnosisIntakeModal
                            patientExtid={patient.extid}
                            onClose={() => setShowIntakeModal(false)}
                            onApply={() => {
                                setShowIntakeModal(false);
                                setTab('diagnosis');
                            }}
                        />
                    )}

                    <div className="border-b border-stone-200 mb-6">
                        {/* Icons are dropped below `sm`. Three tabs plus icons overflow a 360px
                            screen, and with no scroll affordance the third tab was simply
                            unreachable - the labels carry the meaning, so they are what
                            survives. */}
                        <nav className="-mb-px flex space-x-4 sm:space-x-6">
                            {TABS.map(({ key, label, icon: Icon }) => (
                                <button
                                    key={key}
                                    type="button"
                                    onClick={() => setTab(key)}
                                    className={`inline-flex min-h-11 items-center px-1 pb-3 text-sm font-normal border-b-2 ${
                                        tab === key
                                            ? 'border-brand-green text-brand-green-hover'
                                            : 'border-transparent text-stone-500 hover:text-stone-700 hover:border-stone-300'
                                    }`}
                                >
                                    <Icon className="hidden h-4 w-4 mr-2 sm:inline" />
                                    {label}
                                </button>
                            ))}
                        </nav>
                    </div>

                    {/* Mounted only while selected, so each tab refetches its own row on return
                        and a half-filled form is never left holding stale state behind a hidden
                        tab. */}
                    {tab === 'diagnosis' && <Diagnosis />}
                    {tab === 'variants' && <Variants />}
                    {tab === 'treatment' && <PriorTreatment />}
                </>
            )}
        </div>
    );
}
