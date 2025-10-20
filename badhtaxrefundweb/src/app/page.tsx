'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

interface UserDetails {
  username: string;
  taxYears: number[];
}

export default function HomePage() {
  const [userDetails, setUserDetails] = useState<UserDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const router = useRouter();

  useEffect(() => {
    const username = localStorage.getItem('username');
    if (!username) {
      router.push('/login');
      return;
    }

    fetchUserDetails(username);
  }, [router]);

  const fetchUserDetails = async (username: string) => {
    try {
      const response = await fetch(`/api/user-details?username=${encodeURIComponent(username)}`);
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
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Left Sidebar - Past Years */}
            <div className="lg:col-span-1">
              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="px-4 py-5 sm:p-6">
                  <h3 className="text-lg font-medium text-gray-900 mb-4">Past Tax Years</h3>
                  <nav className="space-y-2">
                    {userDetails?.taxYears
                      .sort((a, b) => b - a) // Sort in descending order
                      .map((year) => (
                        <Link
                          key={year}
                          href="#"
                          className="block px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-md"
                        >
                          Tax Year {year}
                        </Link>
                      ))}
                  </nav>
                </div>
              </div>
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3">
              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="px-4 py-5 sm:p-6">
                  <h2 className="text-2xl font-bold text-gray-900 mb-6">
                    Welcome, {userDetails?.username}!
                  </h2>

                  {/* Current Year Tax Filing Message */}
                  {!hasCurrentYear && (
                    <div className="mb-6 p-4 bg-yellow-50 border-l-4 border-yellow-400">
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
                    </div>
                  )}

                  {/* Tax Years Summary */}
                  <div className="mb-6">
                    <h3 className="text-lg font-medium text-gray-900 mb-3">Your Tax Filing History</h3>
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                      {userDetails?.taxYears
                        .sort((a, b) => b - a)
                        .map((year) => (
                          <div key={year} className="bg-gray-50 p-4 rounded-lg">
                            <h4 className="font-medium text-gray-900">Tax Year {year}</h4>
                            <p className="text-sm text-gray-600">Filed</p>
                          </div>
                        ))}
                    </div>
                  </div>

                  {/* Quick Actions */}
                  <div>
                    <h3 className="text-lg font-medium text-gray-900 mb-3">Quick Actions</h3>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <button className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-md text-sm font-medium">
                        File New Tax Return
                      </button>
                      <button className="bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded-md text-sm font-medium">
                        View Previous Returns
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}