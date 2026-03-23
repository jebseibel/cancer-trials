// Base types
export interface Profile {
    extid: string;
    nickname: string;
    fullname: string;
}

export interface ProfileRequest {
    nickname: string;
    fullname: string;
}

export interface Company {
    extid: string;
    code: string;
    name: string;
    description?: string;
}

export interface CompanyRequest {
    code: string;
    name: string;
    description?: string;
}

export interface Customer {
    extid: string;
    code: string;
    name: string;
    contactName: string;
    description: string;
    contactEmail: string;
    contactPhone: string;
}

export interface CustomerRequest {
    code: string;
    name: string;
    contactName: string;
    description: string;
    contactEmail: string;
    contactPhone: string;
}

export interface Purchase {
    extid: string;
    customer: string;
    items: string;
    status: string;
}

export interface PurchaseRequest {
    customer: string;
    items: string;
    status: string;
}

// Authentication
export interface User {
    extid: string;
    username: string;
    email?: string;
    role: string;
}

export interface UserRequest {
    username: string;
    email?: string;
    password?: string;
    role?: string;
}

export interface LoginRequest {
    username: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    password: string;
    email?: string;
}

export interface AuthResponse {
    token: string;
    username: string;
    email?: string;
    role: string;
}

// Pagination
export interface PageRequest {
    page?: number;
    size?: number;
    sort?: string;
    active?: boolean;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    pageable: {
        offset: number;
        pageNumber: number;
        pageSize: number;
        paged: boolean;
        unpaged: boolean;
    };
    last: boolean;
    numberOfElements: number;
    first: boolean;
    empty: boolean;
}