import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { ListChecks, Check, ChevronDown, ChevronRight, MapPin } from 'lucide-react';
import { matchingApi } from '../services/api';
import { useCurrentPatient } from '../lib/PatientContext';
import type { TrialAssessment } from '../types/api';
import SignalRow from '../components/SignalRow';

// Run on demand, never on page load. Ranking assesses thousands of trials in one request and
// takes tens of seconds, so it has to be something the reader chooses to start and can see
// the progress of - not a spinner that appears unbidden and looks like a hang.

// Where the trial runs, shown on the card itself. For most people travel is the constraint that
// decides whether a trial is possible at all, so the cities have to be visible at a glance
// rather than hidden inside the location signal.
function TrialSites({ assessment }: { assessment: TrialAssessment }) {
    const [showAllSites, setShowAllSites] = useState(false);
    const { siteCities, siteCount, hasUnitedStatesSite } = assessment;

    if (!siteCities || siteCities.length === 0) {
        return (
            <div className="mt-1.5 flex items-start gap-1.5 text-sm text-gray-500">
                <MapPin className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                <span>No locations listed for this trial yet.</span>
            </div>
        );
    }

    const visible = showAllSites ? siteCities : siteCities.slice(0, 4);
    const hidden = siteCount - visible.length;

    return (
        <div
            className={`mt-1.5 flex items-start gap-1.5 text-sm ${
                hasUnitedStatesSite ? 'text-gray-700' : 'text-amber-800'
            }`}
        >
            <MapPin className="mt-0.5 h-3.5 w-3.5 shrink-0" />
            <div>
                {!hasUnitedStatesSite && (
                    <span className="font-medium">Outside the United States — </span>
                )}
                {visible.join(' · ')}
                {hidden > 0 && !showAllSites && (
                    <button
                        type="button"
                        onClick={() => setShowAllSites(true)}
                        className="ml-1 inline-flex min-h-6 items-center px-1 py-0.5 underline hover:no-underline"
                    >
                        and {hidden} more
                    </button>
                )}
            </div>
        </div>
    );
}

