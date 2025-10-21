import { NextResponse } from 'next/server';

export async function GET() {
  try {
    const baseUrl = process.env.BADHTAXFILESERV_BASEURL || 'http://localhost:4000';
    
    return NextResponse.json({
      success: true,
      data: {
        badhtaxfileservBaseUrl: baseUrl,
        nodeEnv: process.env.NODE_ENV,
        rawEnvValue: process.env.BADHTAXFILESERV_BASEURL,
        allEnvVars: Object.keys(process.env).filter(key => key.includes('BADHTAX')),
        allProcessEnv: Object.keys(process.env).slice(0, 20), // First 20 env vars
        envVarExists: process.env.BADHTAXFILESERV_BASEURL ? 'YES' : 'NO',
        envVarType: typeof process.env.BADHTAXFILESERV_BASEURL,
        envVarLength: process.env.BADHTAXFILESERV_BASEURL?.length || 0
      }
    });
  } catch (error) {
    console.error('Debug error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
