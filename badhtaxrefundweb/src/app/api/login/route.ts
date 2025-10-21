import { NextRequest, NextResponse } from 'next/server';
import { config } from '@/config/api';

// Interface for user response from backend
interface UserResponse {
  userId: string;
  firstName: string;
  lastName: string;
  createdAt: string;
  updatedAt: string;
}

// Mock password validation (in a real app, this would use proper authentication)
// For demo purposes, we'll use a simple password for all users
const DEMO_PASSWORD = 'Chang3m3!';

export async function POST(request: NextRequest) {
  try {
    const { userId, password } = await request.json();

    if (!userId || !password) {
      return NextResponse.json(
        { error: 'User ID and password are required' },
        { status: 400 }
      );
    }

    // Validate password (simple demo validation)
    if (password !== DEMO_PASSWORD) {
      return NextResponse.json(
        { error: 'Invalid credentials' },
        { status: 401 }
      );
    }

    // Fetch user from backend to verify user exists
    try {
      const response = await fetch(`${config.badhtaxfileservBaseUrl}/user/${userId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        return NextResponse.json(
          { error: 'User not found' },
          { status: 404 }
        );
      }

      const user: UserResponse = await response.json();

      return NextResponse.json({
        success: true,
        userId: user.userId,
        username: `${user.firstName} ${user.lastName}`,
        message: 'Login successful'
      });
    } catch (fetchError) {
      console.error('Error fetching user from backend:', fetchError);
      return NextResponse.json(
        { error: 'User not found' },
        { status: 404 }
      );
    }
  } catch (error) {
    console.error('Login error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
