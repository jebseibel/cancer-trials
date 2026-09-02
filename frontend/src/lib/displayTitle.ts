import type { AudienceMode } from './AudienceContext';

/**
 * Picks which title to show for the current audience mode.
 *
 * In patient mode, falls back to the scientific title when no friendly title has been
 * generated yet - absence must not read as a blank card. Researcher mode always shows the
 * scientific title; it never reads friendlyTitle at all, since a researcher wants
 * ClinicalTrials.gov's own wording regardless of whether a rewrite exists.
 */
export function displayTitle(
    trial: { briefTitle?: string | null; friendlyTitle?: string | null },
    mode: AudienceMode
): string {
    if (mode === 'patient' && trial.friendlyTitle) {
        return trial.friendlyTitle;
    }
    return trial.briefTitle ?? '';
}
