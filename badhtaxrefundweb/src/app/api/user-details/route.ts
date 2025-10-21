import { NextRequest, NextResponse } from 'next/server';
import { TaxFileService } from '@/services/taxFileService';
import { UserDetails, TaxUserResponse } from '@/types/api';
import { config } from '@/config/api';

// Interface for user response from backend
interface UserResponse {
  userId: string;
  firstName: string;
  lastName: string;
  createdAt: string;
  updatedAt: string;
}

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const userId = searchParams.get('userId');

    if (!userId) {
      return NextResponse.json(
        { error: 'userId is required' },
        { status: 400 }
      );
    }

    // Fetch user details from backend service
    let userDetails: UserDetails;
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
      userDetails = {
        userId: user.userId,
        username: `${user.firstName} ${user.lastName}`,
        taxYears: [] // Will be populated from tax files
      };
    } catch (fetchError) {
      console.error('Error fetching user from backend:', fetchError);
      return NextResponse.json(
        { error: 'User not found' },
        { status: 404 }
      );
    }

    // Try to get all tax files for the user using the new /taxUser endpoint
    try {
      console.log(`Fetching all tax files for user ${userId}`);
      const taxUserResponse: TaxUserResponse = await TaxFileService.getTaxFilesByUser(userId);
      console.log(`Successfully fetched tax files for user ${userId}:`, taxUserResponse);

      // Extract years from the tax files
      const taxYears = taxUserResponse.taxFiles.map(file => file.year).sort((a, b) => b - a);

      return NextResponse.json({
        success: true,
        data: {
          ...userDetails,
          taxYears: taxYears.length > 0 ? taxYears : userDetails.taxYears, // fallback to mock data if no files
          taxFileDetails: taxUserResponse.taxFiles
        }
      });
    } catch (error) {
      console.error(`Failed to get tax files for user ${userId}:`, error);
      
      // Fallback to original behavior if the new endpoint fails
      // Use default tax years if none are available from user details
      const defaultTaxYears = [2022, 2023, 2024];
      const taxYearsToFetch = userDetails.taxYears.length > 0 ? userDetails.taxYears : defaultTaxYears;
      
      const taxFilePromises = taxYearsToFetch.map(async (year) => {
        try {
          console.log(`Fetching tax file for user ${userId}, year ${year}`);
          const taxFileResponse = await TaxFileService.getTaxFile(userId, year);
          console.log(`Successfully fetched tax file for user ${userId}, year ${year}:`, taxFileResponse);
          return {
            year,
            taxFile: taxFileResponse,
            error: null
          };
        } catch (error) {
          console.error(`Failed to get tax file for user ${userId}, year ${year}:`, error);
          return {
            year,
            taxFile: null,
            error: { code: 'API_ERROR', message: 'Failed to fetch tax file data' }
          };
        }
      });

      const taxFileResults = await Promise.all(taxFilePromises);
      console.log('Tax file results:', taxFileResults);

      return NextResponse.json({
        success: true,
        data: {
          ...userDetails,
          taxYears: taxYearsToFetch,
          taxFileDetails: taxFileResults
        }
      });
    }
  } catch (error) {
    console.error('User details error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
