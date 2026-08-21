import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Search, FlaskConical, Sparkles, AlertTriangle } from 'lucide-react';
import { trialApi, ragSearchApi } from '../services/api';
import { useCurrentPatient } from '../lib/PatientContext';
import { runTier1Checks, summariseTier1 } from '../lib/tier1Matching';
import type { Patient, Trial, TrialSearchMatch, TrialSearchChunkMatch } from '../types/api';

const STATUS_OPTIONS = [
    'RECRUITING',
    'ACTIVE_NOT_RECRUITING',
    'COMPLETED',
    'TERMINATED',
    'WITHDRAWN',
    'NOT_YET_RECRUITING',
];

type Mode = 'keyword' | 'meaning';

export default function TrialSearch() {
    const [mode, setMode] = useState<Mode>('keyword');

    return (
        <div>
            <h1 className="text-3xl font-bold text-gray-900 mb-2">Trial Search</h1>
            <p className="text-gray-600 mb-4">
                {mode === 'keyword'
                    ? 'Search saved trials by title, NCT number, or status.'
                    : 'Describe a situation in plain words and find trials whose text means the same thing.'}
            </p>

            <div className="mb-6 inline-flex rounded-md border border-gray-300 bg-white p-1">
                <ModeButton active={mode === 'keyword'} onClick={() => setMode('keyword')}>
                    <Search className="h-4 w-4" /> By keyword
                </ModeButton>
                <ModeButton active={mode === 'meaning'} onClick={() => setMode('meaning')}>
                    <Sparkles className="h-4 w-4" /> By meaning
                </ModeButton>
            </div>

            {mode === 'keyword' ? <KeywordSearch /> : <MeaningSearch />}
        </div>
    );
}

function ModeButton({
    active,
    onClick,
    children,
}: {
    active: boolean;
    onClick: () => void;
    children: React.ReactNode;
}) {
    return (
        <button
            type="button"
            onClick={onClick}
            className={`inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded min-h-[2.25rem] ${
                active ? 'bg-green-600 text-white' : 'text-gray-700 hover:bg-gray-50'
            }`}
        >
            {children}
        </button>
    );
}

/** The original page: fetch a page of trials and filter them in the browser. */
function KeywordSearch() {
    const [term, setTerm] = useState('');
    const [status, setStatus] = useState('');
    const { patient } = useCurrentPatient();

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
        <>
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
                                {patient && <Tier1Badge patient={patient} trial={trial} />}
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
        </>
    );
}

/**
 * Semantic search over indexed trial text.
 *
 * <p>Runs on submit rather than on keystroke: every call embeds the query and searches the
 * vector store, so search-as-you-type would fire a round trip per character.
 */
function MeaningSearch() {
    const [draft, setDraft] = useState('');
    const [query, setQuery] = useState('');
    const [criteriaOnly, setCriteriaOnly] = useState(true);
    const [recruitingOnly, setRecruitingOnly] = useState(false);

    const { data, isLoading, isError, error } = useQuery({
        queryKey: ['rag-search', query, criteriaOnly, recruitingOnly],
        enabled: query.trim().length > 0,
        queryFn: async () => {
            const response = await ragSearchApi.search({
                query,
                maxTrials: 10,
                criteriaOnly,
                recruitingOnly,
            });
            return response.data;
        },
    });

    return (
        <>
            <form
                onSubmit={(e) => {
                    e.preventDefault();
                    setQuery(draft);
                }}
                className="bg-white shadow rounded-lg p-4 mb-6"
            >
                <div className="flex flex-col sm:flex-row gap-3">
                    <div className="relative flex-1">
                        <Sparkles className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
                        <input
                            type="text"
                            value={draft}
                            onChange={(e) => setDraft(e.target.value)}
                            placeholder="e.g. hormone receptor positive, HER2 negative, spread to the bones"
                            className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-green-500 focus:border-green-500"
                        />
                    </div>
                    <button
                        type="submit"
                        disabled={!draft.trim()}
                        className="px-4 py-2 bg-green-600 text-white rounded-md font-medium hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed min-h-[2.5rem]"
                    >
                        Search
                    </button>
                </div>

                <div className="mt-3 flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-6">
                    <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer min-h-[1.75rem]">
                        <input
                            type="checkbox"
                            checked={criteriaOnly}
                            onChange={(e) => setCriteriaOnly(e.target.checked)}
                            className="h-4 w-4 rounded border-gray-300 text-green-600 focus:ring-green-500"
                        />
                        Match on who can join, not what the trial is about
                    </label>
                    <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer min-h-[1.75rem]">
                        <input
                            type="checkbox"
                            checked={recruitingOnly}
                            onChange={(e) => setRecruitingOnly(e.target.checked)}
                            className="h-4 w-4 rounded border-gray-300 text-green-600 focus:ring-green-500"
                        />
                        Only trials recruiting now
                    </label>
                </div>
            </form>

            {!query && (
                <p className="text-gray-500">
                    Describe a diagnosis, a biomarker, or a treatment history and press Search.
                </p>
            )}
            {isLoading && <p className="text-gray-500">Searching...</p>}
            {isError && (
                <p className="text-red-600">
                    Search failed{error instanceof Error ? `: ${error.message}` : '.'}
                </p>
            )}
            {!isLoading && !isError && query && data?.length === 0 && (
                <p className="text-gray-500">
                    Nothing matched. Trials have to be prepared for search before they can be found
                    this way.
                </p>
            )}

            {data && data.length > 0 && (
                <div className="space-y-3">
                    {data.map((match) => (
                        <MeaningResult key={match.trialExtid} match={match} />
                    ))}
                </div>
            )}
        </>
    );
}

