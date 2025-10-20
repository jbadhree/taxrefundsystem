import { NextRequest, NextResponse } from 'next/server';
import { TaxFileService } from '@/services/taxFileService';

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const userId = searchParams.get('userId');
    const year = searchParams.get('year');
    const fileId = searchParams.get('fileId');

    if (!fileId && (!userId || !year)) {
      return NextResponse.json(
        { error: 'Either fileId or both userId and year are required' },
        { status: 400 }
      );
    }

    let yearNumber: number | undefined;
    if (year) {
      yearNumber = parseInt(year);
      if (isNaN(yearNumber)) {
        return NextResponse.json(
          { error: 'year must be a valid number' },
          { status: 400 }
        );
      }
    }

    const response = await TaxFileService.getRefund(userId || undefined, yearNumber, fileId || undefined);
    
    return NextResponse.json(response, { 
      status: response.success ? 200 : 404 
    });
  } catch (error) {
    console.error('Get refund error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
