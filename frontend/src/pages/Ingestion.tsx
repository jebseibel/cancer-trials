import { useEffect, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Database, Download, Loader2 } from 'lucide-react';
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
                title: 'Ingestion Complete',
                lines: [
                    { label: 'Fetched from ClinicalTrials.gov', value: '' },
                    { label: '  Studies fetched', value: data.studiesFetched },
                    { label: '  Staging rows written', value: data.stagingRowsWritten },
                    { label: '  Staging rows skipped (already seen)', value: data.stagingRowsSkipped },
                    { label: '', value: '' },
                    { label: 'Normalized into the database', value: '' },
                    { label: '  Pending rows processed', value: data.pendingRowsProcessed },
                    { label: '  Trials normalized', value: data.trialsNormalized },
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
                title: 'Search Index Updated',
                lines: [
                    { label: 'Trials indexed', value: data.trialsIndexed },
                    { label: 'Chunks written', value: data.chunksWritten },
                    { label: 'Trials skipped (no text to index)', value: data.trialsSkipped },
                ],
                errors: data.errors,
            });
        },
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        ingestMutation.mutate();
    };

    const busy = ingestMutation.isPending || backfillMutation.isPending;
    const elapsed = useElapsedSeconds(busy);

    return (
        <div className="px-4 py-6 sm:px-0">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">Ingest Trials</h1>
            <p className="text-gray-600 mb-6">
                Fetch trials from ClinicalTrials.gov, stage them, and normalize into the database.
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
                            Large pulls take a while and the request stays open until it finishes.
                            Staging is quick; embedding for search is the slow part &mdash; run
                            it separately from the RAG backfill afterwards.
                        </p>
                    )}
                </div>

                <div className="flex flex-wrap items-center gap-3">
                    <button
                        type="submit"
                        disabled={busy}
                        className="inline-flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-md shadow-sm hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {ingestMutation.isPending ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                            <Download className="h-4 w-4" />
                        )}
                        {ingestMutation.isPending ? 'Ingesting...' : 'Run Ingestion'}
                    </button>

                    <button
                        type="button"
                        onClick={() => backfillMutation.mutate()}
                        disabled={busy}
                        className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-md shadow-sm hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {backfillMutation.isPending ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                            <Database className="h-4 w-4" />
                        )}
                        {backfillMutation.isPending ? 'Indexing...' : 'Backfill Search Index'}
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
                    <strong>Two steps.</strong> Ingestion loads trials into the database.
                    Backfill chunks and embeds them so semantic search can find them &mdash;
                    newly ingested trials are not searchable until it runs. Backfill is
                    idempotent, so re-running it is safe.
                </p>
            </form>

            {/* Inline banner rather than alert() - viro uses alert() for this, but a banner is
                less disruptive and keeps the failure visible while you fix it. */}
            {ingestMutation.isError && (
                <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4 mb-6">
                    Ingestion request failed. Check that the backend is running and try again.
                </div>
            )}
            {backfillMutation.isError && (
                <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4 mb-6">
                    Backfill failed. Trials are still safely in the database &mdash; the search
                    index just was not updated. Check that the backend and vector store are
                    running, then re-run Backfill.
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
