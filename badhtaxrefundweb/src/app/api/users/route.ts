import { NextRequest, NextResponse } from 'next/server';
import { config } from '@/config/api';

// Interface for user creation request
interface CreateUserRequest {
  userId: string;
  firstName: string;
  lastName: string;
}

// Interface for user response from backend
interface UserResponse {
  userId: string;
  firstName: string;
  lastName: string;
  createdAt: string;
  updatedAt: string;
}

// Interface for all users response from backend
interface AllUsersResponse {
  users: UserResponse[];
  totalUsers: number;
}

// Generate random user ID
function generateRandomUserId(): string {
  const timestamp = Date.now().toString(36);
  const randomStr = Math.random().toString(36).substring(2, 8);
  return `user-${timestamp}-${randomStr}`;
}

// GET all users
export async function GET() {
  try {
    const response = await fetch(`${config.badhtaxfileservBaseUrl}/user`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`Backend API error: ${response.status}`);
    }

    const data: AllUsersResponse = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error fetching users:', error);
    return NextResponse.json(
      { error: 'Failed to fetch users' },
      { status: 500 }
    );
  }
}

// POST create new user
export async function POST(request: NextRequest) {
  try {
    const { firstName, lastName } = await request.json();

    if (!firstName || !lastName) {
      return NextResponse.json(
        { error: 'First name and last name are required' },
        { status: 400 }
      );
    }

    const userId = generateRandomUserId();
    const createUserRequest: CreateUserRequest = {
      userId,
      firstName,
      lastName,
    };

    const response = await fetch(`${config.badhtaxfileservBaseUrl}/user`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(createUserRequest),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || `Backend API error: ${response.status}`);
    }

    const data: UserResponse = await response.json();
    return NextResponse.json(data, { status: 201 });
  } catch (error) {
    console.error('Error creating user:', error);
    return NextResponse.json(
      { error: error instanceof Error ? error.message : 'Failed to create user' },
      { status: 500 }
    );
  }
}