function AssessmentCard({ assessment }: { assessment: TrialAssessment }) {
    const [showAll, setShowAll] = useState(false);

    const notable = assessment.signals.filter(
        (s) => s.outcome === 'CONCERN' || s.outcome === 'UNKNOWN'
    );
    const passed = assessment.signals.filter((s) => s.outcome === 'PASS');

    return (
        <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-2">
                <div className="min-w-0">
                    <Link
                        to={`/trials/${assessment.trialExtid}`}
                        className="font-medium text-blue-700 hover:underline"
                    >
                        {assessment.briefTitle || assessment.nctId}
                    </Link>
                    <div className="mt-0.5 text-xs text-gray-500">
                        {assessment.nctId}
                        {assessment.overallStatus && ` · ${assessment.overallStatus.replace(/_/g, ' ')}`}
                        {!assessment.breastCancer && ' · not specific to breast cancer'}
                    </div>
                    {/* Directly under the title, never behind a toggle. Getting there is often
                        what decides whether a trial is possible, so it belongs beside the name
                        rather than inside a signal a reader has to expand. */}
                    <TrialSites assessment={assessment} />
                </div>
                {/* Counts, never a percentage. There is no fit score by design.
                    They wrap to their own line on a narrow screen rather than holding width
                    against the title - the trial name is what a reader scans for. */}
                <div className="flex w-full shrink-0 flex-wrap gap-x-3 gap-y-1 text-xs text-gray-600 sm:w-auto">
                    {assessment.concernCount > 0 && (
                        <span className="text-amber-700">
                            {assessment.concernCount} to check
                        </span>
                    )}
                    {assessment.unknownCount > 0 && (
                        <span className="text-sky-700">{assessment.unknownCount} to ask about</span>
                    )}
                    {passed.length > 0 && (
                        <span className="text-gray-500">{passed.length} matched</span>
                    )}
                </div>
            </div>

            {notable.length > 0 && (
                <ul className="mt-3 space-y-2">
                    {notable.map((s) => (
                        <SignalRow key={s.name} signal={s} />
                    ))}
                </ul>
            )}

            {notable.length === 0 && (
                <p className="mt-3 text-sm text-gray-600">
                    Nothing came up that needs checking on the details recorded so far.
                </p>
            )}

            {/* Matches are de-emphasised on purpose. A green checklist reads like an
                eligibility verdict, and this tool does not make that call. */}
            {passed.length > 0 && (
                <div className="mt-3">
                    <button
                        type="button"
                        onClick={() => setShowAll((v) => !v)}
                        className="inline-flex min-h-8 items-center gap-1 py-1 text-xs text-gray-500 hover:text-gray-700"
                    >
                        {showAll ? <ChevronDown className="h-3 w-3" /> : <ChevronRight className="h-3 w-3" />}
                        What matched
                    </button>
                    {showAll && (
                        <ul className="mt-2 space-y-1">
                            {passed.map((s) => (
                                <li key={s.name} className="flex items-start gap-2 text-sm text-gray-600">
                                    <Check className="mt-0.5 h-4 w-4 shrink-0 text-green-600" />
                                    <span>{s.detail}</span>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            )}
        </div>
    );
}

export default function RankedTrials() {
    const { patient } = useCurrentPatient();

    const rank = useMutation({
        mutationFn: async () => {
            // Always breast-only. This was a checkbox, and unchecking it filled the list with
            // trials for other cancers - which the disease-type signal already demotes to the
            // bottom anyway, so the control cost a reader attention and bought nothing.
            //
            // It also filters before assessment rather than after, so it is what keeps this
            // call at a few seconds instead of paying per-trial work on the ~54% of the corpus
            // that is other diseases.
            const response = await matchingApi.rank(patient!.extid, { breastOnly: true, limit: 50 });
            return response.data;
        },
    });

    const results = rank.data ?? [];

    return (
        <div className="mx-auto max-w-4xl">
            <div className="mb-6">
                <h1 className="flex items-center gap-2 text-2xl font-semibold text-gray-900">
                    <ListChecks className="h-6 w-6 text-blue-600" />
                    Trials for You
                </h1>
                <p className="mt-1 text-sm text-gray-600">
                    This looks through the breast cancer trials we have using the details on
                    your Patient Record, and lists the ones worth a closer look first.
                </p>
            </div>

            {/* Said plainly and up front, because it is the one thing a reader could most
                easily get wrong about what this page is. */}
            <div className="mb-6 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                <p className="font-medium">This is a starting point for conversations, not medical advice.</p>
                <p className="mt-1">
                    Nothing here decides whether you qualify for a trial — only your care team
                    can do that. Anything flagged is something to ask about, and a trial is never
                    hidden because of a flag.
                </p>
            </div>

            <div className="mb-6 flex flex-wrap items-center gap-3">
                <button
                    type="button"
                    onClick={() => rank.mutate()}
                    disabled={!patient || rank.isPending}
                    className="rounded-md bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
                >
                    {rank.isPending ? 'Looking through the trials…' : 'Find trials for me'}
                </button>
            </div>

            {/* This takes tens of seconds. Saying so is the difference between "working" and
                "broken" to someone waiting. */}
            {rank.isPending && (
                <div className="rounded-lg border border-gray-200 bg-white p-6 text-center text-sm text-gray-600">
                    Checking each trial against your record. This can take up to a minute.
                </div>
            )}

            {rank.isError && (
                <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
                    Something went wrong while looking through the trials. Please try again.
                </div>
            )}

            {rank.isSuccess && results.length === 0 && (
                <div className="rounded-lg border border-gray-200 bg-white p-6 text-sm text-gray-600">
                    No trials came back. If your Patient Record is empty, filling it in will give
                    this much more to work with.
                </div>
            )}

            {results.length > 0 && (
                <>
                    <p className="mb-3 text-sm text-gray-600">
                        Showing {results.length} trials, most promising first.
                    </p>
                    <div className="space-y-4">
                        {results.map((a) => (
                            <AssessmentCard key={a.trialExtid} assessment={a} />
                        ))}
                    </div>
                </>
            )}
        </div>
    );
}
