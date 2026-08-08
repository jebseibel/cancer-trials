import { X } from 'lucide-react';

/**
 * One row of a job result. Two conventions, borrowed from viro-server's recon modals:
 *  - label and value both empty  -> renders as vertical space (section separator)
 *  - label starting with two spaces -> renders indented, for grouping under a heading
 */
export interface JobResultLine {
    label: string;
    value: string | number;
}

export interface JobResultContent {
    title: string;
    lines: JobResultLine[];
    /**
     * Job errors, shown as a count plus the first few. Deliberately not folded into `lines`:
     * a 1,000-trial run can produce a long list, and a modal that becomes a wall of text is
     * worse than the inline card it replaces.
     */
    errors?: string[];
}

/** How many individual errors to show before collapsing to "+N more". */
const MAX_ERRORS_SHOWN = 5;

/**
 * Result dialog for a triggered backend job.
 *
 * Deliberately has no job-specific knowledge - each page builds its own `lines`, so this stays
 * usable for ingestion, backfill, or anything added later.
 */
export default function JobResultModal({
    content,
    onClose,
}: {
    content: JobResultContent | null;
    onClose: () => void;
}) {
    if (!content) return null;

    const errors = content.errors ?? [];
    const hiddenErrorCount = Math.max(0, errors.length - MAX_ERRORS_SHOWN);

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
            onClick={onClose}
            role="presentation"
        >
            <div
                className="w-full max-w-md rounded-lg bg-white shadow-xl"
                // Stop backdrop clicks from closing when the click is inside the dialog.
                onClick={(e) => e.stopPropagation()}
                role="dialog"
                aria-modal="true"
                aria-label={content.title}
            >
                <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4">
                    <h2 className="text-lg font-medium text-gray-900">{content.title}</h2>
                    <button
                        type="button"
                        onClick={onClose}
                        aria-label="Close"
                        className="text-gray-400 hover:text-gray-600"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                <div className="px-6 py-4">
                    <dl className="space-y-1">
                        {content.lines.map((line, i) => {
                            // Spacer row.
                            if (!line.label && line.value === '') {
                                return <div key={i} className="h-3" />;
                            }
                            const indented = line.label.startsWith('  ');
                            // A label with no value is a section heading.
                            const isHeading = line.value === '';
                            return (
                                <div
                                    key={i}
                                    className={`flex justify-between text-sm ${indented ? 'pl-4' : ''}`}
                                >
                                    <dt
                                        className={
                                            isHeading
                                                ? 'font-medium text-gray-900'
                                                : 'text-gray-600'
                                        }
                                    >
                                        {line.label.trim()}
                                    </dt>
                                    <dd className="font-medium text-gray-900">{line.value}</dd>
                                </div>
                            );
                        })}
                    </dl>

                    {errors.length > 0 ? (
                        <div className="mt-4 rounded-md border border-amber-200 bg-amber-50 p-3">
                            <p className="text-sm font-medium text-amber-800">
                                {errors.length} error{errors.length === 1 ? '' : 's'}
                            </p>
                            <ul className="mt-1 space-y-1 text-xs text-amber-700">
                                {errors.slice(0, MAX_ERRORS_SHOWN).map((e, i) => (
                                    <li key={i} className="break-words">
                                        {e}
                                    </li>
                                ))}
                            </ul>
                            {hiddenErrorCount > 0 && (
                                <p className="mt-1 text-xs text-amber-700">
                                    +{hiddenErrorCount} more &mdash; see the backend log for the
                                    full list.
                                </p>
                            )}
                        </div>
                    ) : (
                        <p className="mt-4 text-sm text-green-700">No errors.</p>
                    )}
                </div>

                <div className="flex justify-end border-t border-gray-200 px-6 py-3">
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-md bg-gray-100 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200"
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
}
