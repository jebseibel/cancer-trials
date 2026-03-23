import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Edit2, Trash2, X } from 'lucide-react';
import { userApi } from '../services/api';
import type { User, UserRequest } from '../types/api';

export default function Users() {
    const [isCreateOpen, setIsCreateOpen] = useState(false);
    const [editingId, setEditingId] = useState<string | null>(null);
    const [search, setSearch] = useState('');
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [formData, setFormData] = useState<UserRequest>({
        username: '',
        password: '',
        email: '',
        role: 'USER',
    });

    const queryClient = useQueryClient();

    // Fetch users
    const { data: users = [], isLoading } = useQuery({
        queryKey: ['users'],
        queryFn: async () => {
            const response = await userApi.getAll();
            return response.data.content;
        },
    });

    // Filtered users
    const filteredUsers = useMemo(() => {
        return users.filter(
            (user) =>
                user.username.toLowerCase().includes(search.toLowerCase()) ||
                user.email?.toLowerCase().includes(search.toLowerCase())
        );
    }, [users, search]);

    // Create/Update mutation
    const mutation = useMutation({
        mutationFn: async (data: UserRequest) => {
            if (editingId) {
                // Don't send password on update
                const updateData = { ...data };
                delete updateData.password;
                return userApi.update(editingId, updateData);
            } else {
                return userApi.create(data);
            }
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['users'] });
            setSuccessMessage(
                editingId ? 'User updated successfully' : 'User created successfully'
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
        mutationFn: (extid: string) => userApi.delete(extid),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['users'] });
            setSuccessMessage('User deleted successfully');
            setTimeout(() => setSuccessMessage(''), 3000);
        },
        onError: (err: any) => {
            setError(err.response?.data?.message || 'Failed to delete user');
        },
    });

    const resetForm = () => {
        setFormData({
            username: '',
            password: '',
            email: '',
            role: 'USER',
        });
        setEditingId(null);
        setIsCreateOpen(false);
        setError('');
    };

    const handleEdit = (user: User) => {
        setFormData({
            username: user.username,
            password: '',
            email: user.email || '',
            role: user.role,
        });
        setEditingId(user.extid);
        setIsCreateOpen(true);
        setError('');
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (!formData.username.trim()) {
            setError('Username is required');
            return;
        }

        if (!editingId && !formData.password?.trim()) {
            setError('Password is required for new users');
            return;
        }

        if (formData.password && formData.password.length < 6) {
            setError('Password must be at least 6 characters');
            return;
        }

        mutation.mutate(formData);
    };

    const handleDelete = (user: User) => {
        if (window.confirm(`Are you sure you want to delete user "${user.username}"?`)) {
            deleteMutation.mutate(user.extid);
        }
    };

    return (
        <div className="px-4 py-6 sm:px-0">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-900 mb-2">Users</h1>
                <p className="text-gray-600">Manage system users</p>
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
                        placeholder="Search by username or email..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                    />
                </div>
                <button
                    onClick={() => {
                        resetForm();
                        setIsCreateOpen(true);
                    }}
                    className="flex items-center justify-center px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
                >
                    <Plus className="h-5 w-5 mr-2" />
                    New User
                </button>
            </div>

            {/* Modal/Form */}
            {isCreateOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-md w-full max-h-[90vh] overflow-y-auto">
                        <div className="sticky top-0 bg-white border-b p-4 flex justify-between items-center">
                            <h2 className="text-lg font-bold text-gray-900">
                                {editingId ? 'Edit User' : 'Create User'}
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
                                    Username
                                </label>
                                <input
                                    type="text"
                                    maxLength={50}
                                    value={formData.username}
                                    onChange={(e) =>
                                        setFormData({ ...formData, username: e.target.value })
                                    }
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                                    placeholder="Enter username"
                                />
                            </div>

                            {!editingId && (
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Password
                                    </label>
                                    <input
                                        type="password"
                                        maxLength={255}
                                        value={formData.password || ''}
                                        onChange={(e) =>
                                            setFormData({ ...formData, password: e.target.value })
                                        }
                                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                                        placeholder="Enter password"
                                    />
                                </div>
                            )}

                            {editingId && (
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Password (leave blank to keep current)
                                    </label>
                                    <input
                                        type="password"
                                        maxLength={255}
                                        value={formData.password || ''}
                                        onChange={(e) =>
                                            setFormData({ ...formData, password: e.target.value })
                                        }
                                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                                        placeholder="Enter new password (optional)"
                                    />
                                </div>
                            )}

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Email
                                </label>
                                <input
                                    type="email"
                                    maxLength={100}
                                    value={formData.email || ''}
                                    onChange={(e) =>
                                        setFormData({ ...formData, email: e.target.value })
                                    }
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                                    placeholder="Enter email"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Role
                                </label>
                                <select
                                    value={formData.role || 'USER'}
                                    onChange={(e) =>
                                        setFormData({ ...formData, role: e.target.value })
                                    }
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                                >
                                    <option value="USER">User</option>
                                    <option value="ADMIN">Admin</option>
                                    <option value="MANAGER">Manager</option>
                                </select>
                            </div>

                            <div className="flex gap-2 pt-4">
                                <button
                                    type="submit"
                                    disabled={mutation.isPending}
                                    className="flex-1 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:bg-gray-400 transition-colors"
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

            {/* Users Table */}
            <div className="bg-white rounded-lg shadow overflow-hidden">
                {isLoading ? (
                    <div className="p-6 text-center text-gray-500">Loading users...</div>
                ) : filteredUsers.length === 0 ? (
                    <div className="p-6 text-center text-gray-500">
                        No users found. Create one to get started.
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead className="bg-gray-50 border-b">
                                <tr>
                                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-900">
                                        Username
                                    </th>
                                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-900">
                                        Email
                                    </th>
                                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-900">
                                        Role
                                    </th>
                                    <th className="px-6 py-3 text-right text-sm font-medium text-gray-900">
                                        Actions
                                    </th>
                                </tr>
                            </thead>
                            <tbody className="divide-y">
                                {filteredUsers.map((user) => (
                                    <tr key={user.extid} className="hover:bg-gray-50">
                                        <td className="px-6 py-4 text-sm text-gray-900">
                                            {user.username}
                                        </td>
                                        <td className="px-6 py-4 text-sm text-gray-600">
                                            {user.email || '-'}
                                        </td>
                                        <td className="px-6 py-4 text-sm">
                                            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                                                {user.role}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 text-right text-sm space-x-2">
                                            <button
                                                onClick={() => handleEdit(user)}
                                                className="inline-flex items-center text-blue-600 hover:text-blue-700"
                                            >
                                                <Edit2 className="h-4 w-4" />
                                            </button>
                                            <button
                                                onClick={() => handleDelete(user)}
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
