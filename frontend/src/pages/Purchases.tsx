import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Edit2, Trash2, X } from 'lucide-react';
import { purchaseApi } from '../services/api';
import type { Purchase, PurchaseRequest } from '../types/api';

export default function Purchases() {
    const [isCreateOpen, setIsCreateOpen] = useState(false);
    const [editingId, setEditingId] = useState<string | null>(null);
    const [search, setSearch] = useState('');
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [formData, setFormData] = useState<PurchaseRequest>({
        customer: '',
        items: '',
        status: '',
    });

    const queryClient = useQueryClient();

    // Fetch purchases
    const { data: purchases = [], isLoading } = useQuery({
        queryKey: ['purchases'],
        queryFn: async () => {
            const response = await purchaseApi.getAll();
            return response.data.content;
        },
    });

    // Filtered purchases
    const filteredPurchases = useMemo(() => {
        return purchases.filter(
            (purchase) =>
                purchase.customer.toLowerCase().includes(search.toLowerCase()) ||
                purchase.status.toLowerCase().includes(search.toLowerCase())
        );
    }, [purchases, search]);

    // Create/Update mutation
    const mutation = useMutation({
        mutationFn: async (data: PurchaseRequest) => {
            if (editingId) {
                return purchaseApi.update(editingId, data);
            } else {
                return purchaseApi.create(data);
            }
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['purchases'] });
            setSuccessMessage(
                editingId ? 'Purchase updated successfully' : 'Purchase created successfully'
            );
            resetForm();
            setTimeout(() => setSuccessMessage(''), 3000);
        },
        onError: (err: any) => {
            setError(err.response?.data?.message || 'An error occurred');
        },
    });

    // Delete mutation
    const deleteMutation = useMutation({
        mutationFn: (extid: string) => purchaseApi.delete(extid),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['purchases'] });
            setSuccessMessage('Purchase deleted successfully');
            setTimeout(() => setSuccessMessage(''), 3000);
        },
        onError: (err: any) => {
            setError(err.response?.data?.message || 'Failed to delete purchase');
        },
    });

    const resetForm = () => {
        setFormData({
            customer: '',
            items: '',
            status: '',
        });
        setEditingId(null);
        setIsCreateOpen(false);
        setError('');
    };

    const handleEdit = (purchase: Purchase) => {
        setFormData({
            customer: purchase.customer,
            items: purchase.items,
            status: purchase.status,
        });
        setEditingId(purchase.extid);
        setIsCreateOpen(true);
        setError('');
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (!formData.customer.trim()) {
            setError('Customer is required');
            return;
        }
        if (!formData.items.trim()) {
            setError('Items are required');
            return;
        }
        if (!formData.status.trim()) {
            setError('Status is required');
            return;
        }

        mutation.mutate(formData);
    };

    const handleDelete = (purchase: Purchase) => {
        if (window.confirm(`Are you sure you want to delete this purchase?`)) {
            deleteMutation.mutate(purchase.extid);
        }
    };

    return (
        <div className="px-4 py-6 sm:px-0">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-900 mb-2">Purchases</h1>
                <p className="text-gray-600">Manage your purchases</p>
            </div>

            {/* Messages */}
            {error && (
                <div className="rounded-md bg-red-50 p-4 mb-6">
                    <p className="text-sm text-red-800">{error}</p>
                </div>
            )}
            {successMessage && (
                <div className="rounded-md bg-green-50 p-4 mb-6">
                    <p className="text-sm text-green-800">{successMessage}</p>
                </div>
            )}

            {/* Create Button and Search */}
            <div className="mb-6 flex flex-col sm:flex-row gap-4">
                <div className="flex-1">
                    <input
                        type="text"
                        placeholder="Search by customer or status..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                    />
                </div>
                <button
                    onClick={() => {
                        resetForm();
                        setIsCreateOpen(true);
                    }}
                    className="flex items-center justify-center px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
                >
                    <Plus className="h-5 w-5 mr-2" />
                    New Purchase
                </button>
            </div>

            {/* Modal/Form */}
            {isCreateOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-md w-full max-h-[90vh] overflow-y-auto">
                        <div className="sticky top-0 bg-white border-b p-4 flex justify-between items-center">
                            <h2 className="text-lg font-bold text-gray-900">
                                {editingId ? 'Edit Purchase' : 'Create Purchase'}
                            </h2>
                            <button
                                onClick={resetForm}
                                className="text-gray-400 hover:text-gray-600"
                            >
                                <X className="h-5 w-5" />
                            </button>
                        </div>

                        <form onSubmit={handleSubmit} className="p-4 space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Customer
                                </label>
                                <input
                                    type="text"
                                    maxLength={50}
                                    value={formData.customer}
                                    onChange={(e) =>
                                        setFormData({ ...formData, customer: e.target.value })
                                    }
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                                    placeholder="Enter customer name"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Items
                                </label>
                                <textarea
                                    maxLength={255}
                                    value={formData.items}
                                    onChange={(e) =>
                                        setFormData({ ...formData, items: e.target.value })
                                    }
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                                    placeholder="Enter items (comma separated)"
                                    rows={3}
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Status
                                </label>
                                <select
                                    value={formData.status}
                                    onChange={(e) =>
                                        setFormData({ ...formData, status: e.target.value })
                                    }
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                                >
                                    <option value="">Select status</option>
                                    <option value="pending">Pending</option>
                                    <option value="processing">Processing</option>
                                    <option value="completed">Completed</option>
                                    <option value="cancelled">Cancelled</option>
                                </select>
                            </div>

                            <div className="flex gap-2 pt-4">
                                <button
                                    type="submit"
                                    disabled={mutation.isPending}
                                    className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:bg-gray-400 transition-colors"
                                >
                                    {mutation.isPending ? 'Saving...' : 'Save'}
                                </button>
                                <button
                                    type="button"
                                    onClick={resetForm}
                                    className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
                                >
                                    Cancel
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Purchases Table */}
            <div className="bg-white rounded-lg shadow overflow-hidden">
                {isLoading ? (
                    <div className="p-6 text-center text-gray-500">Loading purchases...</div>
                ) : filteredPurchases.length === 0 ? (
                    <div className="p-6 text-center text-gray-500">
                        No purchases found. Create one to get started.
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead className="bg-gray-50 border-b">
                                <tr>
                                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-900">
                                        Customer
                                    </th>
                                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-900">
                                        Items
                                    </th>
                                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-900">
                                        Status
                                    </th>
                                    <th className="px-6 py-3 text-right text-sm font-medium text-gray-900">
                                        Actions
                                    </th>
                                </tr>
                            </thead>
                            <tbody className="divide-y">
                                {filteredPurchases.map((purchase) => (
                                    <tr key={purchase.extid} className="hover:bg-gray-50">
                                        <td className="px-6 py-4 text-sm text-gray-900">
                                            {purchase.customer}
                                        </td>
                                        <td className="px-6 py-4 text-sm text-gray-600 max-w-xs truncate">
                                            {purchase.items}
                                        </td>
                                        <td className="px-6 py-4 text-sm">
                                            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                                                {purchase.status}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 text-right text-sm space-x-2">
                                            <button
                                                onClick={() => handleEdit(purchase)}
                                                className="inline-flex items-center text-blue-600 hover:text-blue-700"
                                            >
                                                <Edit2 className="h-4 w-4" />
                                            </button>
                                            <button
                                                onClick={() => handleDelete(purchase)}
                                                disabled={deleteMutation.isPending}
                                                className="inline-flex items-center text-red-600 hover:text-red-700 disabled:text-gray-400"
                                            >
                                                <Trash2 className="h-4 w-4" />
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}
