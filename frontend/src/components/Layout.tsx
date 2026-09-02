import { useState } from 'react';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import type { LucideIcon } from 'lucide-react';
import { Home, Search, Bookmark, LogOut, FlaskConical, Download, Stethoscope, ListChecks, Menu, X, Heart, Microscope } from 'lucide-react';
import { authHelpers } from '../services/api';
import { useAudience } from '../lib/AudienceContext';
import type { AudienceMode } from '../lib/AudienceContext';

// One list, rendered twice - as the desktop row and as the mobile panel. They cannot drift
// apart, which is what went wrong before: the desktop nav was hidden below `sm` with nothing
// put in its place, so a phone had no way to reach any page but the Dashboard.
//
// adminOnly marks a page gated on the logged-in user's role, not on the Patient/Researcher
// display toggle - that toggle only changes how trial titles read, it says nothing about who
// is allowed to pull and index the corpus. Process Trials stays out of the nav for every
// non-admin account regardless of which title style they have selected.
const NAV_ITEMS: { to: string; label: string; icon: LucideIcon; adminOnly?: boolean }[] = [
    { to: '/', label: 'Dashboard', icon: Home },
    { to: '/ranked-trials', label: 'Trials for You', icon: ListChecks },
    { to: '/trials', label: 'Trial Search', icon: Search },
    { to: '/saved-trials', label: 'Saved Trials', icon: Bookmark },
    { to: '/diagnosis', label: 'Diagnosis', icon: Stethoscope },
    { to: '/ingestion', label: 'Process Trials', icon: Download, adminOnly: true },
];

