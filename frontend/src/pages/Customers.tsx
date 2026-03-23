import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Edit2, Trash2, X } from 'lucide-react';
import { customerApi } from '../services/api';
import type { Customer, CustomerRequest } from '../types/api';

export default function Customers() {
    const [isCreateOpen, setIsCreateOpen] = useState(false);
    const [editingId, setEditingId] = useState<string | null>(null);
    const [search, setSearch] = useState('');
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [formData, setFormData] = useState<CustomerRequest>({
        code: '',
        name: '',
        contactName: '',
        description: '',
        contactEmail: '',
        contactPhone: '',
    });

    const queryClient = useQueryClient();

    // Fetch customers
    const { data: customers = [], isLoading } = useQuery({
        queryKey: ['customers'],
        queryFn: async () => {
            const response = await customerApi.getAll();
            return response.data.content;
        },
    });

    // Filtered customers
    const filteredCustomers = useMemo(() => {
        return customers.filter(
            (customer) =>
                customer.name.toLowerCase().includes(search.toLowerCase()) ||
                customer.code.toLowerCase().includes(search.toLowerCase())
        );
    }, [customers, search]);

    // Create/Update mutation
    const mutation = useMutation({
        mutationFn: async (data: CustomerRequest) => {
            if (editingId) {
                return customerApi.update(editingId, data);
            } else {
                return customerApi.create(data);
            }
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['customers'] });
            setSuccessMessage(
                editingId ? 'Customer updated successfully' : 'Customer created successfully'
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
        mutationFn: (extid: string) => customerApi.delete(extid),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['customers'] });
            setSuccessMessage('Customer deleted successfully');
            setTimeout(() => setSuccessMessage(''), 3000);
        },
        onError: (err: any) => {
            setError(err.response?.data?.message || 'Failed to delete customer');
        },
    });

    const resetForm = () => {
        setFormData({
            code: '',
            name: '',
            contactName: '',
            description: '',
            contactEmail: '',
            contactPhone: '',
        });
        setEditingId(null);
        setIsCreateOpen(false);
        setError('');
    };

    const handleEdit = (customer: Customer) => {
        setFormData({
            code: customer.code,
            name: customer.name,
            contactName: customer.contactName,
            description: customer.description,
            contactEmail: customer.contactEmail,
            contactPhone: customer.contactPhone,
        });
        setEditingId(customer.extid);
        setIsCreateOpen(true);
        setError('');
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (!formData.code.trim()) {
            setError('Code is required');
            return;
        }
        if (!formData.name.trim()) {
            setError('Name is required');
            return;
        }
        if (!formData.contactName.trim()) {
            setError('Contact Name is required');
            return;
        }
        if (!formData.contactEmail.trim()) {
            setError('Contact Email is required');
            return;
        }
        if (!formData.contactPhone.trim()) {
            setError('Contact Phone is required');
            return;
        }

        mutation.mutate(formData);
    };

    const handleDelete = (customer: Customer) => {
        if (window.confirm(`Are you sure you want to delete "${customer.name}"?`)) {
            deleteMutation.mutate(customer.extid);
        }
    };

    return (
        <div className="px-4 py-6 sm:px-0">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-900 mb-2">Customers</h1>
                <p className="text-gray-600">Manage your customers</p>
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
                        placeholder="Search by name or code..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                </div>
                <button
                    onClick={() => {
                        resetForm();
                        setIsCreateOpen(true);
                    }}
                    className="flex items-center justify-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                >
                    <Plus className="h-5 w-5 mr-2" />
                    New Customer
                </button>
            </div>

            {/* Modal/Form */}
            {isCreateOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-md w-full max-h-[90vh] overflow-y-auto">
                        <div className="sticky top-0 bg-white border-b p-4 flex justify-between items-center">
                            <h2 className="text-lg font-bold text-gray-900">
                                {editingId ? 'Edit Customer' : 'Create Customer'}
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
                                    Code
                                </label>
                                <input
                                    type="text"
                                    maxLength={8}
                                    value={formData.code}
                                    onChange={(e) =>
                                        setFormData({ ...formData, code: e.target.value })
                                    }
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    placeholder="Enter code"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Name
                                </label>
                                <input
                                    type="text"
                                    maxLength={120}
                                    value={formData.name}
                                    onChange={(e) =>
                                        setFormData({ ...formData, name: e.target.value })
                                    }
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    placeholder="Enter name"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Contact Name
                                </label>
                                <input
                                    type="text"
                                    maxLength={255}
                                    value={formData.contactName}
                                    onChange={(e) =>
                                        setFormData({ ...formData, contactName: e.target.value })
                                    }
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    placeholder="Enter contact name"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Contact Email
                                </label>
                                <input
                                    type="email"
                                    maxLength={255}
                                    value={formData.contactEmail}
                                    onChange={(e) =>
                                        setFormData({ ...formData, contactEmail: e.target.value })
                                    }
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    placeholder="Enter email"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Contact Phone
                                </label>
                                <input
                                    type="tel"
                                    maxLength={255}
                                    value={formData.contactPhone}
                                    onChange={(e) =>
                                        setFormData({ ...formData, contactPhone: e.target.value })
                                    }
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    placeholder="Enter phone"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Description
                                </label>
                                <textarea
                                    maxLength={255}
                                    value={formData.description}
                                    onChange={(e) =>
                                        setFormData({ ...formData, description: e.target.value })
                                    }
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    placeholder="Enter description"
                                    rows={3}
                                />
                            </div>

                            <div className="flex gap-2 pt-4">
                                <button
                                    type="submit"
                                    disabled={mutation.isPending}
                                    className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 transition-colors"
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

            {/* Customers Table */}
            <div className="bg-white rounded-lg shadow overflow-hidden">
                {isLoading ? (
                    <div className="p-6 text-center text-gray-500">Loading customers...</div>
                ) : filteredCustomers.length === 0 ? (
                    <div className="p-6 text-center text-gray-500">
                        No customers found. Create one to get started.
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead className="bg-gray-50 border-b">
                                <tr>
                                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-900">
                                        Code
                                    </th>
                                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-900">
                                        Name
                                    </th>
                                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-900">
                                        Contact
                                    </th>
                                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-900">
                                        Email
                                    </th>
                                    <th className="px-6 py-3 text-right text-sm font-medium text-gray-900">
                                        Actions
                                    </th>
                                </tr>
                            </thead>
                            <tbody className="divide-y">
                                {filteredCustomers.map((customer) => (
                                    <tr key={customer.extid} className="hover:bg-gray-50">
                                        <td className="px-6 py-4 text-sm text-gray-900">
                                            {customer.code}
                                        </td>
                                        <td className="px-6 py-4 text-sm text-gray-900">
                                            {customer.name}
                                        </td>
                                        <td className="px-6 py-4 text-sm text-gray-600">
                                            {customer.contactName}
                                        </td>
                                        <td className="px-6 py-4 text-sm text-gray-600">
                                            {customer.contactEmail}
                                        </td>
                                        <td className="px-6 py-4 text-right text-sm space-x-2">
                                            <button
                                                onClick={() => handleEdit(customer)}
                                                className="inline-flex items-center text-blue-600 hover:text-blue-700"
                                            >
                                                <Edit2 className="h-4 w-4" />
                                            </button>
                                            <button
                                                onClick={() => handleDelete(customer)}
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
