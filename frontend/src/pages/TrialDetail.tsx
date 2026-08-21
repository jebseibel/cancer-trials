import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { ArrowLeft, MapPin, Target, UserCircle, Layers, Check, X, HelpCircle, Stethoscope, ListChecks, Sparkles, AlertTriangle, Loader2 } from 'lucide-react';
import {
    trialApi,
    locationApi,
    armGroupApi,
    interventionApi,
    outcomeApi,
    overallOfficialApi,
    trialStatusApi,
    matchingApi,
} from '../services/api';
import { useCurrentPatient } from '../lib/PatientContext';
import { runTier1Checks } from '../lib/tier1Matching';
import SignalRow from '../components/SignalRow';
import type { CheckOutcome } from '../lib/tier1Matching';
import type { AiTrialCheck } from '../types/api';
import { TRIAL_STATUS_VALUES } from '../types/api';

export default function TrialDetail() {
    const { extid } = useParams<{ extid: string }>();
    const queryClient = useQueryClient();
    const { patient } = useCurrentPatient();
    const [savingStatus, setSavingStatus] = useState(false);
    const [notesDraft, setNotesDraft] = useState<string | null>(null);

    const { data: trial, isLoading, isError } = useQuery({
        queryKey: ['trial', extid],
        queryFn: async () => (await trialApi.getByExtid(extid!)).data,
        enabled: !!extid,
    });

    // The Tier 2 assessment for this one trial. Its own endpoint rather than a slice of the
    // ranked list, so opening a trial directly - from a link, or a saved trial - still explains
    // itself. Silent on failure: the assessment is additional context, and a trial's own record
    // must still render if matching is unavailable.
    const { data: assessment } = useQuery({
        queryKey: ['assessment', extid, patient?.extid],
        queryFn: async () => (await matchingApi.assessTrial(extid!, patient!.extid)).data,
        enabled: !!extid && !!patient?.extid,
        retry: false,
    });

    // Hidden rather than disabled when unconfigured: a button that always fails is worse than
    // no button, and "not set up" is not something a reader can act on.
    const { data: aiStatus } = useQuery({
        queryKey: ['ai-status'],
        queryFn: async () => (await matchingApi.aiStatus()).data,
        retry: false,
        staleTime: Infinity,
    });

    const aiCheck = useMutation({
        mutationFn: async () => (await matchingApi.aiCheck(extid!, patient!.extid)).data,
    });

    const { data: locations } = useQuery({
        queryKey: ['locations', extid],
        queryFn: async () => (await locationApi.getByTrialExtid(extid!)).data,
        enabled: !!extid,
    });

    const { data: armGroups } = useQuery({
        queryKey: ['armGroups', extid],
        queryFn: async () => (await armGroupApi.getByTrialExtid(extid!)).data,
        enabled: !!extid,
    });

    const { data: interventions } = useQuery({
        queryKey: ['interventions', extid],
        queryFn: async () => (await interventionApi.getByTrialExtid(extid!)).data,
        enabled: !!extid,
    });

    const { data: outcomes } = useQuery({
        queryKey: ['outcomes', extid],
        queryFn: async () => (await outcomeApi.getByTrialExtid(extid!)).data,
        enabled: !!extid,
    });

    const { data: officials } = useQuery({
        queryKey: ['overallOfficials', extid],
        queryFn: async () => (await overallOfficialApi.getByTrialExtid(extid!)).data,
        enabled: !!extid,
    });

    const { data: myStatuses } = useQuery({
        queryKey: ['trialStatuses', patient?.extid],
        queryFn: async () => (await trialStatusApi.getByPatientExtid(patient!.extid)).data,
        enabled: !!patient?.extid,
    });


    const myStatus = myStatuses?.find((s) => s.trialExtid === extid);

    const handleStatusChange = async (newStatus: string) => {
        if (!patient?.extid || !extid) return;
        setSavingStatus(true);
        try {
            if (myStatus) {
                await trialStatusApi.update(myStatus.extid, {
                    status: newStatus,
                    statusChangedAt: new Date().toISOString(),
                });
            } else {
                await trialStatusApi.create({
                    trialExtid: extid,
                    patientExtid: patient.extid,
                    status: newStatus,
                    statusChangedAt: new Date().toISOString(),
                });
            }
            await queryClient.invalidateQueries({ queryKey: ['trialStatuses', patient.extid] });
        } finally {
            setSavingStatus(false);
        }
    };

    const handleNotesBlur = async () => {
        if (!patient?.extid || !extid || notesDraft === null) return;
        setSavingStatus(true);
        try {
            if (myStatus) {
                await trialStatusApi.update(myStatus.extid, { notes: notesDraft });
            } else {
                await trialStatusApi.create({
                    trialExtid: extid,
                    patientExtid: patient.extid,
                    status: 'SAVED',
                    notes: notesDraft,
                });
            }
            await queryClient.invalidateQueries({ queryKey: ['trialStatuses', patient.extid] });
        } finally {
            setSavingStatus(false);
            setNotesDraft(null);
        }
    };

    if (isLoading) return <p className="px-4 py-6 text-gray-500">Loading trial...</p>;
    if (isError || !trial) return <p className="px-4 py-6 text-red-600">Trial not found.</p>;

    return (
        <div>
            <Link to="/trials" className="inline-flex items-center text-sm text-gray-600 hover:text-gray-900 mb-4">
                <ArrowLeft className="h-4 w-4 mr-1" />
                Back to search
            </Link>

            <div className="bg-white shadow rounded-lg p-6 mb-6">
                <div className="flex items-start justify-between gap-4 mb-2">
                    <span className="text-xs font-mono text-gray-500">{trial.nctId ?? 'No NCT ID'}</span>
                    {trial.overallStatus && (
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                            {trial.overallStatus.replaceAll('_', ' ')}
                        </span>
                    )}
                </div>
                <h1 className="text-2xl font-bold text-gray-900 mb-1">{trial.briefTitle}</h1>
                {trial.officialTitle && trial.officialTitle !== trial.briefTitle && (
                    <p className="text-sm text-gray-500 mb-4">{trial.officialTitle}</p>
                )}
                {trial.briefSummary && <p className="text-gray-700 mt-4">{trial.briefSummary}</p>}

                <dl className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-6 text-sm">
                    <div>
                        <dt className="text-gray-500">Study type</dt>
                        <dd className="text-gray-900 font-medium">{trial.studyType ?? '—'}</dd>
                    </div>
                    <div>
                        <dt className="text-gray-500">Sex</dt>
                        <dd className="text-gray-900 font-medium">{trial.sex ?? '—'}</dd>
                    </div>
                    <div>
                        <dt className="text-gray-500">Age range</dt>
                        <dd className="text-gray-900 font-medium">
                            {trial.minimumAge ?? '—'} – {trial.maximumAge ?? '—'}
                        </dd>
                    </div>
                    <div>
                        <dt className="text-gray-500">Healthy volunteers</dt>
                        <dd className="text-gray-900 font-medium">
                            {trial.healthyVolunteers === true ? 'Yes' : trial.healthyVolunteers === false ? 'No' : '—'}
                        </dd>
                    </div>
                </dl>
            </div>

            {/* Personal tracking */}
            <div className="bg-white shadow rounded-lg p-6 mb-6">
                <h2 className="text-lg font-medium text-gray-900 mb-4">Your Tracking</h2>
                {!patient ? (
                    <p className="text-sm text-gray-500">
                        No patient record yet. Create one to track this trial.
                    </p>
                ) : (
                    <div className="space-y-4">
                        <div className="flex items-center gap-3">
                            <label className="text-sm font-medium text-gray-700">Status</label>
                            <select
                                value={myStatus?.status ?? ''}
                                onChange={(e) => handleStatusChange(e.target.value)}
                                disabled={savingStatus}
                                className="px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-green-500 focus:border-green-500 disabled:opacity-50"
                            >
                                <option value="" disabled>
                                    Select status...
                                </option>
                                {TRIAL_STATUS_VALUES.map((s) => (
                                    <option key={s} value={s}>
                                        {s.replaceAll('_', ' ')}
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Notes</label>
                            <textarea
                                defaultValue={myStatus?.notes ?? ''}
                                onChange={(e) => setNotesDraft(e.target.value)}
                                onBlur={handleNotesBlur}
                                rows={3}
                                placeholder="Personal notes about this trial..."
                                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-green-500 focus:border-green-500"
                            />
                        </div>
                    </div>
                )}
            </div>

            {/* Tier 1 matching - deterministic checks only, per DIAGNOSIS_MATCHING_DESIGN.md */}
            {patient && (
                <div className="bg-white shadow rounded-lg p-6 mb-6">
                    <h2 className="text-lg font-medium text-gray-900 mb-1 flex items-center gap-2">
                        <Stethoscope className="h-5 w-5 text-green-600" />
                        Basic Eligibility Checks
                    </h2>
                    <p className="text-sm text-gray-500 mb-4">
                        Compares your recorded details against this trial's stated age, sex, and
                        recruitment status. These are the only checks that can be made
                        automatically — they are not an eligibility decision.
                    </p>
                    <ul className="space-y-3">
                        {runTier1Checks(patient, trial).map((check) => (
                            <li key={check.label} className="flex items-start gap-3">
                                <OutcomeIcon outcome={check.outcome} />
                                <div className="text-sm">
                                    <span className="font-medium text-gray-900">{check.label}</span>
                                    <p className="text-gray-600">{check.detail}</p>
                                </div>
                            </li>
                        ))}
                    </ul>
                    <p className="mt-4 text-xs text-gray-500 border-t pt-3">
                        Everything else in the eligibility criteria below is unassessed. A trial
                        that fails a check here may still be worth asking about — confirm with the
                        study team.
                    </p>
                </div>
            )}

            {/* Tier 2 - the assessment that drives the ranked list. Shown here so a trial
                opened directly explains itself the same way it does in that list. */}
            {assessment && assessment.signals.length > 0 && (
                <div className="bg-white shadow rounded-lg p-6 mb-6">
                    <h2 className="text-lg font-medium text-gray-900 mb-1 flex items-center gap-2">
                        <ListChecks className="h-5 w-5 text-blue-600" />
                        What We Checked Against Your Record
                    </h2>
                    <p className="text-sm text-gray-500 mb-4">
                        Compares this trial's own text against your diagnosis, variants and prior
                        treatment. Anything flagged is something to ask about — none of it decides
                        whether you qualify.
                    </p>

                    {/* Counts, never a percentage. A number that looks like a probability
                        invites reliance this tool must not earn. */}
                    <p className="text-sm text-gray-600 mb-4">
                        {[
                            assessment.concernCount > 0 && `${assessment.concernCount} to check`,
                            assessment.unknownCount > 0 && `${assessment.unknownCount} to ask about`,
                            assessment.passCount > 0 && `${assessment.passCount} matched`,
                        ]
                            .filter(Boolean)
                            .join(' · ') || 'Nothing to flag on this trial.'}
                    </p>

                    {/* Every signal, unlike the ranked list which collapses passes. There is one
                        trial here and the reader is already looking at it closely. */}
                    <ul className="space-y-2">
                        {assessment.signals.map((s) => (
                            <SignalRow key={s.name} signal={s} />
                        ))}
                    </ul>

                    <p className="mt-4 text-xs text-gray-500 border-t pt-3">
                        These checks read the trial's own wording, which is often ambiguous. Use
                        “why?” to see the exact text behind any flag, and confirm anything that
                        matters with the study team.
                    </p>
                </div>
            )}

            {/* Reads criteria the patterns cannot - a carve-out inside an exclusion, an
                unusual phrasing, a criterion nobody wrote a rule for. Runs on a press, never
                on load: it costs money and takes seconds. */}
            {patient && aiStatus?.available && (
                <div className="bg-white shadow rounded-lg p-6 mb-6">
                    <h2 className="text-lg font-medium text-gray-900 mb-1 flex items-center gap-2">
                        <Sparkles className="h-5 w-5 text-purple-600" />
                        Read This Trial Against Your Record
                    </h2>
                    <p className="text-sm text-gray-500 mb-4">
                        Reads this trial's eligibility criteria line by line against your record and
                        reports what it finds. It can tell you if something rules you out, and what
                        it could not judge &mdash; it cannot tell you that you qualify.
                    </p>

                    <button
                        type="button"
                        onClick={() => aiCheck.mutate()}
                        disabled={aiCheck.isPending}
                        className="inline-flex items-center gap-2 rounded-md bg-purple-600 px-4 py-2 text-white hover:bg-purple-700 disabled:opacity-50 min-h-[2.5rem]"
                    >
                        {aiCheck.isPending ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                            <Sparkles className="h-4 w-4" />
                        )}
                        {aiCheck.isPending ? 'Reading the criteria...' : 'Check this trial'}
                    </button>

                    {aiCheck.isError && (
                        <p className="mt-3 text-sm text-red-600">
                            {(aiCheck.error as { response?: { data?: { message?: string } } })
                                ?.response?.data?.message
                                ?? 'The check could not be run. Nothing about this trial has changed.'}
                        </p>
                    )}

                    {aiCheck.data && <AiCheckResult check={aiCheck.data} />}
                </div>
            )}

            {/* Eligibility */}
            {trial.eligibilityCriteria && (
                <div className="bg-white shadow rounded-lg p-6 mb-6">
                    <h2 className="text-lg font-medium text-gray-900 mb-3">Eligibility Criteria</h2>
                    <pre className="whitespace-pre-wrap text-sm text-gray-700 font-sans">{trial.eligibilityCriteria}</pre>
                </div>
            )}

            {/* Interventions */}
            {!!interventions?.length && (
                <Section title="Interventions" icon={<Target className="h-5 w-5 text-green-600" />}>
                    <ul className="space-y-2">
                        {interventions.map((i) => (
                            <li key={i.extid} className="text-sm">
                                <span className="font-medium text-gray-900">{i.name}</span>
                                {i.type && <span className="text-gray-500"> ({i.type})</span>}
                                {i.description && <p className="text-gray-600 mt-0.5">{i.description}</p>}
                            </li>
                        ))}
                    </ul>
                </Section>
            )}

            {/* Arm Groups */}
            {!!armGroups?.length && (
                <Section title="Arm Groups" icon={<Layers className="h-5 w-5 text-green-600" />}>
                    <ul className="space-y-2">
                        {armGroups.map((a) => (
                            <li key={a.extid} className="text-sm">
                                <span className="font-medium text-gray-900">{a.label}</span>
                                {a.type && <span className="text-gray-500"> ({a.type})</span>}
                                {a.description && <p className="text-gray-600 mt-0.5">{a.description}</p>}
                            </li>
                        ))}
                    </ul>
                </Section>
            )}

            {/* Outcomes */}
            {!!outcomes?.length && (
                <Section title="Outcomes" icon={<Target className="h-5 w-5 text-green-600" />}>
                    <ul className="space-y-3">
                        {outcomes.map((o) => (
                            <li key={o.extid} className="text-sm">
                                <span className="inline-block px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-700 mr-2">
                                    {o.outcomeType}
                                </span>
                                <span className="font-medium text-gray-900">{o.measure}</span>
                                {o.timeFrame && <p className="text-gray-500 mt-0.5">Time frame: {o.timeFrame}</p>}
                            </li>
                        ))}
                    </ul>
                </Section>
            )}

            {/* Locations */}
            {!!locations?.length && (
                <Section title="Locations" icon={<MapPin className="h-5 w-5 text-green-600" />}>
                    <ul className="space-y-2">
                        {locations.map((l) => (
                            <li key={l.extid} className="text-sm text-gray-700">
                                {[l.facility, l.city, l.state, l.country].filter(Boolean).join(', ') || '—'}
                                {l.status && <span className="text-gray-500"> — {l.status}</span>}
                            </li>
                        ))}
                    </ul>
                </Section>
            )}

            {/* Contacts */}
            {!!officials?.length && (
                <Section title="Contacts" icon={<UserCircle className="h-5 w-5 text-green-600" />}>
                    <ul className="space-y-2">
                        {officials.map((o) => (
                            <li key={o.extid} className="text-sm text-gray-700">
                                <span className="font-medium text-gray-900">{o.name}</span>
                                {o.role && <span className="text-gray-500"> — {o.role}</span>}
                                {o.affiliation && <p className="text-gray-500">{o.affiliation}</p>}
                            </li>
                        ))}
                    </ul>
                </Section>
            )}
        </div>
    );
}

/** Amber, not red, for a failed check - it is a flag to ask about, never an auto-exclusion. */
/**
 * What the model reported.
 *
 * <p>An exclusion is stated plainly with its quoted criterion, because that is a checkable
 * claim. Its absence is rendered as "nothing here rules you out" and never as a match - the
 * whole point of the response shape is that eligibility is not the model's to declare.
 */
function AiCheckResult({ check }: { check: AiTrialCheck }) {
    const ruledOut = check.rulesPatientOut === true;

    return (
        <div className="mt-4 space-y-4">
            <div
                className={`rounded border px-4 py-3 text-sm ${
                    ruledOut
                        ? 'border-amber-300 bg-amber-50 text-amber-900'
                        : 'border-gray-200 bg-gray-50 text-gray-700'
                }`}
            >
                <div className="flex items-start gap-2">
                    {ruledOut ? (
                        <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
                    ) : (
                        <Check className="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
                    )}
                    <div>
                        <p className="font-medium">
                            {ruledOut
                                ? 'Something in this trial would rule you out'
                                : 'Nothing in these criteria rules you out'}
                        </p>
                        {check.summary && <p className="mt-1">{check.summary}</p>}
                    </div>
                </div>
                {ruledOut && check.exclusionCriterion && (
                    <blockquote className="mt-3 border-l-2 border-amber-400 pl-3 text-xs italic">
                        &ldquo;{check.exclusionCriterion}&rdquo;
                    </blockquote>
                )}
            </div>

            {/* Listed before the matches on purpose: these are the reason to run this at all.
                They are what turns an appointment into specific questions. */}
            <AiList
                title="Worth asking your care team"
                items={check.openQuestions}
                className="border-sky-200 bg-sky-50 text-sky-900"
            />
            <AiList
                title="Things to be aware of"
                items={check.concerns}
                className="border-amber-200 bg-amber-50 text-amber-900"
            />
            <AiList
                title="Criteria your record appears to meet"
                items={check.criteriaSheAppearsToMeet}
                className="border-gray-200 bg-gray-50 text-gray-600"
            />

            <p className="border-t pt-3 text-xs text-gray-500">
                Read by {check.model ?? 'an AI model'}, which can misread a criterion. Nothing here
                decides whether you qualify &mdash; only the study team can do that. Take the
                questions above to your care team rather than acting on this.
            </p>
        </div>
    );
}

function AiList({ title, items, className }: { title: string; items?: string[] | null; className: string }) {
    if (!items || items.length === 0) {
        return null;
    }
    return (
        <div className={`rounded border px-4 py-3 text-sm ${className}`}>
            <p className="mb-2 font-medium">{title}</p>
            <ul className="list-disc space-y-1 pl-5">
                {items.map((item, i) => (
                    <li key={i}>{item}</li>
                ))}
            </ul>
        </div>
    );
}

function OutcomeIcon({ outcome }: { outcome: CheckOutcome }) {
    if (outcome === 'pass') {
        return <Check className="h-5 w-5 text-green-600 shrink-0 mt-0.5" aria-label="matches" />;
    }
    if (outcome === 'fail') {
        return <X className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" aria-label="does not match" />;
    }
    return <HelpCircle className="h-5 w-5 text-gray-400 shrink-0 mt-0.5" aria-label="not assessed" />;
}

function Section({ title, icon, children }: { title: string; icon: React.ReactNode; children: React.ReactNode }) {
    return (
        <div className="bg-white shadow rounded-lg p-6 mb-6">
            <h2 className="text-lg font-medium text-gray-900 mb-3 flex items-center gap-2">
                {icon}
                {title}
            </h2>
            {children}
        </div>
    );
}