export default function Layout() {
    const navigate = useNavigate();
    const [menuOpen, setMenuOpen] = useState(false);
    const { mode, setMode } = useAudience();
    const isAdmin = authHelpers.isAdmin();
    const visibleNavItems = NAV_ITEMS.filter((item) => !item.adminOnly || isAdmin);

    const handleLogout = () => {
        authHelpers.removeToken();
        authHelpers.removeUsername();
        authHelpers.removeRole();
        // The remembered patient belongs to the account that just signed out.
        localStorage.removeItem('selectedPatientExtid');
        navigate('/login');
    };

    return (
        <div className="min-h-screen bg-brand-beige">
            {/* Navigation */}
            <nav className="bg-brand-green shadow-md">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between h-16">
                        <div className="flex min-w-0">
                            <Link to="/" className="flex items-center px-2 text-white min-w-0">
                                <FlaskConical className="h-7 w-7 sm:h-8 sm:w-8 shrink-0 text-white" />
                                {/* Shrunk rather than truncated below `sm`. The full name is
                                    reassuring on a page someone opens while anxious, so it keeps
                                    all its words and gives up type size instead. */}
                                <span className="font-heading ml-2 truncate text-base sm:text-xl font-bold">
                                    Breast Cancer Trial Finder
                                </span>
                            </Link>
                            <div className="hidden sm:ml-6 sm:flex sm:space-x-8">
                                {visibleNavItems.map(({ to, label, icon: Icon }) => (
                                    <Link
                                        key={to}
                                        to={to}
                                        className="inline-flex items-center px-1 pt-1 text-sm font-normal text-green-50 border-b-2 border-transparent hover:border-white hover:text-white transition-colors"
                                    >
                                        <Icon className="h-4 w-4 mr-2" />
                                        {label}
                                    </Link>
                                ))}
                            </div>
                        </div>
                        <div className="flex items-center gap-1 sm:gap-4">
                            <button
                                onClick={handleLogout}
                                className="inline-flex h-11 w-11 items-center justify-center text-green-100 hover:text-white"
                                title="Logout"
                                aria-label="Log out"
                            >
                                <LogOut className="h-5 w-5" />
                            </button>
                            {/* The one control that reaches the whole app, so it is a real button
                                with a label rather than a tappable icon. */}
                            <button
                                type="button"
                                onClick={() => setMenuOpen((open) => !open)}
                                className="inline-flex h-11 w-11 items-center justify-center text-green-100 hover:text-white sm:hidden"
                                aria-label={menuOpen ? 'Close menu' : 'Open menu'}
                                aria-expanded={menuOpen}
                                aria-controls="mobile-nav"
                            >
                                {menuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
                            </button>
                        </div>
                    </div>
                </div>

                {/* Its own row, on a visibly darker band than the main bar, rather than
                    squeezed in beside logout - this is a real, named setting that changes how
                    every trial title on the site reads, not a decoration. Desktop only; the
                    mobile slide-down panel below already gives it a dedicated row of its own. */}
                <div className="hidden sm:block border-t border-brand-green-hover bg-brand-green-hover/40">
                    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-2 flex items-center gap-3">
                        <span className="text-xs font-medium text-green-100">Showing trial titles for</span>
                        <AudienceSwitch mode={mode} onChange={setMode} />
                    </div>
                </div>

                {/* A Link changes the route without unmounting Layout, so the panel has to be
                    closed explicitly - otherwise it stays open on top of the page it just
                    navigated to. */}
                {menuOpen && (
                    <div id="mobile-nav" className="border-t border-brand-green-hover sm:hidden bg-brand-green">
                        <div className="px-3 py-3 border-b border-brand-green-hover">
                            <AudienceSwitch mode={mode} onChange={setMode} className="w-full" />
                        </div>
                        <div className="space-y-1 px-2 py-2">
                            {visibleNavItems.map(({ to, label, icon: Icon }) => (
                                <Link
                                    key={to}
                                    to={to}
                                    onClick={() => setMenuOpen(false)}
                                    className="flex min-h-11 items-center rounded-md px-3 py-2 text-base font-normal text-green-50 hover:bg-brand-green-hover hover:text-white"
                                >
                                    <Icon className="mr-3 h-5 w-5 shrink-0 text-green-100" />
                                    {label}
                                </Link>
                            ))}
                        </div>
                    </div>
                )}
            </nav>

            {/* Main Content */}
            {/* Padding lives here unconditionally, so a page never has to remember its own and
                a new one inherits it. Previously each page carried `px-4 ... sm:px-0`, and the
                one page that forgot (RankedTrials) put its cards flush against both edges. */}
            <main className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
                <Outlet />
            </main>
        </div>
    );
}

/**
 * The one control that changes how trial titles read across Trials for You, Trial Search and
 * Saved Trials - see `AudienceContext`. A two-way switch rather than a checkbox: "Patient" and
 * "Researcher" are both real, named modes, not an on/off state with one obvious default.
 */
function AudienceSwitch({
    mode,
    onChange,
    className = '',
}: {
    mode: AudienceMode;
    onChange: (mode: AudienceMode) => void;
    className?: string;
}) {
    return (
        <div
            role="group"
            aria-label="Title style"
            className={`inline-flex rounded-md border border-white/30 bg-brand-green-hover/60 p-0.5 text-sm ${className}`}
        >
            <button
                type="button"
                onClick={() => onChange('patient')}
                aria-pressed={mode === 'patient'}
                className={`inline-flex items-center gap-1.5 rounded px-3 py-1.5 font-medium transition-colors ${
                    mode === 'patient'
                        ? 'bg-white text-brand-green shadow-sm'
                        : 'text-green-50 hover:bg-white/10'
                }`}
            >
                <Heart className="h-4 w-4" />
                Patient
            </button>
            <button
                type="button"
                onClick={() => onChange('researcher')}
                aria-pressed={mode === 'researcher'}
                className={`inline-flex items-center gap-1.5 rounded px-3 py-1.5 font-medium transition-colors ${
                    mode === 'researcher'
                        ? 'bg-white text-brand-green shadow-sm'
                        : 'text-green-50 hover:bg-white/10'
                }`}
            >
                <Microscope className="h-4 w-4" />
                Researcher
            </button>
        </div>
    );
}
