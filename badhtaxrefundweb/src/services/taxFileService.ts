import { apiEndpoints } from '@/config/api';
import { 
  CreateTaxFileRequest, 
  TaxFileResponse, 
  RefundResponse, 
  ProcessRefundEventRequest,
  TaxUserResponse
} from '@/types/api';

// API service for communicating with badhtaxfileserv
export class TaxFileService {
  private static async makeRequest<T>(
    url: string, 
    options: RequestInit = {}
  ): Promise<T> {
    try {
      const response = await fetch(url, {
        headers: {
          'Content-Type': 'application/json',
          ...options.headers,
        },
        ...options,
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  // Create a new tax file
  static async createTaxFile(request: CreateTaxFileRequest): Promise<TaxFileResponse> {
    return this.makeRequest<TaxFileResponse>(apiEndpoints.taxFile.create, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // Get tax file by userId and year
  static async getTaxFile(userId: string, year: number): Promise<TaxFileResponse> {
    return this.makeRequest<TaxFileResponse>(apiEndpoints.taxFile.get(userId, year));
  }

  // Get all tax files for a user
  static async getTaxFilesByUser(userId: string): Promise<TaxUserResponse> {
    return this.makeRequest<TaxUserResponse>(apiEndpoints.taxFile.getByUser(userId));
  }

  // Get refund information
  static async getRefund(
    userId?: string, 
    year?: number, 
    fileId?: string
  ): Promise<RefundResponse> {
    return this.makeRequest<RefundResponse>(apiEndpoints.refund.get(userId, year, fileId));
  }

  // Process refund event
  static async processRefundEvent(request: ProcessRefundEventRequest): Promise<void> {
    await this.makeRequest<void>(apiEndpoints.refundEvent.process, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }
}
