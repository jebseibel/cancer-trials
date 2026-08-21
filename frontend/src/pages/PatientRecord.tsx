import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Stethoscope, Dna, Pill } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import Diagnosis from './Diagnosis';
import Variants from './Variants';
import PriorTreatment from './PriorTreatment';
import { patientDiagnosisApi, patientVariantApi } from '../services/api';
import { useCurrentPatient } from '../lib/PatientContext';
import { buildDiagnosisSummary } from '../lib/diagnosisSummary';

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
    const { patient } = useCurrentPatient();

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
                <Stethoscope className="h-6 w-6 text-green-600" />
                <h1 className="text-2xl font-bold text-gray-900">Patient Diagnosis</h1>
            </div>

            {/* The clinical picture in one line, above the tabs so it is there whichever one is
                being edited. Nothing here is stored - it is the same record read in the order a
                clinician thinks about it, so it cannot drift from the forms below. */}
            {summary && (
                <div className="mb-6 rounded-lg border border-gray-200 bg-white px-4 py-3">
                    <p className="text-sm leading-relaxed text-gray-900">{summary}</p>
                </div>
            )}

            <div className="border-b border-gray-200 mb-6">
                {/* Icons are dropped below `sm`. Three tabs plus icons overflow a 360px screen,
                    and with no scroll affordance the third tab was simply unreachable - the
                    labels carry the meaning, so they are what survives. */}
                <nav className="-mb-px flex space-x-4 sm:space-x-6">
                    {TABS.map(({ key, label, icon: Icon }) => (
                        <button
                            key={key}
                            type="button"
                            onClick={() => setTab(key)}
                            className={`inline-flex min-h-11 items-center px-1 pb-3 text-sm font-medium border-b-2 ${
                                tab === key
                                    ? 'border-green-600 text-green-700'
                                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
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
