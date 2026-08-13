import { useState } from 'react';
import { Stethoscope, Dna, Pill } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import Diagnosis from './Diagnosis';
import Variants from './Variants';
import PriorTreatment from './PriorTreatment';

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

    return (
        <div>
            <div className="flex items-center gap-2 mb-4">
                <Stethoscope className="h-6 w-6 text-green-600" />
                <h1 className="text-2xl font-bold text-gray-900">Patient Diagnosis</h1>
            </div>

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
