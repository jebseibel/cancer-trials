import axios from 'axios';
import type {
    Company,
    CompanyRequest,
    Customer,
    CustomerRequest,
    Purchase,
    PurchaseRequest,
    User,
    UserRequest,
    LoginRequest,
    RegisterRequest,
    AuthResponse,
    PageResponse,
} from '../types/api';

// API Configuration
// Use relative path for production, localhost for development
const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Axios interceptor to attach JWT token to requests
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Axios interceptor to handle 403 responses and redirect to login
apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        // Don't redirect on auth endpoints (login/register) - let the form handle errors
        const isAuthEndpoint = error.config?.url?.includes('/auth/');

        if ((error.response?.status === 403 || error.response?.status === 401) && !isAuthEndpoint) {
            // Clear invalid token
            localStorage.removeItem('token');
            // Redirect to login page
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// API Endpoints
export const companyApi = {
    getAll: () => apiClient.get<Company[]>('/company'),
    getById: (extid: string) => apiClient.get<Company>(`/company/${extid}`),
    create: (company: CompanyRequest) => apiClient.post<Company>('/company', company),
    update: (extid: string, company: CompanyRequest) => apiClient.put<Company>(`/company/${extid}`, company),
    delete: (extid: string) => apiClient.delete(`/company/${extid}`),
};

export const customerApi = {
    getAll: () => apiClient.get<PageResponse<Customer>>('/customer'),
    getById: (extid: string) => apiClient.get<Customer>(`/customer/${extid}`),
    create: (customer: CustomerRequest) => apiClient.post<Customer>('/customer', customer),
    update: (extid: string, customer: CustomerRequest) => apiClient.put<Customer>(`/customer/${extid}`, customer),
    delete: (extid: string) => apiClient.delete(`/customer/${extid}`),
};

export const purchaseApi = {
    getAll: () => apiClient.get<PageResponse<Purchase>>('/purchase'),
    getById: (extid: string) => apiClient.get<Purchase>(`/purchase/${extid}`),
    create: (purchase: PurchaseRequest) => apiClient.post<Purchase>('/purchase', purchase),
    update: (extid: string, purchase: PurchaseRequest) => apiClient.put<Purchase>(`/purchase/${extid}`, purchase),
    delete: (extid: string) => apiClient.delete(`/purchase/${extid}`),
};

export const userApi = {
    getAll: () => apiClient.get<PageResponse<User>>('/user'),
    getById: (extid: string) => apiClient.get<User>(`/user/${extid}`),
    create: (user: UserRequest) => apiClient.post<User>('/user', user),
    update: (extid: string, user: UserRequest) => apiClient.put<User>(`/user/${extid}`, user),
    delete: (extid: string) => apiClient.delete(`/user/${extid}`),
};

export const authApi = {
    login: (credentials: LoginRequest) => apiClient.post<AuthResponse>('/auth/login', credentials),
    register: (userData: RegisterRequest) => apiClient.post<AuthResponse>('/auth/register', userData),
};

// Auth helper functions
export const authHelpers = {
    saveToken: (token: string) => localStorage.setItem('token', token),
    getToken: () => localStorage.getItem('token'),
    removeToken: () => localStorage.removeItem('token'),
    isAuthenticated: () => !!localStorage.getItem('token'),
};