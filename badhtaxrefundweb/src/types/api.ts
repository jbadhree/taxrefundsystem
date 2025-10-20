// TypeScript interfaces matching the Java DTOs from badhtaxfileserv

export interface CreateTaxFileRequest {
  userId: string;
  year: number;
  income: number;
  expense: number;
  taxRate: number;
  deducted: number;
  refund: number;
}

export interface TaxFileResponse {
  success: boolean;
  data?: {
    id: string;
    userId: string;
    year: number;
    status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
    createdAt: string;
    updatedAt: string;
  };
  errorDetail?: {
    code: string;
    message: string;
    details?: string;
  };
}

export interface RefundResponse {
  success: boolean;
  data?: {
    id: string;
    fileId: string;
    userId: string;
    year: number;
    amount: number;
    status: 'PENDING' | 'PROCESSING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';
    createdAt: string;
    updatedAt: string;
  };
  errorDetail?: {
    code: string;
    message: string;
    details?: string;
  };
}

export interface ProcessRefundEventRequest {
  fileId: string;
  type: 'STATUS_UPDATE' | 'ERROR' | 'COMPLETION';
  eventData: {
    status?: string;
    errorDetail?: {
      code: string;
      message: string;
      details?: string;
    };
  };
}

// New interfaces for the /taxUser endpoint
export interface TaxFileSummary {
  fileId: string;
  year: number;
  income: number;
  expense: number;
  taxRate: number;
  deducted: number;
  refundAmount: number;
  taxStatus: 'PENDING' | 'COMPLETED';
  refundStatus?: 'PENDING' | 'IN_PROGRESS' | 'APPROVED' | 'REJECTED' | 'ERROR';
  refundEta?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TaxUserResponse {
  userId: string;
  taxFiles: TaxFileSummary[];
  totalFiles: number;
}

// User-related interfaces for the web app
export interface User {
  id: string;
  email: string;
  username: string;
  description: string;
}

export interface UserDetails {
  userId: string;
  username: string;
  taxYears: number[];
  taxFileDetails?: TaxFileSummary[];
}
