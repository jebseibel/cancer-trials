import { useEffect, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Database, Download, Loader2, PlayCircle, AlertTriangle } from 'lucide-react';
import { ingestionApi, ragApi } from '../services/api';
import { OVERALL_STATUS_OPTIONS } from '../types/api';
import JobResultModal from '../components/JobResultModal';
import type { JobResultContent } from '../components/JobResultModal';

export default function Ingestion() {
    const queryClient = useQueryClient();
    const [condition, setCondition] = useState('');
    const [term, setTerm] = useState('');
    const [location, setLocation] = useState('');
    // Defaults mirror the backend's cancer.ingestion.clinicaltrials.* values.
    const [overallStatus, setOverallStatus] = useState('RECRUITING');
    const [maxStudies, setMaxStudies] = useState(1000);
    const [modalContent, setModalContent] = useState<JobResultContent | null>(null);
    const [confirmOpen, setConfirmOpen] = useState(false);

    const ingestMutation = useMutation({
        mutationFn: async () => {
            const response = await ingestionApi.runClinicalTrials({
                condition: condition.trim() || undefined,
                term: term.trim() || undefined,
                location: location.trim() || undefined,
                overallStatus,
                maxStudies,
            });
            return response.data;
        },
        onSuccess: (data) => {
            setModalContent({
                title: 'Trials Pulled',
                lines: [
                    { label: 'Downloaded from ClinicalTrials.gov', value: '' },
                    { label: '  Trials found', value: data.studiesFetched },
                    { label: '  New trials', value: data.stagingRowsWritten },
                    { label: '  Already had (skipped)', value: data.stagingRowsSkipped },
                    { label: '', value: '' },
                    { label: 'Saved to the database', value: '' },
                    { label: '  Trials processed', value: data.pendingRowsProcessed },
                    { label: '  Trials saved', value: data.trialsNormalized },
                ],
                errors: [...data.ingestErrors, ...data.normalizationErrors],
            });
            // Trial lists are stale until this runs - without it you have to navigate away and
            // back to see newly ingested trials.
            queryClient.invalidateQueries({ queryKey: ['trials'] });
        },
    });

    const backfillMutation = useMutation({
        mutationFn: async () => {
            const response = await ragApi.backfill();
            return response.data;
        },
        onSuccess: (data) => {
            setModalContent({
                title: 'Ready to Search',
                lines: [
                    { label: 'Trials made searchable', value: data.trialsIndexed },
                    { label: 'Sections of text prepared', value: data.chunksWritten },
                    { label: 'Trials skipped (nothing to read)', value: data.trialsSkipped },
                ],
                errors: data.errors,
            });
        },
    });

    // Both steps in one press, using the form values above. Runs them in sequence rather than
    // calling the two mutations - a failed pull must not be followed by a backfill, which would
    // index a half-loaded corpus and report success.
    const processAllMutation = useMutation({
        mutationFn: async () => {
            const ingest = (
                await ingestionApi.runClinicalTrials({
                    condition: condition.trim() || undefined,
                    term: term.trim() || undefined,
                    location: location.trim() || undefined,
                    overallStatus,
                    maxStudies,
                })
            ).data;

            const backfill = (await ragApi.backfill()).data;
            return { ingest, backfill };
        },
        onSuccess: ({ ingest, backfill }) => {
            setModalContent({
                title: 'All Steps Complete',
                lines: [
                    { label: 'Downloaded from ClinicalTrials.gov', value: '' },
                    { label: '  Studies fetched', value: ingest.studiesFetched },
                    { label: '  New trials staged', value: ingest.stagingRowsWritten },
                    { label: '  Already had (skipped)', value: ingest.stagingRowsSkipped },
                    { label: '', value: '' },
                    { label: 'Saved to the database', value: '' },
                    { label: '  Trials saved', value: ingest.trialsNormalized },
                    { label: '', value: '' },
                    { label: 'Made searchable', value: '' },
                    { label: '  Trials made searchable', value: backfill.trialsIndexed },
                    { label: '  Sections of text prepared', value: backfill.chunksWritten },
                ],
                errors: [
                    ...ingest.ingestErrors,
                    ...ingest.normalizationErrors,
                    ...backfill.errors,
                ],
            });
            queryClient.invalidateQueries({ queryKey: ['trials'] });
        },
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        ingestMutation.mutate();
    };

    const busy =
        ingestMutation.isPending || backfillMutation.isPending || processAllMutation.isPending;
    const elapsed = useElapsedSeconds(busy);

    return (
        <div className="px-4 py-6 sm:px-0">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">Process Trials</h1>
            <p className="text-gray-600 mb-6">
                Download trials from ClinicalTrials.gov and prepare them so search can find them.
            </p>

            <form onSubmit={handleSubmit} className="bg-white shadow rounded-lg p-6 mb-6 space-y-4">
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Condition</label>
                    <input
                        type="text"
                        value={condition}
                        onChange={(e) => setCondition(e.target.value)}
                        placeholder="e.g. breast cancer"
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-green-500 focus:border-green-500"
                    />
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Term (optional)</label>
                    <input
                        type="text"
                        value={term}
                        onChange={(e) => setTerm(e.target.value)}
                        placeholder="free-text search term"
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-green-500 focus:border-green-500"
                    />
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Location (optional)</label>
                    <input
                        type="text"
                        value={location}
                        onChange={(e) => setLocation(e.target.value)}
                        placeholder="e.g. Denver, CO"
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-green-500 focus:border-green-500"
                    />
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
                    <select
                        value={overallStatus}
                        onChange={(e) => setOverallStatus(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-green-500 focus:border-green-500"
                    >
                        {OVERALL_STATUS_OPTIONS.map((opt) => (
                            <option key={opt.value} value={opt.value}>
                                {opt.label}
                            </option>
                        ))}
                    </select>
                    <p className="mt-1 text-xs text-gray-500">
                        Recruiting is the only part of the corpus a patient can actually join
                        &mdash; about 15% of trials.
                    </p>
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Max studies</label>
                    <input
                        type="number"
                        min={1}
                        max={50000}
                        value={maxStudies}
                        onChange={(e) => setMaxStudies(Number(e.target.value))}
                        className="w-32 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-green-500 focus:border-green-500"
                    />
                    {maxStudies > 2000 && (
                        <p className="mt-1 text-xs text-amber-700">
                            Large pulls take a while and this page stays open until it finishes.
                            Downloading is quick; preparing for search is the slow part &mdash;
                            for a pull this size, consider running the two steps separately.
                        </p>
                    )}
                </div>

                <div className="flex flex-wrap items-center gap-3">
                    <button
                        type="button"
                        onClick={() => setConfirmOpen(true)}
                        disabled={busy}
                        className="inline-flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-md shadow-sm hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {processAllMutation.isPending ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                            <PlayCircle className="h-4 w-4" />
                        )}
                        {processAllMutation.isPending
                            ? 'Working...'
                            : 'Pull Trials and Prepare for Search'}
                    </button>

                    <button
                        type="submit"
                        disabled={busy}
                        className="inline-flex items-center gap-2 px-4 py-2 bg-white text-gray-700 border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {ingestMutation.isPending ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                            <Download className="h-4 w-4" />
                        )}
                        {ingestMutation.isPending ? 'Pulling...' : 'Pull Trials'}
                    </button>

                    <button
                        type="button"
                        onClick={() => backfillMutation.mutate()}
                        disabled={busy}
                        className="inline-flex items-center gap-2 px-4 py-2 bg-white text-gray-700 border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {backfillMutation.isPending ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                            <Database className="h-4 w-4" />
                        )}
                        {backfillMutation.isPending ? 'Preparing...' : 'Prepare for Search'}
                    </button>

                    {busy && (
                        <span className="text-sm text-gray-500 tabular-nums">
                            {elapsed}s elapsed
                        </span>
                    )}
                </div>

                {/* These are two steps on purpose. Saying so here is what prevents the
                    "I ingested but search finds nothing" confusion. */}
                <p className="text-xs text-gray-500">
                    <strong>Pull Trials and Prepare for Search</strong> does everything in one go.
                    The other two run the same work one step at a time: <strong>Pull Trials</strong>{' '}
                    loads trials into the database, and <strong>Prepare for Search</strong> makes
                    them findable. Newly loaded trials will not appear in search until that has
                    run. Re-running any of these is safe.
                </p>
            </form>

            {/* Inline banner rather than alert() - viro uses alert() for this, but a banner is
                less disruptive and keeps the failure visible while you fix it. */}
            {ingestMutation.isError && (
                <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4 mb-6">
                    Could not pull trials. Check that the server is running and try again.
                </div>
            )}
            {backfillMutation.isError && (
                <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4 mb-6">
                    Could not prepare trials for search. The trials are still safely in the
                    database &mdash; they just will not turn up in search yet. Check that the
                    server is running, then try <strong>Prepare for Search</strong> again.
                </div>
            )}

            {processAllMutation.isError && (
                <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4 mb-6">
                    Something went wrong partway through. Any trials already downloaded are safely
                    in the database &mdash; run <strong>Prepare for Search</strong> on its own to
                    finish making them findable.
                </div>
            )}

            {/* Fires before the long job, not after. This is the button a first-time user
                presses without knowing it is a multi-minute run. */}
            {confirmOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
                    <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
                        <div className="flex items-center gap-2 mb-4">
                            <AlertTriangle className="h-5 w-5 text-amber-500" />
                            <h2 className="text-lg font-semibold text-gray-900">
                                This takes a few minutes
                            </h2>
                        </div>

                        <p className="text-sm text-gray-600 mb-3">This will do two things:</p>
                        <ol className="text-sm text-gray-700 mb-4 space-y-1 list-decimal list-inside">
                            <li>Download matching trials from ClinicalTrials.gov and save them</li>
                            <li>Prepare them so search can find them</li>
                        </ol>

                        <div className="rounded-md bg-gray-50 border border-gray-200 p-3 mb-4 text-sm">
                            <div className="flex justify-between py-0.5">
                                <span className="text-gray-500">Condition</span>
                                <span className="font-medium text-gray-900">
                                    {condition.trim() || 'default'}
                                </span>
                            </div>
                            <div className="flex justify-between py-0.5">
                                <span className="text-gray-500">Status</span>
                                <span className="font-medium text-gray-900">{overallStatus}</span>
                            </div>
                            <div className="flex justify-between py-0.5">
                                <span className="text-gray-500">Max trials</span>
                                <span className="font-medium text-gray-900">{maxStudies}</span>
                            </div>
                        </div>

                        <p className="text-sm text-amber-800 bg-amber-50 border border-amber-200 rounded-md px-3 py-2 mb-5">
                            Leave this tab open until it finishes. Nothing is lost if you close
                            it, but the job will not complete.
                        </p>

                        <div className="flex justify-end gap-3">
                            <button
                                type="button"
                                onClick={() => setConfirmOpen(false)}
                                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
                            >
                                Cancel
                            </button>
                            <button
                                type="button"
                                onClick={() => {
                                    setConfirmOpen(false);
                                    processAllMutation.mutate();
                                }}
                                className="px-4 py-2 text-sm font-medium text-white bg-green-600 rounded-md hover:bg-green-700"
                            >
                                Start
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <JobResultModal content={modalContent} onClose={() => setModalContent(null)} />
        </div>
    );
}

/**
 * Seconds elapsed while a job runs. Ingesting 1,000 trials takes ~80s and a full pull is far
 * longer, so a bare spinner reads as hung. Real progress reporting needs backend job tracking
 * (see FRONTEND_JOB_TRIGGER_PLAN.md); this is the cheap version that shows it is alive.
 */
function useElapsedSeconds(running: boolean): number {
    const [elapsed, setElapsed] = useState(0);
    const startedAt = useRef<number | null>(null);

    useEffect(() => {
        if (!running) {
            startedAt.current = null;
            setElapsed(0);
            return;
        }
        startedAt.current = Date.now();
        const id = setInterval(() => {
            if (startedAt.current !== null) {
                setElapsed(Math.floor((Date.now() - startedAt.current) / 1000));
            }
        }, 1000);
        return () => clearInterval(id);
    }, [running]);

    return elapsed;
}
