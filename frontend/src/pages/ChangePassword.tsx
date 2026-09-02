import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, KeyRound } from 'lucide-react';
import axios from 'axios';
import { accountApi } from '../services/api';

/** Matches the backend's @Size(min = 6); kept in sync so the form fails before the request does. */
const MIN_LENGTH = 6;

/**
 * Change your own password.
 *
 * Confirmation field is client-side only - the backend takes one new password, and a typo you
 * cannot see is the failure this prevents. Everything else is checked server-side too, because a
 * form is a convenience, not a security boundary.
 */
export default function ChangePassword() {
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirm, setConfirm] = useState('');
    const [error, setError] = useState('');
    const [done, setDone] = useState('');

    const navigate = useNavigate();

    const mutation = useMutation({
        mutationFn: () => accountApi.changePassword(currentPassword, newPassword),
        onSuccess: (response) => {
            setDone(
                typeof response.data === 'string'
                    ? response.data
                    : 'Password changed.'
            );
            setError('');
            setCurrentPassword('');
            setNewPassword('');
            setConfirm('');
        },
        onError: (err) => {
            // The backend answers with a sentence, so show it rather than a generic failure -
            // "your current password is not correct" is the whole diagnosis.
            const message =
                axios.isAxiosError(err) && typeof err.response?.data === 'string'
                    ? err.response.data
                    : 'Could not change your password.';
            setError(message);
            setDone('');
        },
    });

    const submit = (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setDone('');

        if (newPassword.length < MIN_LENGTH) {
            setError(`The new password must be at least ${MIN_LENGTH} characters.`);
            return;
        }
        if (newPassword !== confirm) {
            setError('The two new passwords do not match.');
            return;
        }
        if (newPassword === currentPassword) {
            setError('The new password is the same as your current one.');
            return;
        }

        mutation.mutate();
    };

    return (
        <div className="px-4 py-6 sm:px-0">
            <Link
                to="/"
                className="mb-2 inline-flex items-center gap-1 text-sm text-stone-600 hover:text-brand-green-hover"
            >
                <ArrowLeft className="h-4 w-4" /> Back
            </Link>

            <h2 className="font-heading mb-1 flex items-center gap-2 text-3xl font-bold text-stone-900">
                <KeyRound className="h-7 w-7 text-brand-green" /> Change password
            </h2>
            <p className="mb-6 text-sm text-stone-600">
                You will stay signed in here. Sessions on other devices keep working until their
                token expires.
            </p>

            {error && (
                <div className="mb-4 max-w-md rounded-md bg-red-50 p-3 text-sm text-red-800" role="alert">
                    {error}
                </div>
            )}

            {done && (
                <div className="mb-4 max-w-md rounded-md bg-green-50 p-3 text-sm text-green-800" role="status">
                    {done}
                </div>
            )}

            <form onSubmit={submit} className="max-w-md space-y-4 rounded-lg bg-brand-beige-card p-6 shadow">
                <Field
                    id="currentPassword"
                    label="Current password"
                    value={currentPassword}
                    onChange={setCurrentPassword}
                    autoComplete="current-password"
                />
                <Field
                    id="newPassword"
                    label="New password"
                    value={newPassword}
                    onChange={setNewPassword}
                    autoComplete="new-password"
                    hint={`At least ${MIN_LENGTH} characters.`}
                />
                <Field
                    id="confirmPassword"
                    label="Confirm new password"
                    value={confirm}
                    onChange={setConfirm}
                    autoComplete="new-password"
                />

                <div className="flex items-center gap-3 pt-2">
                    <button
                        type="submit"
                        disabled={
                            mutation.isPending || !currentPassword || !newPassword || !confirm
                        }
                        className="rounded-lg bg-brand-green px-4 py-2 text-sm font-medium text-white
                            hover:bg-brand-green-hover disabled:opacity-60"
                    >
                        {mutation.isPending ? 'Changing…' : 'Change password'}
                    </button>
                    <button
                        type="button"
                        onClick={() => navigate(-1)}
                        className="rounded-lg border border-stone-300 px-4 py-2 text-sm font-medium
                            text-stone-700 hover:bg-stone-50"
                    >
                        Cancel
                    </button>
                </div>
            </form>
        </div>
    );
}

function Field({
    id,
    label,
    value,
    onChange,
    autoComplete,
    hint,
}: {
    id: string;
    label: string;
    value: string;
    onChange: (v: string) => void;
    autoComplete: string;
    hint?: string;
}) {
    return (
        <div>
            <label htmlFor={id} className="block text-sm font-medium text-stone-700">
                {label}
            </label>
            <input
                id={id}
                name={id}
                type="password"
                value={value}
                onChange={(e) => onChange(e.target.value)}
                // Lets a password manager offer to update the stored entry rather than
                // treating this as a fresh login.
                autoComplete={autoComplete}
                className="mt-1 block w-full rounded-md border border-stone-300 px-3 py-2 text-sm
                    focus:border-brand-green focus:outline-none focus:ring-1 focus:ring-brand-green"
            />
            {hint && <p className="mt-1 text-xs text-stone-500">{hint}</p>}
        </div>
    );
}
