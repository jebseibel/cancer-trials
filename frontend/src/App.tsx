import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import TrialSearch from './pages/TrialSearch';
import TrialDetail from './pages/TrialDetail';
import SavedTrials from './pages/SavedTrials';
import Ingestion from './pages/Ingestion';
import PatientRecord from './pages/PatientRecord';

// Create a client
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            refetchOnWindowFocus: false,
            retry: 1,
        },
    },
});

function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <Router>
                <Routes>
                    <Route path="/login" element={<Login />} />
                    <Route
                        path="/"
                        element={
                            <ProtectedRoute>
                                <Layout />
                            </ProtectedRoute>
                        }
                    >
                        <Route index element={<Dashboard />} />
                        <Route path="trials" element={<TrialSearch />} />
                        <Route path="trials/:extid" element={<TrialDetail />} />
                        <Route path="saved-trials" element={<SavedTrials />} />
                        <Route path="diagnosis" element={<PatientRecord />} />
                        {/* Folded into the Diagnosis page as tabs; kept so old links still land. */}
                        <Route path="variants" element={<Navigate to="/diagnosis" replace />} />
                        <Route
                            path="prior-treatment"
                            element={<Navigate to="/diagnosis" replace />}
                        />
                        <Route path="ingestion" element={<Ingestion />} />
                    </Route>
                </Routes>
            </Router>
        </QueryClientProvider>
    );
}

export default App;
