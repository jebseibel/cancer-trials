import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { patientApi, authHelpers } from '../services/api';
import type { AccessLevel, PatientAccess } from '../types/api';
import { covers } from './accessLevel';
import { clearPendingIntakeDraft } from './diagnosisIntakeDraft';

/**
 * Which patient the app is currently showing, and what the signed-in user may do with it.
 *
 * Replaces `useCurrentAppUser`, which fetched *every* app_user and filtered client-side by
 * username - invisible with one patient, and a disclosure with any sharing at all, since the
 * browser received every patient's row in order to find one. `/patient/mine` returns only what
 * the caller holds a grant to.
 *
 * With a single patient this auto-selects the only result and no switcher ever appears, so the
 * multi-patient case costs nothing until it exists.
 */

const STORAGE_KEY = 'selectedPatientExtid';

interface PatientContextValue {
    /** Every patient the caller may see, best access first. */
    patients: PatientAccess[];
    /** The one currently being shown, or null while loading or when there are none. */
    patient: PatientAccess | null;
    /** What the caller may do with the selected patient. */
    accessLevel: AccessLevel | null;
    /** True when the caller may change the record, not merely read it. */
    canEdit: boolean;
    /** True when the caller owns it, and may therefore share it. */
    isOwner: boolean;
    selectPatient: (extid: string) => void;
    isLoading: boolean;
    isError: boolean;
}

const PatientContext = createContext<PatientContextValue | null>(null);

export function PatientProvider({ children }: { children: React.ReactNode }) {
    const [selectedExtid, setSelectedExtid] = useState<string | null>(
        () => localStorage.getItem(STORAGE_KEY)
    );

    const { data, isLoading, isError } = useQuery({
        queryKey: ['patients', 'mine'],
        queryFn: async () => (await patientApi.mine()).data,
        // Only ask once there is a token; an unauthenticated call would just 401.
        enabled: authHelpers.isAuthenticated(),
    });

    const patients = useMemo(() => data ?? [], [data]);

    // A remembered extid the caller no longer has access to must not produce a wall of 404s -
    // it can go stale when a grant is revoked, or when the database is rebuilt and extids
    // regenerate. Validate against what came back and fall back to the first entry.
    useEffect(() => {
        if (patients.length === 0) return;
        const stillValid = selectedExtid && patients.some((p) => p.extid === selectedExtid);
        if (!stillValid) {
            const fallback = patients[0].extid;
            setSelectedExtid(fallback);
            localStorage.setItem(STORAGE_KEY, fallback);
        }
    }, [patients, selectedExtid]);

    const patient = useMemo(
        () => patients.find((p) => p.extid === selectedExtid) ?? null,
        [patients, selectedExtid]
    );

    const value = useMemo<PatientContextValue>(() => {
        const accessLevel = patient?.accessLevel ?? null;
        return {
            patients,
            patient,
            accessLevel,
            canEdit: covers(accessLevel, 'EDIT_RECORD'),
            isOwner: accessLevel === 'OWNER',
            selectPatient: (extid: string) => {
                // A draft from a document-intake session belongs to whichever record was
                // selected when it was built - it must not bleed into a different patient's
                // forms after switching.
                clearPendingIntakeDraft();
                setSelectedExtid(extid);
                localStorage.setItem(STORAGE_KEY, extid);
            },
            isLoading,
            isError,
        };
    }, [patients, patient, isLoading, isError]);

    return <PatientContext.Provider value={value}>{children}</PatientContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useCurrentPatient(): PatientContextValue {
    const ctx = useContext(PatientContext);
    if (!ctx) {
        throw new Error('useCurrentPatient must be used inside a PatientProvider');
    }
    return ctx;
}
