# Tax Refund System - NextJS Frontend

A NextJS-based frontend application for the Tax Refund System with authentication and user management features.

## Features

- **Authentication System**: Login screen with username/password validation
- **User Dashboard**: Welcome page with user details and tax year management
- **Mock Data**: Configurable mock data for testing and development
- **Responsive Design**: Built with Tailwind CSS for modern, responsive UI

## Getting Started

### Prerequisites

- Node.js (version 18 or higher)
- npm or yarn

### Installation

1. Navigate to the project directory:
   ```bash
   cd badhtaxrefundweb
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm run dev
   ```

4. Open [http://localhost:3000](http://localhost:3000) in your browser.

### Demo Credentials

- **Email**: Bruce@taxrefund.com
- **Password**: Chang3m3!

## Project Structure

```
src/
├── app/
│   ├── api/
│   │   ├── login/route.ts          # Login API endpoint
│   │   └── user-details/route.ts   # User details API endpoint
│   ├── login/page.tsx              # Login page component
│   ├── page.tsx                    # Home page component
│   └── layout.tsx                  # Root layout
├── data/
│   └── mockData.ts                 # Mock data and helper functions
└── globals.css                     # Global styles
```

## API Endpoints

### POST /api/login
Authenticates a user with email and password.

**Request Body:**
```json
{
  "email": "Bruce@taxrefund.com",
  "password": "Chang3m3!"
}
```

**Response:**
```json
{
  "success": true,
  "username": "Bruce Scott",
  "message": "Login successful"
}
```

### GET /api/user-details?username={username}
Fetches user details including tax years.

**Response:**
```json
{
  "success": true,
  "data": {
    "username": "Bruce Scott",
    "taxYears": [2024, 2023]
  }
}
```

## Customizing Mock Data

To modify user credentials or tax years, edit the `src/data/mockData.ts` file:

```typescript
// Add new users
export const mockUsers: User[] = [
  {
    email: 'Bruce@taxrefund.com',
    password: 'Chang3m3!',
    username: 'Bruce Scott'
  },
  // Add more users here
];

// Modify user details
export const mockUserDetails: UserDetails = {
  username: 'Bruce Scott',
  taxYears: [2024, 2023] // Add or remove tax years
};
```

## Features Overview

### Login Page
- Clean, modern login form
- Input validation
- Error handling
- Demo credentials display

### Home Page
- Personalized welcome message
- Tax year management
- Current year filing reminder
- Left sidebar with past tax years
- Quick action buttons

### Authentication
- Client-side authentication using localStorage
- Automatic redirect to login if not authenticated
- Logout functionality

## Technologies Used

- **Next.js 15**: React framework with App Router
- **TypeScript**: Type-safe JavaScript
- **Tailwind CSS**: Utility-first CSS framework
- **React Hooks**: State management and side effects

## Development

### Available Scripts

- `npm run dev`: Start development server
- `npm run build`: Build for production
- `npm run start`: Start production server
- `npm run lint`: Run ESLint

### Code Structure

The application follows Next.js 15 App Router conventions with:
- Server-side API routes in `app/api/`
- Client-side pages in `app/`
- Shared data and utilities in `src/data/`
- TypeScript interfaces for type safety