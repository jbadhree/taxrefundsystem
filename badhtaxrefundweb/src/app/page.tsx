'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { TaxFileSummary } from '@/types/api';

interface UserDetails {
  userId: string;
  username: string;
  taxYears: number[];
  taxFileDetails?: TaxFileSummary[];
}

export default function HomePage() {
  const [userDetails, setUserDetails] = useState<UserDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedFiles, setExpandedFiles] = useState<Set<string>>(new Set());
  const [showTaxForm, setShowTaxForm] = useState(false);
  const [formLoading, setFormLoading] = useState(false);
  const [formData, setFormData] = useState({
    income: '',
    expense: '',
    taxRate: '',
    deducted: '',
    refund: ''
  });
  const router = useRouter();

  useEffect(() => {
    const userId = localStorage.getItem('userId');
    if (!userId) {
      router.push('/login');
      return;
    }

    fetchUserDetails(userId);
  }, [router]);

  const fetchUserDetails = async (userId: string) => {
    try {
      const response = await fetch(`/api/user-details?userId=${encodeURIComponent(userId)}`);
      const data = await response.json();

      if (response.ok) {
        setUserDetails(data.data);
      } else {
        setError(data.error || 'Failed to fetch user details');
      }
    } catch {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('username');
    router.push('/login');
  };

  const toggleExpanded = (fileId: string) => {
    const newExpanded = new Set(expandedFiles);
    if (newExpanded.has(fileId)) {
      newExpanded.delete(fileId);
    } else {
      newExpanded.add(fileId);
    }
    setExpandedFiles(newExpanded);
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const calculateDaysUntilETA = (etaString: string) => {
    const etaDate = new Date(etaString);
    const today = new Date();
    const diffTime = etaDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  };

  const handleFormChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleTaxFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormLoading(true);
    setError('');

    try {
      const userId = localStorage.getItem('userId');
      if (!userId) {
        throw new Error('User ID not found');
      }

      const currentYear = new Date().getFullYear();
      const requestBody = {
        userId,
        year: currentYear,
        income: parseFloat(formData.income) || 0,
        expense: parseFloat(formData.expense) || 0,
        taxRate: (parseFloat(formData.taxRate) || 0) / 100, // Convert percentage to decimal
        deducted: parseFloat(formData.deducted) || 0,
        refund: parseFloat(formData.refund) || 0
      };

      const response = await fetch('/api/tax-file', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody),
      });

      const data = await response.json();

      if (response.ok) {
        // Refresh user details to get the new tax file
        await fetchUserDetails(userId);
        setShowTaxForm(false);
        setFormData({
          income: '',
          expense: '',
          taxRate: '',
          deducted: '',
          refund: ''
        });
      } else {
        setError(data.error || 'Failed to create tax file');
      }
    } catch {
      setError('Network error. Please try again.');
    } finally {
      setFormLoading(false);
    }
  };

  const currentYear = new Date().getFullYear();
  const hasCurrentYear = userDetails?.taxYears.includes(currentYear) || false;

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-lg">Loading...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-red-600">{error}</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <h1 className="text-3xl font-bold text-gray-900">Tax Refund System</h1>
            <button
              onClick={handleLogout}
              className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-md text-sm font-medium"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <div className="max-w-4xl mx-auto">
            {/* Main Content */}
            <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="px-4 py-5 sm:p-6">
                  <h2 className="text-2xl font-bold text-gray-900 mb-6">
                    Welcome, {userDetails?.username}!
                  </h2>

                  {/* Current Year Tax Filing Message */}
                  {!hasCurrentYear && (
                    <div className="mb-6 p-4 bg-yellow-50 border-l-4 border-yellow-400">
                      <div className="flex items-center justify-between">
                        <div className="flex">
                          <div className="flex-shrink-0">
                            <svg className="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor">
                              <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                            </svg>
                          </div>
                          <div className="ml-3">
                            <p className="text-sm text-yellow-700">
                              <strong>Action Required:</strong> You haven&apos;t filed taxes for {currentYear} yet. 
                              Please file your taxes for the current year.
                            </p>
                          </div>
                        </div>
                        <button
                          onClick={() => setShowTaxForm(!showTaxForm)}
                          className="ml-4 bg-yellow-600 hover:bg-yellow-700 text-white px-4 py-2 rounded-md text-sm font-medium"
                        >
                          {showTaxForm ? 'Cancel' : 'File Tax Return'}
                        </button>
                      </div>
                    </div>
                  )}

                  {/* Tax Filing Form */}
                  {showTaxForm && !hasCurrentYear && (
                    <div className="mb-6 p-6 bg-white border border-gray-200 rounded-lg shadow-sm">
                      <h3 className="text-lg font-medium text-gray-900 mb-4">File Tax Return for {currentYear}</h3>
                      <form onSubmit={handleTaxFormSubmit} className="space-y-4">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div>
                            <label htmlFor="income" className="block text-sm font-medium text-gray-700 mb-1">
                              Income ($)
                            </label>
                            <input
                              type="number"
                              id="income"
                              name="income"
                              value={formData.income}
                              onChange={handleFormChange}
                              min="0"
                              step="0.01"
                              required
                              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-gray-900"
                              placeholder="Enter your income"
                            />
                          </div>
                          <div>
                            <label htmlFor="expense" className="block text-sm font-medium text-gray-700 mb-1">
                              Expenses ($)
                            </label>
                            <input
                              type="number"
                              id="expense"
                              name="expense"
                              value={formData.expense}
                              onChange={handleFormChange}
                              min="0"
                              step="0.01"
                              required
                              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-gray-900"
                              placeholder="Enter your expenses"
                            />
                          </div>
                          <div>
                            <label htmlFor="taxRate" className="block text-sm font-medium text-gray-700 mb-1">
                              Tax Rate (0-100%)
                            </label>
                            <input
                              type="number"
                              id="taxRate"
                              name="taxRate"
                              value={formData.taxRate}
                              onChange={handleFormChange}
                              min="0"
                              max="100"
                              step="0.1"
                              required
                              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-gray-900"
                              placeholder="e.g., 22 for 22%"
                            />
                          </div>
                          <div>
                            <label htmlFor="deducted" className="block text-sm font-medium text-gray-700 mb-1">
                              Tax Deducted ($)
                            </label>
                            <input
                              type="number"
                              id="deducted"
                              name="deducted"
                              value={formData.deducted}
                              onChange={handleFormChange}
                              min="0"
                              step="0.01"
                              required
                              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-gray-900"
                              placeholder="Enter tax deducted"
                            />
                          </div>
                          <div className="md:col-span-2">
                            <label htmlFor="refund" className="block text-sm font-medium text-gray-700 mb-1">
                              Refund Amount ($)
                            </label>
                            <input
                              type="number"
                              id="refund"
                              name="refund"
                              value={formData.refund}
                              onChange={handleFormChange}
                              min="0"
                              step="0.01"
                              required
                              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-gray-900"
                              placeholder="Enter expected refund amount"
                            />
                          </div>
                        </div>
                        
                        {error && (
                          <div className="text-red-600 text-sm">{error}</div>
                        )}
                        
                        <div className="flex justify-end space-x-3">
                          <button
                            type="button"
                            onClick={() => setShowTaxForm(false)}
                            className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md"
                          >
                            Cancel
                          </button>
                          <button
                            type="submit"
                            disabled={formLoading}
                            className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
                          >
                            {formLoading ? 'Filing...' : 'File Tax Return'}
                          </button>
                        </div>
                      </form>
                    </div>
                  )}

                  {/* Tax Files Summary */}
                  <div className="mb-6">
                    <h3 className="text-lg font-medium text-gray-900 mb-3">Your Tax Filing History</h3>
                    {userDetails?.taxFileDetails && userDetails.taxFileDetails.length > 0 ? (
                      <div className="space-y-4">
                        {userDetails.taxFileDetails
                          .sort((a, b) => b.year - a.year)
                          .map((taxFile) => (
                            <div key={taxFile.fileId} className="bg-white border border-gray-200 rounded-lg shadow-sm">
                              <div 
                                className="p-4 cursor-pointer hover:bg-gray-50"
                                onClick={() => toggleExpanded(taxFile.fileId)}
                              >
                                <div className="flex items-center justify-between">
                                  <div className="flex-1">
                                    <h4 className="font-medium text-gray-900">Tax Year {taxFile.year}</h4>
                                    <div className="flex items-center space-x-4 mt-1">
                                      <span className="text-sm text-gray-600">
                                        File ID: {taxFile.fileId}
                                      </span>
                                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                                        taxFile.taxStatus === 'COMPLETED' 
                                          ? 'bg-green-100 text-green-800' 
                                          : 'bg-yellow-100 text-yellow-800'
                                      }`}>
                                        {taxFile.taxStatus}
                                      </span>
                                      {taxFile.refundStatus && (
                                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                                          taxFile.refundStatus === 'APPROVED' 
                                            ? 'bg-green-100 text-green-800'
                                            : taxFile.refundStatus === 'REJECTED' || taxFile.refundStatus === 'ERROR'
                                            ? 'bg-red-100 text-red-800'
                                            : 'bg-blue-100 text-blue-800'
                                        }`}>
                                          Refund: {taxFile.refundStatus}
                                        </span>
                                      )}
                                      {taxFile.refundEta && (
                                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                                          ETA: {calculateDaysUntilETA(taxFile.refundEta)} days
                                        </span>
                                      )}
                                    </div>
                                  </div>
                                  <div className="flex items-center space-x-2">
                                    <div className="text-right">
                                      <div className="text-sm font-medium text-gray-900">
                                        {formatCurrency(taxFile.refundAmount)}
                                      </div>
                                      <div className="text-xs text-gray-500">Refund Amount</div>
                                    </div>
                                    <svg 
                                      className={`w-5 h-5 text-gray-400 transition-transform ${
                                        expandedFiles.has(taxFile.fileId) ? 'rotate-180' : ''
                                      }`} 
                                      fill="none" 
                                      stroke="currentColor" 
                                      viewBox="0 0 24 24"
                                    >
                                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                                    </svg>
                                  </div>
                                </div>
                              </div>
                              
                              {expandedFiles.has(taxFile.fileId) && (
                                <div className="border-t border-gray-200 p-4 bg-gray-50">
                                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <div>
                                      <h5 className="font-medium text-gray-900 mb-2">Financial Details</h5>
                                      <dl className="space-y-1">
                                        <div className="flex justify-between">
                                          <dt className="text-sm text-gray-600">Income:</dt>
                                          <dd className="text-sm font-medium text-gray-900">{formatCurrency(taxFile.income)}</dd>
                                        </div>
                                        <div className="flex justify-between">
                                          <dt className="text-sm text-gray-600">Expenses:</dt>
                                          <dd className="text-sm font-medium text-gray-900">{formatCurrency(taxFile.expense)}</dd>
                                        </div>
                                        <div className="flex justify-between">
                                          <dt className="text-sm text-gray-600">Tax Rate:</dt>
                                          <dd className="text-sm font-medium text-gray-900">{(taxFile.taxRate * 100).toFixed(1)}%</dd>
                                        </div>
                                        <div className="flex justify-between">
                                          <dt className="text-sm text-gray-600">Deducted:</dt>
                                          <dd className="text-sm font-medium text-gray-900">{formatCurrency(taxFile.deducted)}</dd>
                                        </div>
                                        <div className="flex justify-between">
                                          <dt className="text-sm text-gray-600">Refund Amount:</dt>
                                          <dd className="text-sm font-medium text-green-600">{formatCurrency(taxFile.refundAmount)}</dd>
                                        </div>
                                      </dl>
                                    </div>
                                    
                                    <div>
                                      <h5 className="font-medium text-gray-900 mb-2">Status & Timeline</h5>
                                      <dl className="space-y-1">
                                        <div className="flex justify-between">
                                          <dt className="text-sm text-gray-600">Tax Status:</dt>
                                          <dd className="text-sm font-medium text-gray-900">{taxFile.taxStatus}</dd>
                                        </div>
                                        {taxFile.refundStatus && (
                                          <div className="flex justify-between">
                                            <dt className="text-sm text-gray-600">Refund Status:</dt>
                                            <dd className="text-sm font-medium text-gray-900">{taxFile.refundStatus}</dd>
                                          </div>
                                        )}
                                        {taxFile.refundEta && (
                                          <div className="flex justify-between">
                                            <dt className="text-sm text-gray-600">Refund ETA:</dt>
                                            <dd className="text-sm font-medium text-gray-900">
                                              {formatDate(taxFile.refundEta)} 
                                              <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                                                {calculateDaysUntilETA(taxFile.refundEta)} days
                                              </span>
                                            </dd>
                                          </div>
                                        )}
                                        <div className="flex justify-between">
                                          <dt className="text-sm text-gray-600">Created:</dt>
                                          <dd className="text-sm font-medium text-gray-900">{formatDate(taxFile.createdAt)}</dd>
                                        </div>
                                        <div className="flex justify-between">
                                          <dt className="text-sm text-gray-600">Updated:</dt>
                                          <dd className="text-sm font-medium text-gray-900">{formatDate(taxFile.updatedAt)}</dd>
                                        </div>
                                      </dl>
                                    </div>
                                  </div>
                                </div>
                              )}
                            </div>
                          ))}
                      </div>
                    ) : (
                      <div className="text-center py-8">
                        <div className="text-gray-500 mb-2">No tax files found</div>
                        <div className="text-sm text-gray-400">Your tax filing history will appear here once you file your taxes.</div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
  );
}