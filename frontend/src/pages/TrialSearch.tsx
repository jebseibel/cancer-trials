import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Search, FlaskConical } from 'lucide-react';
import { trialApi, patientDiagnosisApi } from '../services/api';
import { useCurrentAppUser } from '../lib/useCurrentAppUser';
import { runTier1Checks, summariseTier1 } from '../lib/tier1Matching';
import type { PatientDiagnosis, Trial } from '../types/api';

const STATUS_OPTIONS = [
    'RECRUITING',
    'ACTIVE_NOT_RECRUITING',
    'COMPLETED',
    'TERMINATED',
    'WITHDRAWN',
    'NOT_YET_RECRUITING',
];

export default function TrialSearch() {
    const [term, setTerm] = useState('');
    const [status, setStatus] = useState('');

    const { data: appUser } = useCurrentAppUser();

    const { data: diagnosis } = useQuery({
        queryKey: ['patientDiagnosis', appUser?.extid],
        queryFn: async () => {
            const rows = (await patientDiagnosisApi.getByAppUserExtid(appUser!.extid)).data;
            return rows[0] ?? null;
        },
        enabled: !!appUser?.extid,
    });

    const { data, isLoading, isError } = useQuery({
        queryKey: ['trials'],
        queryFn: async () => {
            const response = await trialApi.getAll({ size: 200 });
            return response.data;
        },
    });

    const trials = useMemo(() => {
        const all = data?.content ?? [];
        const lowerTerm = term.trim().toLowerCase();
        return all.filter((trial) => {
            const matchesTerm =
                !lowerTerm ||
                trial.briefTitle?.toLowerCase().includes(lowerTerm) ||
                trial.nctId?.toLowerCase().includes(lowerTerm) ||
                trial.briefSummary?.toLowerCase().includes(lowerTerm);
            const matchesStatus = !status || trial.overallStatus === status;
            return matchesTerm && matchesStatus;
        });
    }, [data, term, status]);

    return (
        <div className="px-4 py-6 sm:px-0">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">Trial Search</h1>
            <p className="text-gray-600 mb-6">Search saved trials by title, NCT number, or status.</p>

            <div className="bg-white shadow rounded-lg p-4 mb-6 flex flex-col sm:flex-row gap-4">
                <div className="relative flex-1">
                    <Search className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
                    <input
                        type="text"
                        value={term}
                        onChange={(e) => setTerm(e.target.value)}
                        placeholder="Search title, summary, or NCT number..."
                        className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-green-500 focus:border-green-500"
                    />
                </div>
                <select
                    value={status}
                    onChange={(e) => setStatus(e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-green-500 focus:border-green-500"
                >
                    <option value="">All statuses</option>
                    {STATUS_OPTIONS.map((s) => (
                        <option key={s} value={s}>
                            {s.replaceAll('_', ' ')}
                        </option>
                    ))}
                </select>
            </div>

            {isLoading && <p className="text-gray-500">Loading trials...</p>}
            {isError && <p className="text-red-600">Failed to load trials.</p>}

            {!isLoading && !isError && trials.length === 0 && (
                <p className="text-gray-500">No trials match your search.</p>
            )}

            <div className="space-y-3">
                {trials.map((trial) => (
                    <Link
                        key={trial.extid}
                        to={`/trials/${trial.extid}`}
                        className="block bg-white shadow rounded-lg p-5 hover:shadow-md transition-shadow"
                    >
                        <div className="flex items-start justify-between gap-4">
                            <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-2 mb-1">
                                    <FlaskConical className="h-4 w-4 text-green-600 flex-shrink-0" />
                                    <span className="text-xs font-mono text-gray-500">{trial.nctId ?? 'No NCT ID'}</span>
                                </div>
                                <h2 className="text-lg font-medium text-gray-900 truncate">{trial.briefTitle}</h2>
                                {trial.briefSummary && (
                                    <p className="text-sm text-gray-600 mt-1 line-clamp-2">{trial.briefSummary}</p>
                                )}
                                {diagnosis && <Tier1Badge diagnosis={diagnosis} trial={trial} />}
                            </div>
                            {trial.overallStatus && (
                                <span className="flex-shrink-0 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                                    {trial.overallStatus.replaceAll('_', ' ')}
                                </span>
                            )}
                        </div>
                    </Link>
                ))}
            </div>
        </div>
    );
}

/**
 * One-line Tier 1 summary. Amber rather than red on a mismatch, and never hidden from the
 * list - DIAGNOSIS_MATCHING_DESIGN.md section 5 forbids auto-excluding a trial because a
 * check did not match.
 */
function Tier1Badge({ diagnosis, trial }: { diagnosis: PatientDiagnosis; trial: Trial }) {
    const summary = summariseTier1(runTier1Checks(diagnosis, trial));
    const style =
        summary.outcome === 'pass'
            ? 'bg-green-50 text-green-800'
            : summary.outcome === 'fail'
              ? 'bg-amber-50 text-amber-800'
              : 'bg-gray-100 text-gray-600';

    return (
        <span className={`mt-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${style}`}>
            {summary.text}
        </span>
    );
}
