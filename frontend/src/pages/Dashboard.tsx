import { FlaskConical, Bookmark, Heart, Download, Loader2 } from 'lucide-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { trialApi, trialStatusApi, ingestionApi } from '../services/api';
import { useCurrentAppUser } from '../lib/useCurrentAppUser';

export default function Dashboard() {
    const { data: appUser } = useCurrentAppUser();
    const queryClient = useQueryClient();

    const { data: trialsPage } = useQuery({
        queryKey: ['trials'],
        queryFn: async () => (await trialApi.getAll({ size: 1 })).data,
    });

    const { data: myStatuses } = useQuery({
        queryKey: ['trialStatuses', appUser?.extid],
        queryFn: async () => (await trialStatusApi.getByAppUserExtid(appUser!.extid)).data,
        enabled: !!appUser?.extid,
    });

    const pullTrialsMutation = useMutation({
        mutationFn: async () => (await ingestionApi.runClinicalTrials({})).data,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['trials'] });
        },
    });

    const handlePullLatestTrials = () => {
        if (
            window.confirm(
                'Pull the latest trials from ClinicalTrials.gov? This fetches new/updated trials and normalizes them into the database.'
            )
        ) {
            pullTrialsMutation.mutate();
        }
    };

    const totalTrials = trialsPage?.totalElements ?? 0;
    const savedCount = myStatuses?.length ?? 0;
    const interestedCount = myStatuses?.filter((s) => s.status === 'INTERESTED').length ?? 0;

    return (
        <div className="px-4 py-6 sm:px-0">
            <h2 className="text-3xl font-bold text-gray-900 mb-2">Tina Cancer Project</h2>
            <h1 className="text-xl text-gray-600 mb-8">Search trials and track your status on each one</h1>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 mb-8">
                <Link to="/trials" className="bg-white overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
                    <div className="p-5">
                        <div className="flex items-center">
                            <div className="flex-shrink-0">
                                <FlaskConical className="h-6 w-6 text-green-600" />
                            </div>
                            <div className="ml-5 w-0 flex-1">
                                <dl>
                                    <dt className="text-sm font-medium text-gray-500 truncate">Trials in database</dt>
                                    <dd className="text-lg font-medium text-gray-900">{totalTrials}</dd>
                                </dl>
                            </div>
                        </div>
                    </div>
                </Link>

                <Link to="/saved-trials" className="bg-white overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
                    <div className="p-5">
                        <div className="flex items-center">
                            <div className="flex-shrink-0">
                                <Bookmark className="h-6 w-6 text-blue-600" />
                            </div>
                            <div className="ml-5 w-0 flex-1">
                                <dl>
                                    <dt className="text-sm font-medium text-gray-500 truncate">Saved/tracked trials</dt>
                                    <dd className="text-lg font-medium text-gray-900">{savedCount}</dd>
                                </dl>
                            </div>
                        </div>
                    </div>
                </Link>

                <Link to="/saved-trials" className="bg-white overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
                    <div className="p-5">
                        <div className="flex items-center">
                            <div className="flex-shrink-0">
                                <Heart className="h-6 w-6 text-red-500" />
                            </div>
                            <div className="ml-5 w-0 flex-1">
                                <dl>
                                    <dt className="text-sm font-medium text-gray-500 truncate">Interested</dt>
                                    <dd className="text-lg font-medium text-gray-900">{interestedCount}</dd>
                                </dl>
                            </div>
                        </div>
                    </div>
                </Link>
            </div>

            {/* Quick Actions */}
            <div className="bg-white shadow rounded-lg p-6 mb-8">
                <h2 className="text-lg font-medium text-gray-900 mb-4">Quick Actions</h2>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <Link
                        to="/trials"
                        className="relative rounded-lg border border-gray-300 bg-white px-6 py-5 shadow-sm flex items-center space-x-3 hover:border-gray-400 focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-green-500"
                    >
                        <div className="flex-shrink-0">
                            <FlaskConical className="h-10 w-10 text-green-600" />
                        </div>
                        <div className="flex-1 min-w-0">
                            <span className="absolute inset-0" aria-hidden="true" />
                            <p className="text-sm font-medium text-gray-900">Search Trials</p>
                            <p className="text-sm text-gray-500 truncate">Search saved trials by title or status</p>
                        </div>
                    </Link>
                    <Link
                        to="/saved-trials"
                        className="relative rounded-lg border border-gray-300 bg-white px-6 py-5 shadow-sm flex items-center space-x-3 hover:border-gray-400 focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-blue-500"
                    >
                        <div className="flex-shrink-0">
                            <Bookmark className="h-10 w-10 text-blue-600" />
                        </div>
                        <div className="flex-1 min-w-0">
                            <span className="absolute inset-0" aria-hidden="true" />
                            <p className="text-sm font-medium text-gray-900">View Saved Trials</p>
                            <p className="text-sm text-gray-500 truncate">See trials you're tracking</p>
                        </div>
                    </Link>
                    <button
                        type="button"
                        onClick={handlePullLatestTrials}
                        disabled={pullTrialsMutation.isPending}
                        className="relative rounded-lg border border-gray-300 bg-white px-6 py-5 shadow-sm flex items-center space-x-3 hover:border-gray-400 focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed text-left"
                    >
                        <div className="flex-shrink-0">
                            {pullTrialsMutation.isPending ? (
                                <Loader2 className="h-10 w-10 text-green-600 animate-spin" />
                            ) : (
                                <Download className="h-10 w-10 text-green-600" />
                            )}
                        </div>
                        <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium text-gray-900">
                                {pullTrialsMutation.isPending ? 'Pulling...' : 'Pull Latest Trials'}
                            </p>
                            <p className="text-sm text-gray-500 truncate">
                                Fetch and normalize new trials from ClinicalTrials.gov
                            </p>
                        </div>
                    </button>
                </div>

                {pullTrialsMutation.isSuccess && (
                    <div className="mt-4 rounded-md bg-green-50 border border-green-200 p-4 text-sm text-green-800">
                        Pulled {pullTrialsMutation.data.studiesFetched} studies,{' '}
                        {pullTrialsMutation.data.trialsNormalized} trials normalized.
                        {(pullTrialsMutation.data.ingestErrors.length > 0 ||
                            pullTrialsMutation.data.normalizationErrors.length > 0) && (
                            <span className="text-red-700">
                                {' '}
                                ({pullTrialsMutation.data.ingestErrors.length +
                                    pullTrialsMutation.data.normalizationErrors.length}{' '}
                                error(s) — see Ingest page for details.)
                            </span>
                        )}
                    </div>
                )}
                {pullTrialsMutation.isError && (
                    <div className="mt-4 rounded-md bg-red-50 border border-red-200 p-4 text-sm text-red-700">
                        Failed to pull trials. Check that the backend is running and try again.
                    </div>
                )}
            </div>
        </div>
    );
}
