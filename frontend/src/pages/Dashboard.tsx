import { Building2, ShoppingCart, UserCog } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { customerApi, purchaseApi, userApi } from '../services/api';

export default function Dashboard() {
    // Fetch data for stats
    const { data: customersPage } = useQuery({
        queryKey: ['customers'],
        queryFn: async () => {
            const response = await customerApi.getAll();
            return response.data;
        },
    });

    const { data: purchasesPage } = useQuery({
        queryKey: ['purchases'],
        queryFn: async () => {
            const response = await purchaseApi.getAll();
            return response.data;
        },
    });

    const { data: usersPage } = useQuery({
        queryKey: ['users'],
        queryFn: async () => {
            const response = await userApi.getAll();
            return response.data;
        },
    });

    const customers = customersPage?.content || [];
    const purchases = purchasesPage?.content || [];
    const users = usersPage?.content || [];

    return (
        <div className="px-4 py-6 sm:px-0">
            <h2 className="text-3xl font-bold text-gray-900 mb-2">
                Basic Customer System
            </h2>
            <h1 className="text-xl text-gray-600 mb-8">
                Manage customers, purchases, and users
            </h1>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 mb-8">
                <Link to="/customers" className="bg-white overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
                    <div className="p-5">
                        <div className="flex items-center">
                            <div className="flex-shrink-0">
                                <Building2 className="h-6 w-6 text-blue-600" />
                            </div>
                            <div className="ml-5 w-0 flex-1">
                                <dl>
                                    <dt className="text-sm font-medium text-gray-500 truncate">
                                        Customers
                                    </dt>
                                    <dd className="text-lg font-medium text-gray-900">
                                        {customers.length || 0}
                                    </dd>
                                </dl>
                            </div>
                        </div>
                    </div>
                </Link>

                <Link to="/purchases" className="bg-white overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
                    <div className="p-5">
                        <div className="flex items-center">
                            <div className="flex-shrink-0">
                                <ShoppingCart className="h-6 w-6 text-green-600" />
                            </div>
                            <div className="ml-5 w-0 flex-1">
                                <dl>
                                    <dt className="text-sm font-medium text-gray-500 truncate">
                                        Purchases
                                    </dt>
                                    <dd className="text-lg font-medium text-gray-900">
                                        {purchases.length || 0}
                                    </dd>
                                </dl>
                            </div>
                        </div>
                    </div>
                </Link>

                <Link to="/users" className="bg-white overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
                    <div className="p-5">
                        <div className="flex items-center">
                            <div className="flex-shrink-0">
                                <UserCog className="h-6 w-6 text-purple-600" />
                            </div>
                            <div className="ml-5 w-0 flex-1">
                                <dl>
                                    <dt className="text-sm font-medium text-gray-500 truncate">
                                        Users
                                    </dt>
                                    <dd className="text-lg font-medium text-gray-900">
                                        {users.length || 0}
                                    </dd>
                                </dl>
                            </div>
                        </div>
                    </div>
                </Link>
            </div>

            {/* Management Actions */}
            <div className="bg-white shadow rounded-lg p-6 mb-8">
                <h2 className="text-lg font-medium text-gray-900 mb-4">Quick Actions</h2>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <Link
                        to="/customers"
                        className="relative rounded-lg border border-gray-300 bg-white px-6 py-5 shadow-sm flex items-center space-x-3 hover:border-gray-400 focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-blue-500"
                    >
                        <div className="flex-shrink-0">
                            <Building2 className="h-10 w-10 text-blue-600" />
                        </div>
                        <div className="flex-1 min-w-0">
                            <span className="absolute inset-0" aria-hidden="true" />
                            <p className="text-sm font-medium text-gray-900">Manage Customers</p>
                            <p className="text-sm text-gray-500 truncate">
                                Create, edit, or view customers
                            </p>
                        </div>
                    </Link>
                    <Link
                        to="/purchases"
                        className="relative rounded-lg border border-gray-300 bg-white px-6 py-5 shadow-sm flex items-center space-x-3 hover:border-gray-400 focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-green-500"
                    >
                        <div className="flex-shrink-0">
                            <ShoppingCart className="h-10 w-10 text-green-600" />
                        </div>
                        <div className="flex-1 min-w-0">
                            <span className="absolute inset-0" aria-hidden="true" />
                            <p className="text-sm font-medium text-gray-900">Manage Purchases</p>
                            <p className="text-sm text-gray-500 truncate">
                                Create, edit, or view purchases
                            </p>
                        </div>
                    </Link>
                </div>
            </div>
        </div>
    );
}