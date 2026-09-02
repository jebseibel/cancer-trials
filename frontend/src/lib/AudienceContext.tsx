import { createContext, useContext, useMemo, useState } from 'react';

/**
 * Whether trial titles are shown in plain language for a patient, or in the scientific
 * wording ClinicalTrials.gov actually publishes, for a researcher.
 *
 * One global toggle rather than a per-page control - see `PatientContext` for the same
 * shape (a context backed by localStorage). A reader who switches to Researcher mode on
 * Trial Search almost certainly wants the same view on Trials for You and Saved Trials,
 * and three independent switches would drift out of sync with no benefit.
 *
 * Defaults to 'patient': the tool exists to answer a real question for a real patient and
 * her family, per project-description.md - the scientific wording is the exception a reader
 * opts into, not the default they have to opt out of.
 */

const STORAGE_KEY = 'audienceMode';

export type AudienceMode = 'patient' | 'researcher';

interface AudienceContextValue {
    mode: AudienceMode;
    setMode: (mode: AudienceMode) => void;
    toggle: () => void;
}

const AudienceContext = createContext<AudienceContextValue | null>(null);

function readStored(): AudienceMode {
    return localStorage.getItem(STORAGE_KEY) === 'researcher' ? 'researcher' : 'patient';
}

export function AudienceProvider({ children }: { children: React.ReactNode }) {
    const [mode, setModeState] = useState<AudienceMode>(readStored);

    const value = useMemo<AudienceContextValue>(() => {
        const setMode = (next: AudienceMode) => {
            setModeState(next);
            localStorage.setItem(STORAGE_KEY, next);
        };
        return {
            mode,
            setMode,
            toggle: () => setMode(mode === 'patient' ? 'researcher' : 'patient'),
        };
    }, [mode]);

    return <AudienceContext.Provider value={value}>{children}</AudienceContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAudience(): AudienceContextValue {
    const ctx = useContext(AudienceContext);
    if (!ctx) {
        throw new Error('useAudience must be used inside an AudienceProvider');
    }
    return ctx;
}
