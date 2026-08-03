import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Download, Loader2 } from 'lucide-react';
import { ingestionApi } from '../services/api';
import type { IngestionResult } from '../types/api';

export default function Ingestion() {
    const [condition, setCondition] = useState('');
    const [term, setTerm] = useState('');
    const [location, setLocation] = useState('');
    const [maxStudies, setMaxStudies] = useState(50);

    const mutation = useMutation({
        mutationFn: async () => {
            const response = await ingestionApi.runClinicalTrials({
                condition: condition.trim() || undefined,
                term: term.trim() || undefined,
                location: location.trim() || undefined,
                maxStudies,
            });
            return response.data;
        },
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        mutation.mutate();
    };

    const result: IngestionResult | undefined = mutation.data;

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
                    <label className="block text-sm font-medium text-gray-700 mb-1">Max studies</label>
                    <input
                        type="number"
                        min={1}
                        max={500}
                        value={maxStudies}
                        onChange={(e) => setMaxStudies(Number(e.target.value))}
                        className="w-32 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-green-500 focus:border-green-500"
                    />
                </div>

                <button
                    type="submit"
                    disabled={mutation.isPending}
                    className="inline-flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-md shadow-sm hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    {mutation.isPending ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                        <Download className="h-4 w-4" />
                    )}
                    {mutation.isPending ? 'Ingesting...' : 'Run Ingestion'}
                </button>
            </form>

            {mutation.isError && (
                <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4 mb-6">
                    Ingestion request failed. Check that the backend is running and try again.
                </div>
            )}

            {result && (
                <div className="bg-white shadow rounded-lg p-6">
                    <h2 className="text-lg font-medium text-gray-900 mb-4">Result</h2>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-4">
                        <Stat label="Studies fetched" value={result.studiesFetched} />
                        <Stat label="Staging rows written" value={result.stagingRowsWritten} />
                        <Stat label="Staging rows skipped" value={result.stagingRowsSkipped} />
                        <Stat label="Pending rows processed" value={result.pendingRowsProcessed} />
                        <Stat label="Trials normalized" value={result.trialsNormalized} />
                    </div>

                    {result.ingestErrors.length > 0 && (
                        <ErrorList title="Ingest errors" errors={result.ingestErrors} />
                    )}
                    {result.normalizationErrors.length > 0 && (
                        <ErrorList title="Normalization errors" errors={result.normalizationErrors} />
                    )}
                    {result.ingestErrors.length === 0 && result.normalizationErrors.length === 0 && (
                        <p className="text-sm text-green-700">No errors.</p>
                    )}
                </div>
            )}
        </div>
    );
}

function Stat({ label, value }: { label: string; value: number }) {
    return (
        <div>
            <dt className="text-sm font-medium text-gray-500 truncate">{label}</dt>
            <dd className="text-2xl font-semibold text-gray-900">{value}</dd>
        </div>
    );
}

function ErrorList({ title, errors }: { title: string; errors: string[] }) {
    return (
        <div className="mt-4">
            <h3 className="text-sm font-medium text-red-700 mb-1">{title}</h3>
            <ul className="list-disc list-inside text-sm text-red-600 space-y-0.5">
                {errors.map((err, i) => (
                    <li key={i}>{err}</li>
                ))}
            </ul>
        </div>
    );
}
