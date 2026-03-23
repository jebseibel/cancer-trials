import { Link, Outlet, useNavigate } from 'react-router-dom';
import { Home, Building2, ShoppingCart, UserCog, LogOut } from 'lucide-react';
import { authHelpers } from '../services/api';

export default function Layout() {
    const navigate = useNavigate();

    const handleLogout = () => {
        authHelpers.removeToken();
        navigate('/login');
    };

    return (
        <div className="min-h-screen bg-gray-50">
            {/* Navigation */}
            <nav className="bg-white shadow-sm border-b">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between h-16">
                        <div className="flex">
                            <Link to="/" className="flex items-center px-2 text-gray-900">
                                <Home className="h-8 w-8 text-blue-600" />
                                <span className="ml-2 text-xl font-bold">BCS</span>
                            </Link>
                            <div className="hidden sm:ml-6 sm:flex sm:space-x-8">
                                <Link
                                    to="/"
                                    className="inline-flex items-center px-1 pt-1 text-sm font-medium text-gray-900 border-b-2 border-transparent hover:border-gray-300"
                                >
                                    <Home className="h-4 w-4 mr-2" />
                                    Dashboard
                                </Link>
                                <Link
                                    to="/customers"
                                    className="inline-flex items-center px-1 pt-1 text-sm font-medium text-gray-900 border-b-2 border-transparent hover:border-gray-300"
                                >
                                    <Building2 className="h-4 w-4 mr-2" />
                                    Customers
                                </Link>
                                <Link
                                    to="/purchases"
                                    className="inline-flex items-center px-1 pt-1 text-sm font-medium text-gray-900 border-b-2 border-transparent hover:border-gray-300"
                                >
                                    <ShoppingCart className="h-4 w-4 mr-2" />
                                    Purchases
                                </Link>
                                <Link
                                    to="/users"
                                    className="inline-flex items-center px-1 pt-1 text-sm font-medium text-gray-900 border-b-2 border-transparent hover:border-gray-300"
                                >
                                    <UserCog className="h-4 w-4 mr-2" />
                                    Users
                                </Link>
                            </div>
                        </div>
                        <div className="flex items-center space-x-4">
                            <button
                                onClick={handleLogout}
                                className="text-gray-500 hover:text-gray-700"
                                title="Logout"
                            >
                                <LogOut className="h-5 w-5" />
                            </button>
                        </div>
                    </div>
                </div>
            </nav>

            {/* Main Content */}
            <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
                <Outlet />
            </main>
        </div>
    );
}