/**
 * One semantic hit, with the text that caused it.
 *
 * <p>The matched text is shown rather than hidden behind a link: a similarity score is not a
 * reason, and a reader has to be able to see what the machine actually matched on.
 */
function MeaningResult({ match }: { match: TrialSearchMatch }) {
    const [showAll, setShowAll] = useState(false);
    const shown = showAll ? match.matches : match.matches.slice(0, 2);

    return (
        <div className="bg-white shadow rounded-lg p-5">
            <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                        <FlaskConical className="h-4 w-4 text-green-600 flex-shrink-0" />
                        <span className="text-xs font-mono text-gray-500">{match.nctId ?? 'No NCT ID'}</span>
                    </div>
                    <Link
                        to={`/trials/${match.trialExtid}`}
                        className="text-lg font-medium text-gray-900 hover:text-green-700"
                    >
                        {match.briefTitle ?? 'Untitled trial'}
                    </Link>
                </div>
                {match.overallStatus && (
                    <span className="flex-shrink-0 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                        {match.overallStatus.replaceAll('_', ' ')}
                    </span>
                )}
            </div>

            <div className="mt-3 space-y-2">
                {shown.map((chunk, i) => (
                    <ChunkLine key={`${chunk.source}-${chunk.ordinal}-${i}`} chunk={chunk} />
                ))}
            </div>

            {match.matches.length > 2 && (
                <button
                    type="button"
                    onClick={() => setShowAll(!showAll)}
                    className="mt-2 text-sm text-green-700 hover:text-green-800 underline min-h-[1.75rem]"
                >
                    {showAll ? 'Show less' : `Show ${match.matches.length - 2} more matched passages`}
                </button>
            )}
        </div>
    );
}

/**
 * A matched passage.
 *
 * <p>An exclusion match is flagged in amber. Matching an exclusion means the text describes
 * who is kept out, so reading it as a fit gets the answer exactly backwards - but the trial
 * still appears, because an exclusion can carry a carve-out that admits the reader.
 */
function ChunkLine({ chunk }: { chunk: TrialSearchChunkMatch }) {
    return (
        <div
            className={`rounded p-2.5 text-sm ${
                chunk.isExclusion ? 'bg-amber-50 text-amber-900' : 'bg-gray-50 text-gray-700'
            }`}
        >
            <div className="flex items-center gap-1.5 mb-1">
                {chunk.isExclusion && <AlertTriangle className="h-3.5 w-3.5 flex-shrink-0" />}
                <span className="text-xs font-medium uppercase tracking-wide opacity-70">
                    {chunk.isExclusion ? 'Would rule someone out' : sourceLabel(chunk.source)}
                </span>
            </div>
            <p className="whitespace-pre-wrap">{chunk.text}</p>
        </div>
    );
}

/** Field names as a reader would say them, not as the chunker names them. */
function sourceLabel(source: string): string {
    switch (source) {
        case 'INCLUSION_CRITERION':
            return 'Who can join';
        case 'EXCLUSION_CRITERION':
            return 'Would rule someone out';
        case 'ELIGIBILITY_UNPARSED':
            return 'Eligibility criteria';
        case 'BRIEF_SUMMARY':
            return 'Summary';
        case 'DETAILED_DESCRIPTION':
            return 'Description';
        case 'INTERVENTION':
            return 'Treatment given';
        case 'OUTCOME':
            return 'What is measured';
        default:
            return source.replaceAll('_', ' ').toLowerCase();
    }
}

/**
 * One-line Tier 1 summary. Amber rather than red on a mismatch, and never hidden from the
 * list - DIAGNOSIS_MATCHING_DESIGN.md section 5 forbids auto-excluding a trial because a
 * check did not match.
 */
function Tier1Badge({ patient, trial }: { patient: Patient; trial: Trial }) {
    const summary = summariseTier1(runTier1Checks(patient, trial));
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
