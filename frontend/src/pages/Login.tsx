import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi, authHelpers } from '../services/api';
import { LogIn, UserPlus } from 'lucide-react';

export default function Login() {
    const [isLogin, setIsLogin] = useState(true);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [email, setEmail] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            if (isLogin) {
                const response = await authApi.login({ username, password });
                authHelpers.saveToken(response.data.token);
                authHelpers.saveUsername(response.data.username);
                authHelpers.saveRole(response.data.role);
                navigate('/');
            } else {
                const response = await authApi.register({ username, password, email });
                authHelpers.saveToken(response.data.token);
                authHelpers.saveUsername(response.data.username);
                authHelpers.saveRole(response.data.role);
                navigate('/');
            }
        } catch (err: any) {
            setError(err.response?.data || 'Authentication failed. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-green-50 to-blue-50 flex items-center justify-center px-4">
            <div className="max-w-md w-full space-y-8">
                <div>
                    <h2 className="text-center text-3xl font-extrabold text-gray-900 mb-2">
                        Breast Cancer Trial Finder
                    </h2>
                    <p className="mt-2 text-center text-sm text-gray-600">
                        {isLogin ? 'Sign in to your account' : 'Create a new account'}
                    </p>
                </div>

                <div className="bg-white rounded-lg shadow-xl p-8">
                    <form className="space-y-6" onSubmit={handleSubmit}>
                        <div>
                            <label htmlFor="username" className="block text-sm font-medium text-gray-700">
                                Username
                            </label>
                            <input
                                id="username"
                                name="username"
                                type="text"
                                required
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                className="mt-1 appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-green-500 focus:border-green-500"
                            />
                        </div>

                        {!isLogin && (
                            <div>
                                <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                                    Email (optional)
                                </label>
                                <input
                                    id="email"
                                    name="email"
                                    type="email"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    className="mt-1 appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-green-500 focus:border-green-500"
                                />
                            </div>
                        )}

                        <div>
                            <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                                Password
                            </label>
                            <input
                                id="password"
                                name="password"
                                type="password"
                                required
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                className="mt-1 appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-green-500 focus:border-green-500"
                            />
                        </div>

                        {error && (
                            <div className="rounded-md bg-red-50 p-4">
                                <p className="text-sm text-red-800">{error}</p>
                            </div>
                        )}

                        <div>
                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full flex justify-center items-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {loading ? (
                                    'Processing...'
                                ) : (
                                    <>
                                        {isLogin ? (
                                            <>
                                                <LogIn className="mr-2 h-4 w-4" />
                                                Sign In
                                            </>
                                        ) : (
                                            <>
                                                <UserPlus className="mr-2 h-4 w-4" />
                                                Register
                                            </>
                                        )}
                                    </>
                                )}
                            </button>
                        </div>
                    </form>

                    <div className="mt-6">
                        <div className="relative">
                            <div className="absolute inset-0 flex items-center">
                                <div className="w-full border-t border-gray-300" />
                            </div>
                            <div className="relative flex justify-center text-sm">
                                <span className="px-2 bg-white text-gray-500">
                                    {isLogin ? "Don't have an account?" : 'Already have an account?'}
                                </span>
                            </div>
                        </div>

                        <div className="mt-6">
                            <button
                                type="button"
                                onClick={() => {
                                    setIsLogin(!isLogin);
                                    setError('');
                                    setUsername('');
                                    setPassword('');
                                    setEmail('');
                                }}
                                className="w-full flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
                            >
                                {isLogin ? 'Create an account' : 'Sign in instead'}
                            </button>
                        </div>
                    </div>
                </div>

                {/*
                  Outside the card, so it reads as a note about the site rather than part of
                  signing in. Deliberately worded as "ask how to use it" rather than "request
                  access" - this instance holds one person's medical record, and the offer is
                  help with the tool, not an invitation to browse the data.
                */}
                <p className="text-center text-sm text-gray-600">
                    This tool was built for one patient and her family. If that is not you and
                    you would like to know how to use it, email{' '}
                    <a
                        href="mailto:jeb.seibel@yahoo.com"
                        className="font-medium text-green-700 hover:text-green-800 underline"
                    >
                        jeb.seibel@yahoo.com
                    </a>
                    .
                </p>
            </div>
        </div>
    );
}
