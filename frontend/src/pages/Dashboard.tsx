import { FlaskConical, Bookmark, Heart, Download, ArrowRight } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { trialApi, trialStatusApi } from '../services/api';
import { useCurrentPatient } from '../lib/PatientContext';
import findACureImage from '../assets/images/find-a-cure.png';

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
            {/* The hero moment UI_DESIGN.md §2/§4 asks for - a visual anchor the way
                breastcancer.org's photo gives its homepage, without a photo of a person. This
                image is a still life (a card pinned to a wall), so it carries none of the
                PHI/consent question a photo of a patient would. Contained in a rounded card
                rather than full-bleed, so its own pink stays a deliberate accent against the
                app's beige/green palette instead of overriding it. */}
            <div className="flex flex-col-reverse items-center gap-6 mb-8 sm:flex-row sm:items-start">
                <div className="flex-1">
                    <h2 className="font-heading text-3xl font-medium text-stone-900 mb-2">
                        <span className="italic">Breast Cancer</span> Trial Finder
                    </h2>
                    {/* The "we're here to help" line UI_DESIGN.md §4 asks for, ahead of anything
                        numeric - a landing moment rather than a stats panel opening the page. */}
                    <p className="text-lg text-stone-700 mb-1">
                        You don't have to search alone. We're here to help you find trials worth
                        asking your care team about.
                    </p>
                    <h1 className="text-base text-stone-500">Search trials and track your status on each one</h1>
                </div>
                <img
                    src={findACureImage}
                    alt=""
                    className="w-40 h-40 sm:w-48 sm:h-48 shrink-0 rounded-lg object-cover shadow-md"
                />
            </div>

            {/* Stats Grid - flat, borderless tiles (beige fill, sentence-case label over the
                number) rather than white cards with a shadow. Matches the flat article-tile
                look on breastcancer.org's homepage: the shadow only appears on hover, so the
                page reads calm until a reader actually reaches for one. */}
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 mb-8">
                <Link
                    to="/trials"
                    className="group bg-brand-beige-card rounded-lg p-5 hover:shadow-md transition-all"
                >
                    <div className="flex items-center justify-between">
                        <p className="text-sm text-stone-500">
                            Trials we're watching for you
                        </p>
                        <FlaskConical className="h-5 w-5 text-brand-green shrink-0" />
                    </div>
                    <p className="font-heading mt-2 text-3xl font-medium text-stone-900">{totalTrials}</p>
                </Link>

                <Link
                    to="/saved-trials"
                    className="group bg-brand-beige-card rounded-lg p-5 hover:shadow-md transition-all"
                >
                    <div className="flex items-center justify-between">
                        <p className="text-sm text-stone-500">
                            Trials you've saved
                        </p>
                        <Bookmark className="h-5 w-5 text-blue-600 shrink-0" />
                    </div>
                    <p className="font-heading mt-2 text-3xl font-medium text-stone-900">{savedCount}</p>
                </Link>

                <Link
                    to="/saved-trials"
                    className="group bg-brand-beige-card rounded-lg p-5 hover:shadow-md transition-all"
                >
                    <div className="flex items-center justify-between">
                        <p className="text-sm text-stone-500">
                            Trials that caught your eye
                        </p>
                        <Heart className="h-5 w-5 text-red-500 shrink-0" />
                    </div>
                    <p className="font-heading mt-2 text-3xl font-medium text-stone-900">{interestedCount}</p>
                </Link>
            </div>

            {/* Quick Actions - same flat article-tile language as the Stats Grid above: warm
                beige fill, no border, and an arrow that only appears on hover as the click
                affordance, rather than a border that is always on screen. The small label above
                each title is sentence case, not uppercase - UI_DESIGN.md §5 flags heavy all-caps
                as unwelcoming for a reader who may have vision changes from treatment. */}
            <div className="mb-8">
                <h2 className="font-heading text-2xl font-medium text-stone-900 mb-4">A few places to start</h2>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    <Link
                        to="/trials"
                        className="group relative rounded-lg bg-brand-beige-card p-5 hover:shadow-md transition-all focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-brand-green"
                    >
                        <span className="absolute inset-0" aria-hidden="true" />
                        <div className="flex items-center justify-between">
                            <p className="text-sm text-stone-500">Search</p>
                            <FlaskConical className="h-5 w-5 text-brand-green shrink-0" />
                        </div>
                        <p className="mt-2 text-base font-medium text-stone-900">Search Trials</p>
                        <p className="mt-1 text-base text-stone-600 leading-normal">Search saved trials by title or status</p>
                        <ArrowRight className="mt-3 h-4 w-4 text-stone-400 transition-transform group-hover:translate-x-1 group-hover:text-brand-green" />
                    </Link>
                    <Link
                        to="/saved-trials"
                        className="group relative rounded-lg bg-brand-beige-card p-5 hover:shadow-md transition-all focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-blue-500"
                    >
                        <span className="absolute inset-0" aria-hidden="true" />
                        <div className="flex items-center justify-between">
                            <p className="text-sm text-stone-500">Review</p>
                            <Bookmark className="h-5 w-5 text-blue-600 shrink-0" />
                        </div>
                        <p className="mt-2 text-base font-medium text-stone-900">View Saved Trials</p>
                        <p className="mt-1 text-base text-stone-600 leading-normal">See trials you're tracking</p>
                        <ArrowRight className="mt-3 h-4 w-4 text-stone-400 transition-transform group-hover:translate-x-1 group-hover:text-blue-600" />
                    </Link>
                    {/* A link, not a run-it-here button: every other card on this page navigates,
                        and pulling without preparing for search leaves trials that silently do
                        not turn up in results. The Process Trials page does the whole job. */}
                    <Link
                        to="/ingestion"
                        className="group relative rounded-lg bg-brand-beige-card p-5 hover:shadow-md transition-all focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-brand-green"
                    >
                        <span className="absolute inset-0" aria-hidden="true" />
                        <div className="flex items-center justify-between">
                            <p className="text-sm text-stone-500">Admin</p>
                            <Download className="h-5 w-5 text-brand-green shrink-0" />
                        </div>
                        <p className="mt-2 text-base font-medium text-stone-900">Process Trials</p>
                        <p className="mt-1 text-base text-stone-600 leading-normal">Pull new trials and prepare them for search</p>
                        <ArrowRight className="mt-3 h-4 w-4 text-stone-400 transition-transform group-hover:translate-x-1 group-hover:text-brand-green" />
                    </Link>
                </div>
            </div>
        </div>
    );
}
