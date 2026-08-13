import { FlaskConical, Bookmark, Heart, Download } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { trialApi, trialStatusApi } from '../services/api';
import { useCurrentPatient } from '../lib/PatientContext';

export default function Dashboard() {
    const { patient } = useCurrentPatient();

    const { data: trialsPage } = useQuery({
        queryKey: ['trials'],
        queryFn: async () => (await trialApi.getAll({ size: 1 })).data,
    });

    const { data: myStatuses } = useQuery({
        queryKey: ['trialStatuses', patient?.extid],
        queryFn: async () => (await trialStatusApi.getByPatientExtid(patient!.extid)).data,
        enabled: !!patient?.extid,
    });

    const totalTrials = trialsPage?.totalElements ?? 0;
    const savedCount = myStatuses?.length ?? 0;
    const interestedCount = myStatuses?.filter((s) => s.status === 'INTERESTED').length ?? 0;

    return (
        <div>
            <h2 className="text-3xl font-bold text-gray-900 mb-2">Breast Cancer Trial Finder</h2>
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
                    {/* A link, not a run-it-here button: every other card on this page navigates,
                        and pulling without preparing for search leaves trials that silently do
                        not turn up in results. The Process Trials page does the whole job. */}
                    <Link
                        to="/ingestion"
                        className="relative rounded-lg border border-gray-300 bg-white px-6 py-5 shadow-sm flex items-center space-x-3 hover:border-gray-400 focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-green-500"
                    >
                        <div className="flex-shrink-0">
                            <Download className="h-10 w-10 text-green-600" />
                        </div>
                        <div className="flex-1 min-w-0">
                            <span className="absolute inset-0" aria-hidden="true" />
                            <p className="text-sm font-medium text-gray-900">Process Trials</p>
                            <p className="text-sm text-gray-500 truncate">
                                Pull new trials and prepare them for search
                            </p>
                        </div>
                    </Link>
                </div>
            </div>
        </div>
    );
}
