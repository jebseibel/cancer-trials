/**
 * Shared form primitives for the patient-data pages (Diagnosis, Variants, Prior Treatment).
 *
 * Extracted from Diagnosis.tsx unchanged so the three pages stay visually identical rather
 * than drifting apart through copy-paste.
 */

export const inputClass =
    'w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm focus:outline-none focus:ring-green-500 focus:border-green-500';

export function Section({ title, children }: { title: string; children: React.ReactNode }) {
    return (
        <div className="bg-white shadow rounded-lg p-6 mb-6">
            <h2 className="text-lg font-medium text-gray-900 mb-4">{title}</h2>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">{children}</div>
        </div>
    );
}

export function Field({
    label,
    hint,
    required,
    className = '',
    children,
}: {
    label: string;
    hint?: string;
    required?: boolean;
    className?: string;
    children: React.ReactNode;
}) {
    return (
        <div className={className}>
            <label className="block text-sm font-medium text-gray-700 mb-1">
                {label}
                {required && <span className="text-red-600 ml-0.5">*</span>}
            </label>
            {children}
            {hint && <p className="mt-1 text-xs text-gray-500">{hint}</p>}
        </div>
    );
}

export function Select({
    value,
    onChange,
    options,
    labels,
}: {
    value: string;
    onChange: (value: string) => void;
    options: readonly string[];
    /** Optional display text per value. Falls back to the value with underscores spaced out. */
    labels?: Record<string, string>;
}) {
    return (
        <select value={value} onChange={(e) => onChange(e.target.value)} className={inputClass}>
            <option value="">—</option>
            {options.map((o) => (
                <option key={o} value={o}>
                    {labels?.[o] ?? o.replaceAll('_', ' ')}
                </option>
            ))}
        </select>
    );
}

/** Three-state: blank means "not recorded", which is different from "no". */
export function BooleanSelect({
    value,
    onChange,
}: {
    value: string;
    onChange: (value: string) => void;
}) {
    return (
        <select value={value} onChange={(e) => onChange(e.target.value)} className={inputClass}>
            <option value="">—</option>
            <option value="true">Yes</option>
            <option value="false">No</option>
        </select>
    );
}
