import { useState } from 'react';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import type { LucideIcon } from 'lucide-react';
import { Home, Search, Bookmark, LogOut, FlaskConical, Download, Stethoscope, ListChecks, Menu, X } from 'lucide-react';
import { authHelpers } from '../services/api';

// One list, rendered twice - as the desktop row and as the mobile panel. They cannot drift
// apart, which is what went wrong before: the desktop nav was hidden below `sm` with nothing
// put in its place, so a phone had no way to reach any page but the Dashboard.
const NAV_ITEMS: { to: string; label: string; icon: LucideIcon }[] = [
    { to: '/', label: 'Dashboard', icon: Home },
    { to: '/ranked-trials', label: 'Trials for You', icon: ListChecks },
    { to: '/trials', label: 'Trial Search', icon: Search },
    { to: '/saved-trials', label: 'Saved Trials', icon: Bookmark },
    { to: '/diagnosis', label: 'Diagnosis', icon: Stethoscope },
    { to: '/ingestion', label: 'Process Trials', icon: Download },
];

export default function Layout() {
    const navigate = useNavigate();
    const [menuOpen, setMenuOpen] = useState(false);

    const handleLogout = () => {
        authHelpers.removeToken();
        authHelpers.removeUsername();
        authHelpers.removeRole();
        // The remembered patient belongs to the account that just signed out.
        localStorage.removeItem('selectedPatientExtid');
        navigate('/login');
    };

    return (
        <div className="min-h-screen bg-gray-50">
            {/* Navigation */}
            <nav className="bg-white shadow-sm border-b">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between h-16">
                        <div className="flex min-w-0">
                            <Link to="/" className="flex items-center px-2 text-gray-900 min-w-0">
                                <FlaskConical className="h-7 w-7 sm:h-8 sm:w-8 shrink-0 text-green-600" />
                                {/* Shrunk rather than truncated below `sm`. The full name is
                                    reassuring on a page someone opens while anxious, so it keeps
                                    all its words and gives up type size instead. */}
                                <span className="ml-2 truncate text-base sm:text-xl font-bold">
                                    Breast Cancer Trial Finder
                                </span>
                            </Link>
                            <div className="hidden sm:ml-6 sm:flex sm:space-x-8">
                                {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
                                    <Link
                                        key={to}
                                        to={to}
                                        className="inline-flex items-center px-1 pt-1 text-sm font-medium text-gray-900 border-b-2 border-transparent hover:border-gray-300"
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
                                className="inline-flex h-11 w-11 items-center justify-center text-gray-500 hover:text-gray-700"
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
                                className="inline-flex h-11 w-11 items-center justify-center text-gray-600 hover:text-gray-900 sm:hidden"
                                aria-label={menuOpen ? 'Close menu' : 'Open menu'}
                                aria-expanded={menuOpen}
                                aria-controls="mobile-nav"
                            >
                                {menuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
                            </button>
                        </div>
                    </div>
                </div>

                {/* A Link changes the route without unmounting Layout, so the panel has to be
                    closed explicitly - otherwise it stays open on top of the page it just
                    navigated to. */}
                {menuOpen && (
                    <div id="mobile-nav" className="border-t border-gray-200 sm:hidden">
                        <div className="space-y-1 px-2 py-2">
                            {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
                                <Link
                                    key={to}
                                    to={to}
                                    onClick={() => setMenuOpen(false)}
                                    className="flex min-h-11 items-center rounded-md px-3 py-2 text-base font-medium text-gray-900 hover:bg-gray-50"
                                >
                                    <Icon className="mr-3 h-5 w-5 shrink-0 text-gray-500" />
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
