import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Bookmark, FlaskConical } from 'lucide-react';
import { trialApi, trialStatusApi } from '../services/api';
import { useCurrentPatient } from '../lib/PatientContext';
import { useAudience } from '../lib/AudienceContext';
import { displayTitle } from '../lib/displayTitle';
import { TRIAL_STATUS_VALUES } from '../types/api';
import breastCancerRibbonImage from '../assets/images/breastcancer-ribbon.png';

export default function SavedTrials() {
    const { patient } = useCurrentPatient();
    const { mode } = useAudience();
    const [statusFilter, setStatusFilter] = useState('');

    const { data: myStatuses, isLoading: statusesLoading } = useQuery({
        queryKey: ['trialStatuses', patient?.extid],
        queryFn: async () => (await trialStatusApi.getByPatientExtid(patient!.extid)).data,
        enabled: !!patient?.extid,
    });

    const { data: trialsPage, isLoading: trialsLoading } = useQuery({
        queryKey: ['trials'],
        queryFn: async () => (await trialApi.getAll({ size: 200 })).data,
    });

    const rows = useMemo(() => {
        const trials = trialsPage?.content ?? [];
        const statuses = myStatuses ?? [];
        return statuses
            .filter((s) => !statusFilter || s.status === statusFilter)
            .map((s) => ({ status: s, trial: trials.find((t) => t.extid === s.trialExtid) }))
            .filter((row) => !!row.trial);
    }, [myStatuses, trialsPage, statusFilter]);

    const isLoading = statusesLoading || trialsLoading;

    return (
        <div>
            <h1 className="font-heading text-3xl font-bold text-stone-900 mb-2">Saved Trials</h1>
            <p className="text-base text-stone-600 leading-normal mb-4">Trials you're tracking, by personal status.</p>

            {/* Free stock photography with no person in frame (a ribbon), so it needs no
                identifiable-person check - see UI_DESIGN.md §2. Full-width banner below the
                heading, same placement as the Diagnosis page banner. */}
            <img
                src={breastCancerRibbonImage}
                alt=""
                className="mb-6 w-full max-w-4xl h-32 sm:h-40 rounded-lg object-cover shadow-md"
            />

            {!patient ? (
                <p className="text-base text-stone-500 leading-normal">
                    You don't have a patient record yet. Create one and you'll be able to save
                    trials here as you come across them.
                </p>
            ) : (
                <>
                    <div className="bg-brand-beige-card shadow rounded-lg p-4 mb-6 flex gap-4">
                        <select
                            value={statusFilter}
                            onChange={(e) => setStatusFilter(e.target.value)}
                            className="px-3 py-2 border border-stone-300 rounded-md shadow-sm focus:outline-none focus:ring-brand-green focus:border-brand-green"
                        >
                            <option value="">All statuses</option>
                            {TRIAL_STATUS_VALUES.map((s) => (
                                <option key={s} value={s}>
                                    {s.replaceAll('_', ' ')}
                                </option>
                            ))}
                        </select>
                    </div>

                    {isLoading && <p className="text-base text-stone-500">Loading...</p>}
                    {!isLoading && rows.length === 0 && (
                        <p className="text-base text-stone-500 leading-normal">
                            Nothing saved with this status yet — when you find a trial worth a
                            second look, save it and it'll show up here.
                        </p>
                    )}

                    <div className="space-y-3">
                        {rows.map(({ status, trial }) => (
                            <Link
                                key={status.extid}
                                to={`/trials/${trial!.extid}`}
                                className="block bg-brand-beige-card shadow rounded-lg p-5 hover:shadow-md transition-shadow"
                            >
                                <div className="flex items-start justify-between gap-4">
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 mb-1">
                                            <FlaskConical className="h-4 w-4 text-brand-green flex-shrink-0" />
                                            <span className="text-xs font-mono text-stone-500">
                                                {trial!.nctId ?? 'No NCT ID'}
                                            </span>
                                        </div>
                                        <h2 className="text-lg font-medium text-stone-900 truncate">
                                            {displayTitle(trial!, mode)}
                                        </h2>
                                        {mode === 'patient' && trial!.friendlyTitle && (
                                            <p className="text-xs text-stone-500 truncate">{trial!.briefTitle}</p>
                                        )}
                                        {status.notes && (
                                            <p className="text-sm text-stone-600 mt-1 line-clamp-2">{status.notes}</p>
                                        )}
                                    </div>
                                    <span className="flex-shrink-0 inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                                        <Bookmark className="h-3 w-3" />
                                        {status.status.replaceAll('_', ' ')}
                                    </span>
                                </div>
                            </Link>
                        ))}
                    </div>
                </>
            )}
        </div>
    );
}
