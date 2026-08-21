import { useState } from 'react';
import { AlertTriangle, HelpCircle, Check, Minus } from 'lucide-react';
import type { EligibilitySignal } from '../types/api';

// Shared by the ranked list and the trial detail page so the two cannot describe the same
// assessment differently. They do differ in what they show: the ranked list collapses passes
// behind "What matched", because a green checklist beside fifty trials reads as an eligibility
// verdict. On one trial there is room to show everything, and a reader looking at a single
// trial is asking a different question.

const STYLES: Record<string, string> = {
    CONCERN: 'border-amber-300 bg-amber-50 text-amber-900',
    UNKNOWN: 'border-sky-300 bg-sky-50 text-sky-900',
    PASS: 'border-green-200 bg-green-50 text-green-900',
    NOT_APPLICABLE: 'border-gray-200 bg-gray-50 text-gray-600',
};

function SignalIcon({ outcome }: { outcome: string }) {
    const className = 'mt-0.5 h-4 w-4 shrink-0';
    switch (outcome) {
        case 'CONCERN':
            return <AlertTriangle className={className} />;
        case 'PASS':
            return <Check className={className} />;
        case 'NOT_APPLICABLE':
            return <Minus className={className} />;
        default:
            return <HelpCircle className={className} />;
    }
}

export default function SignalRow({ signal }: { signal: EligibilitySignal }) {
    const [showEvidence, setShowEvidence] = useState(false);
    const style = STYLES[signal.outcome] ?? STYLES.UNKNOWN;

    return (
        <li className={`rounded border px-3 py-2 text-sm ${style}`}>
            <div className="flex items-start gap-2">
                <SignalIcon outcome={signal.outcome} />
                <div className="min-w-0 flex-1">
                    <span className="font-medium">{signal.name}:</span> {signal.detail}
                    {/* The quoted trial text sits behind a toggle rather than being hidden.
                        A reader has to be able to check the reasoning, but the wall of trial
                        text should not be the first thing they see. */}
                    {signal.evidence && (
                        <button
                            type="button"
                            onClick={() => setShowEvidence((v) => !v)}
                            className="ml-2 inline-flex min-h-6 items-center gap-1 px-1 py-0.5 align-baseline text-xs underline opacity-80 hover:opacity-100"
                        >
                            {showEvidence ? 'hide' : 'why?'}
                        </button>
                    )}
                    {showEvidence && signal.evidence && (
                        <blockquote className="mt-2 border-l-2 border-current/30 pl-2 text-xs italic opacity-90">
                            “{signal.evidence}”
                        </blockquote>
                    )}
                </div>
            </div>
        </li>
    );
}
