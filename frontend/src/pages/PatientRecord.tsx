import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Stethoscope, Dna, Pill, Upload } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import Diagnosis from './Diagnosis';
import Variants from './Variants';
import PriorTreatment from './PriorTreatment';
import DiagnosisIntakeModal from '../components/DiagnosisIntakeModal';
import { patientDiagnosisApi, patientVariantApi, matchingApi } from '../services/api';
import { useCurrentPatient } from '../lib/PatientContext';
import { buildDiagnosisSummary } from '../lib/diagnosisSummary';
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
    const { data: diagnoses } = useQuery({
        queryKey: ['patientDiagnosis', patient?.extid],
        queryFn: async () => (await patientDiagnosisApi.getByPatientExtid(patient!.extid)).data,
        enabled: !!patient?.extid,
    });
    const { data: variants } = useQuery({
        queryKey: ['patientVariant', patient?.extid],
        queryFn: async () => (await patientVariantApi.getByPatientExtid(patient!.extid)).data,
        enabled: !!patient?.extid,
    });

    const summary = buildDiagnosisSummary(diagnoses?.[0], variants?.[0]);

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

            {/* The clinical picture in one line, above the tabs so it is there whichever one is
                being edited. Nothing here is stored - it is the same record read in the order a
                clinician thinks about it, so it cannot drift from the forms below. */}
            {summary && (
                <div className="mb-6 rounded-lg border border-stone-200 bg-brand-beige-card px-4 py-3">
                    <p className="text-base leading-relaxed text-stone-900">{summary}</p>
                </div>
            )}

            {aiStatus?.available && patient && (
                <button
                    type="button"
                    onClick={() => setShowIntakeModal(true)}
                    className="mb-4 inline-flex items-center px-3 py-2 rounded-md border border-transparent bg-brand-green text-sm font-medium text-white hover:bg-brand-green-hover"
                >
                    <Upload className="h-4 w-4 mr-2" />
                    Upload document to prefill
                </button>
            )}

            {showIntakeModal && patient && (
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
                {/* Icons are dropped below `sm`. Three tabs plus icons overflow a 360px screen,
                    and with no scroll affordance the third tab was simply unreachable - the
                    labels carry the meaning, so they are what survives. */}
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

            {/* Mounted only while selected, so each tab refetches its own row on return and a
                half-filled form is never left holding stale state behind a hidden tab. */}
            {tab === 'diagnosis' && <Diagnosis />}
            {tab === 'variants' && <Variants />}
            {tab === 'treatment' && <PriorTreatment />}
        </div>
    );
}
