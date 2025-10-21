import { NextResponse } from 'next/server';
import { config } from '@/config/api';

// Interface for refund status response
interface RefundStatusResponse {
  userId: string;
  firstName: string;
  lastName: string;
  refundStatus: 'PENDING' | 'IN_PROGRESS' | 'APPROVED' | 'REJECTED' | 'ERROR' | 'NO_REFUND';
  refundAmount?: number;
  taxStatus?: 'PENDING' | 'COMPLETED';
  lastUpdated?: string;
}

// GET refund status for all users
export async function GET() {
  try {
    // First, get all users from the user service
    const usersResponse = await fetch(`${config.badhtaxfileservBaseUrl}/user`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!usersResponse.ok) {
      throw new Error(`User service error: ${usersResponse.status}`);
    }

    const usersData = await usersResponse.json();
    const users = usersData.users || [];

    // For each user, get their refund status
    const refundStatusPromises = users.map(async (user: { userId: string; firstName: string; lastName: string }) => {
      try {
        // Get user's tax files to determine refund status
        const taxFilesResponse = await fetch(
          `${config.badhtaxfileservBaseUrl}/taxFile/taxUser?userId=${user.userId}`,
          {
            method: 'GET',
            headers: {
              'Content-Type': 'application/json',
            },
          }
        );

        let refundStatus: RefundStatusResponse = {
          userId: user.userId,
          firstName: user.firstName,
          lastName: user.lastName,
          refundStatus: 'NO_REFUND',
        };

        if (taxFilesResponse.ok) {
          const taxFilesData = await taxFilesResponse.json();
          const taxFiles = taxFilesData.taxFiles || [];

          if (taxFiles.length > 0) {
            // Get the most recent tax file
            const latestTaxFile = taxFiles.sort((a: { createdAt: string }, b: { createdAt: string }) => 
              new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
            )[0];

            refundStatus = {
              userId: user.userId,
              firstName: user.firstName,
              lastName: user.lastName,
              refundStatus: latestTaxFile.refundStatus || 'PENDING',
              refundAmount: latestTaxFile.refundAmount,
              taxStatus: latestTaxFile.taxStatus,
              lastUpdated: latestTaxFile.updatedAt,
            };
          }
        }

        return refundStatus;
      } catch (error) {
        console.error(`Error getting refund status for user ${user.userId}:`, error);
        return {
          userId: user.userId,
          firstName: user.firstName,
          lastName: user.lastName,
          refundStatus: 'ERROR' as const,
        };
      }
    });

    const refundStatuses = await Promise.all(refundStatusPromises);

    return NextResponse.json({
      users: refundStatuses,
      totalUsers: refundStatuses.length,
    });
  } catch (error) {
    console.error('Error fetching refund statuses:', error);
    return NextResponse.json(
      { error: 'Failed to fetch refund statuses' },
      { status: 500 }
    );
  }
}
