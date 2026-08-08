import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, MapPin, Target, UserCircle, Layers, Check, X, HelpCircle, Stethoscope } from 'lucide-react';
import {
    trialApi,
    locationApi,
    armGroupApi,
    interventionApi,
    outcomeApi,
    overallOfficialApi,
    trialStatusApi,
    patientDiagnosisApi,
} from '../services/api';
import { useCurrentAppUser } from '../lib/useCurrentAppUser';
import { runTier1Checks } from '../lib/tier1Matching';
import type { CheckOutcome } from '../lib/tier1Matching';
import { TRIAL_STATUS_VALUES } from '../types/api';

export default function TrialDetail() {
    const { extid } = useParams<{ extid: string }>();
    const queryClient = useQueryClient();
    const { data: appUser } = useCurrentAppUser();
    const [savingStatus, setSavingStatus] = useState(false);
    const [notesDraft, setNotesDraft] = useState<string | null>(null);

    const { data: trial, isLoading, isError } = useQuery({
        queryKey: ['trial', extid],
        queryFn: async () => (await trialApi.getByExtid(extid!)).data,
        enabled: !!extid,
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
        queryKey: ['trialStatuses', appUser?.extid],
        queryFn: async () => (await trialStatusApi.getByAppUserExtid(appUser!.extid)).data,
        enabled: !!appUser?.extid,
    });

    const { data: diagnosis } = useQuery({
        queryKey: ['patientDiagnosis', appUser?.extid],
        queryFn: async () => {
            const rows = (await patientDiagnosisApi.getByAppUserExtid(appUser!.extid)).data;
            return rows[0] ?? null;
        },
        enabled: !!appUser?.extid,
    });

    const myStatus = myStatuses?.find((s) => s.trialExtid === extid);

    const handleStatusChange = async (newStatus: string) => {
        if (!appUser?.extid || !extid) return;
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
                    appUserExtid: appUser.extid,
                    status: newStatus,
                    statusChangedAt: new Date().toISOString(),
                });
            }
            await queryClient.invalidateQueries({ queryKey: ['trialStatuses', appUser.extid] });
        } finally {
            setSavingStatus(false);
        }
    };

    const handleNotesBlur = async () => {
        if (!appUser?.extid || !extid || notesDraft === null) return;
        setSavingStatus(true);
        try {
            if (myStatus) {
                await trialStatusApi.update(myStatus.extid, { notes: notesDraft });
            } else {
                await trialStatusApi.create({
                    trialExtid: extid,
                    appUserExtid: appUser.extid,
                    status: 'SAVED',
                    notes: notesDraft,
                });
            }
            await queryClient.invalidateQueries({ queryKey: ['trialStatuses', appUser.extid] });
        } finally {
            setSavingStatus(false);
            setNotesDraft(null);
        }
    };

    if (isLoading) return <p className="px-4 py-6 text-gray-500">Loading trial...</p>;
    if (isError || !trial) return <p className="px-4 py-6 text-red-600">Trial not found.</p>;

    return (
        <div className="px-4 py-6 sm:px-0">
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
                {!appUser ? (
                    <p className="text-sm text-gray-500">
                        No app-user profile linked to your login. Ask to have one seeded to enable tracking.
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
            {diagnosis && (
                <div className="bg-white shadow rounded-lg p-6 mb-6">
                    <h2 className="text-lg font-medium text-gray-900 mb-1 flex items-center gap-2">
                        <Stethoscope className="h-5 w-5 text-green-600" />
                        Basic Eligibility Checks
                    </h2>
                    <p className="text-sm text-gray-500 mb-4">
                        Compares your recorded diagnosis against this trial's stated age, sex, and
                        recruitment status. These are the only checks that can be made
                        automatically — they are not an eligibility decision.
                    </p>
                    <ul className="space-y-3">
                        {runTier1Checks(diagnosis, trial).map((check) => (
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
