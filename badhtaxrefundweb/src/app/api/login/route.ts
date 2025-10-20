import { NextRequest, NextResponse } from 'next/server';
import { TaxFileService } from '@/services/taxFileService';
import { User } from '@/types/api';

// Mock user data for authentication (in a real app, this would come from a user service)
const mockUsers: User[] = [
  {
    id: 'user-1',
    email: 'bruce@badhtaxrefund.com',
    username: 'Bruce Scott',
    description: 'yet to file tax'
  },
  {
    id: 'user-2',
    email: 'adam@badhtaxrefund.com',
    username: 'Adam Smith',
    description: 'awaiting refund'
  },
  {
    id: 'user-3',
    email: 'karl@badhtaxrefund.com',
    username: 'Karl Popper',
    description: 'refund errors'
  }
];

// Mock password validation (in a real app, this would use proper authentication)
const mockPasswords: Record<string, string> = {
  'bruce@badhtaxrefund.com': 'Chang3m3!',
  'adam@badhtaxrefund.com': 'Chang3m3!',
  'karl@badhtaxrefund.com': 'Chang3m3!',
};

export async function POST(request: NextRequest) {
  try {
    const { email, password } = await request.json();

    if (!email || !password) {
      return NextResponse.json(
        { error: 'Email and password are required' },
        { status: 400 }
      );
    }

    // Validate credentials
    const user = mockUsers.find(u => u.email === email);
    const expectedPassword = mockPasswords[email];

    if (!user || password !== expectedPassword) {
      return NextResponse.json(
        { error: 'Invalid credentials' },
        { status: 401 }
      );
    }

    return NextResponse.json({
      success: true,
      userId: user.id,
      username: user.username,
      message: 'Login successful'
    });
  } catch (error) {
    console.error('Login error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
