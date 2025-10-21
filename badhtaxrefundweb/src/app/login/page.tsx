'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';

interface UserRefundStatus {
  userId: string;
  firstName: string;
  lastName: string;
  refundStatus: 'PENDING' | 'IN_PROGRESS' | 'APPROVED' | 'REJECTED' | 'ERROR' | 'NO_REFUND';
  refundAmount?: number;
  taxStatus?: 'PENDING' | 'COMPLETED';
  lastUpdated?: string;
}

export default function LoginPage() {
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showCreateUserModal, setShowCreateUserModal] = useState(false);
  const [createUserLoading, setCreateUserLoading] = useState(false);
  const [createUserError, setCreateUserError] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [users, setUsers] = useState<UserRefundStatus[]>([]);
  const [usersLoading, setUsersLoading] = useState(true);
  const router = useRouter();

  // Fetch users with refund status on component mount
  useEffect(() => {
    fetchUsersWithRefundStatus();
  }, []);

  const fetchUsersWithRefundStatus = async () => {
    try {
      setUsersLoading(true);
      const response = await fetch('/api/users/refund-status');
      const data = await response.json();

      if (response.ok) {
        setUsers(data.users || []);
      } else {
        console.error('Failed to fetch users:', data.error);
      }
    } catch (error) {
      console.error('Error fetching users:', error);
    } finally {
      setUsersLoading(false);
    }
  };

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateUserLoading(true);
    setCreateUserError('');

    try {
      const response = await fetch('/api/users', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ firstName, lastName }),
      });

      const data = await response.json();

      if (response.ok) {
        // Add the new user to the list
        const newUser: UserRefundStatus = {
          userId: data.userId,
          firstName: data.firstName,
          lastName: data.lastName,
          refundStatus: 'NO_REFUND',
        };
        setUsers(prev => [newUser, ...prev]);
        
        // Reset form and close modal
        setFirstName('');
        setLastName('');
        setShowCreateUserModal(false);
      } else {
        setCreateUserError(data.error || 'Failed to create user');
      }
    } catch {
      setCreateUserError('Network error. Please try again.');
    } finally {
      setCreateUserLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await fetch('/api/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ userId, password }),
      });

      const data = await response.json();

      if (response.ok) {
        // Store identifiers in localStorage for demo purposes
        localStorage.setItem('userId', data.userId);
        localStorage.setItem('username', data.username);
        router.push('/');
      } else {
        setError(data.error || 'Login failed');
      }
    } catch {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            Sign in to your account
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            Tax Refund System
          </p>
          <p className="mt-1 text-center text-xs text-gray-500">
            Demo password: Chang3m3!
          </p>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="rounded-md shadow-sm -space-y-px">
            <div>
              <label htmlFor="userId" className="sr-only">
                User ID
              </label>
              <input
                id="userId"
                name="userId"
                type="text"
                autoComplete="username"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder="User ID"
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
              />
            </div>
            <div>
              <label htmlFor="password" className="sr-only">
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                autoComplete="current-password"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
          </div>

          {error && (
            <div className="text-red-600 text-sm text-center">{error}</div>
          )}

          <div className="space-y-3">
            <button
              type="submit"
              disabled={loading}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Signing in...' : 'Sign in'}
            </button>
            
            <button
              type="button"
              onClick={() => setShowCreateUserModal(true)}
              className="group relative w-full flex justify-center py-2 px-4 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            >
              Create New User
            </button>
          </div>

          <div className="text-sm text-gray-600">
            <div className="flex justify-between items-center mb-2">
              <p className="text-center">Users & Refund Status</p>
              <button
                onClick={fetchUsersWithRefundStatus}
                disabled={usersLoading}
                className="text-xs bg-gray-100 hover:bg-gray-200 px-2 py-1 rounded"
              >
                {usersLoading ? 'Loading...' : 'Refresh'}
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th scope="col" className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User ID</th>
                    <th scope="col" className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                    <th scope="col" className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Refund Status</th>
                    <th scope="col" className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Amount</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {usersLoading ? (
                    <tr>
                      <td colSpan={4} className="px-4 py-4 text-center text-gray-500">Loading users...</td>
                    </tr>
                  ) : users.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="px-4 py-4 text-center text-gray-500">No users found</td>
                    </tr>
                  ) : (
                    users.map((user) => (
                      <tr key={user.userId}>
                        <td className="px-4 py-2 whitespace-nowrap text-xs font-mono text-gray-600">
                          {user.userId}
                        </td>
                        <td className="px-4 py-2 whitespace-nowrap">
                          {user.firstName} {user.lastName}
                        </td>
                        <td className="px-4 py-2 whitespace-nowrap">
                          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            user.refundStatus === 'APPROVED' 
                              ? 'bg-green-100 text-green-800'
                              : user.refundStatus === 'REJECTED' || user.refundStatus === 'ERROR'
                              ? 'bg-red-100 text-red-800'
                              : user.refundStatus === 'IN_PROGRESS'
                              ? 'bg-blue-100 text-blue-800'
                              : user.refundStatus === 'PENDING'
                              ? 'bg-yellow-100 text-yellow-800'
                              : 'bg-gray-100 text-gray-800'
                          }`}>
                            {user.refundStatus}
                          </span>
                        </td>
                        <td className="px-4 py-2 whitespace-nowrap text-sm text-gray-900">
                          {user.refundAmount ? `$${user.refundAmount.toFixed(2)}` : '-'}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </form>
      </div>

      {/* Create User Modal */}
      {showCreateUserModal && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
          <div className="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
            <div className="mt-3">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-medium text-gray-900">Create New User</h3>
                <button
                  onClick={() => setShowCreateUserModal(false)}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              
              <form onSubmit={handleCreateUser} className="space-y-4">
                <div>
                  <label htmlFor="firstName" className="block text-sm font-medium text-gray-700 mb-1">
                    First Name
                  </label>
                  <input
                    type="text"
                    id="firstName"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    placeholder="Enter first name"
                  />
                </div>
                
                <div>
                  <label htmlFor="lastName" className="block text-sm font-medium text-gray-700 mb-1">
                    Last Name
                  </label>
                  <input
                    type="text"
                    id="lastName"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    placeholder="Enter last name"
                  />
                </div>

                {createUserError && (
                  <div className="text-red-600 text-sm">{createUserError}</div>
                )}

                <div className="flex justify-end space-x-3">
                  <button
                    type="button"
                    onClick={() => setShowCreateUserModal(false)}
                    className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={createUserLoading}
                    className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {createUserLoading ? 'Creating...' : 'Create User'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
