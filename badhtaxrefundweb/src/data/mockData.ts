// Mock data for the tax refund system
export interface User {
  id: string;
  email: string;
  password: string;
  username: string;
  description: string;
}

export interface UserDetails {
  userId: string;
  username: string;
  taxYears: number[];
}

// Mock user credentials
export const mockUsers: User[] = [
  {
    id: 'user-1',
    email: 'bruce@badhtaxrefund.com',
    password: 'Chang3m3!',
    username: 'Bruce Scott',
    description: 'yet to file tax'
  },
  {
    id: 'user-2',
    email: 'adam@badhtaxrefund.com',
    password: 'Chang3m3!',
    username: 'Adam Smith',
    description: 'awaiting refund'
  },
  {
    id: 'user-3',
    email: 'karl@badhtaxrefund.com',
    password: 'Chang3m3!',
    username: 'Karl Popper',
    description: 'refund errors'
  }
];

// Mock user details mapped by user id
export const mockUserDetailsById: Record<string, UserDetails> = {
  'user-1': {
    userId: 'user-1',
    username: 'Bruce Scott',
    taxYears: [2022, 2023, 2024]
  },
  'user-2': {
    userId: 'user-2',
    username: 'Adam Smith',
    taxYears: [2023, 2024, 2025]
  },
  'user-3': {
    userId: 'user-3',
    username: 'Karl Popper',
    taxYears: [2023, 2024, 2025]
  }
};

// Helper function to validate credentials
export function validateCredentials(email: string, password: string): User | null {
  const user = mockUsers.find(u => u.email === email && u.password === password);
  return user || null;
}

// Helper function to get user details
export function getUserDetails(userId: string): UserDetails | null {
  return mockUserDetailsById[userId] ?? null;
}
