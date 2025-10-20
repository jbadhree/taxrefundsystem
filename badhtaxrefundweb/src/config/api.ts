// Configuration utility for API endpoints
// Hardcoded URLs for production and development
export const config = {
  badhtaxfileservBaseUrl: 'https://badhtaxfileserv-lumy2fdqia-uc.a.run.app',
};

// API endpoint helpers
export const apiEndpoints = {
  taxFile: {
    create: `${config.badhtaxfileservBaseUrl}/taxFile`,
    get: (userId: string, year: number) => 
      `${config.badhtaxfileservBaseUrl}/taxFile?userId=${userId}&year=${year}`,
    getByUser: (userId: string) => 
      `${config.badhtaxfileservBaseUrl}/taxFile/taxUser?userId=${userId}`,
  },
  refund: {
    get: (userId?: string, year?: number, fileId?: string) => {
      const params = new URLSearchParams();
      if (userId) params.append('userId', userId);
      if (year) params.append('year', year.toString());
      if (fileId) params.append('fileId', fileId);
      return `${config.badhtaxfileservBaseUrl}/refund?${params.toString()}`;
    },
  },
  refundEvent: {
    process: `${config.badhtaxfileservBaseUrl}/processRefundEvent`,
  },
};
