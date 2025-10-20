import { NextRequest, NextResponse } from 'next/server';
import { TaxFileService } from '@/services/taxFileService';
import { CreateTaxFileRequest } from '@/types/api';

export async function POST(request: NextRequest) {
  try {
    const body: CreateTaxFileRequest = await request.json();

    // Validate required fields
    if (!body.userId || !body.year || body.income === undefined || body.expense === undefined || 
        body.taxRate === undefined || body.deducted === undefined || body.refund === undefined) {
      return NextResponse.json(
        { error: 'All fields are required' },
        { status: 400 }
      );
    }

    // Validate numeric values
    if (body.income < 0 || body.expense < 0 || body.taxRate < 0 || body.taxRate > 1 || 
        body.deducted < 0 || body.refund < 0) {
      return NextResponse.json(
        { error: 'Invalid numeric values. Tax rate should be between 0 and 1 (0-100%)' },
        { status: 400 }
      );
    }

    console.log('Creating tax file with data:', body);
    const taxFileResponse = await TaxFileService.createTaxFile(body);
    console.log('Tax file created successfully:', taxFileResponse);

    return NextResponse.json({
      success: true,
      data: taxFileResponse
    });
  } catch (error) {
    console.error('Tax file creation error:', error);
    return NextResponse.json(
      { error: 'Failed to create tax file' },
      { status: 500 }
    );
  }
}