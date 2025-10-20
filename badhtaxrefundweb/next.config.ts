import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: 'standalone',
  serverExternalPackages: [],
  env: {
    BADHTAXFILESERV_BASEURL: process.env.BADHTAXFILESERV_BASEURL || 'http://localhost:4000',
  },
};

export default nextConfig;
