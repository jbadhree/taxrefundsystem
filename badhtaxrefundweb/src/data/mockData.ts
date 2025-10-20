// Mock data for the tax refund system
export interface User {
  email: string;
  password: string;
  username: string;
}

export interface UserDetails {
  username: string;
  taxYears: number[];
}

// Mock user credentials
export const mockUsers: User[] = [
  {
    email: 'Bruce@taxrefund.com',
    password: 'Chang3m3!',
    username: 'Bruce Scott'
  }
];

// Mock user details
export const mockUserDetails: UserDetails = {
  username: 'Bruce Scott',
  taxYears: [2024, 2023, 2022]
};

// Helper function to validate credentials
export function validateCredentials(email: string, password: string): User | null {
  const user = mockUsers.find(u => u.email === email && u.password === password);
  return user || null;
}

// Helper function to get user details
export function getUserDetails(username: string): UserDetails | null {
  if (username === mockUserDetails.username) {
    return mockUserDetails;
  }
  return null;
}
