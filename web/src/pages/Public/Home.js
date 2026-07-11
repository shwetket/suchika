import React from 'react';
import { Navigate, Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

export const Home = () => {
  const { isAuthenticated } = useAuth();

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center px-4">
      <div className="max-w-lg w-full text-center">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Suchika</h1>
        <p className="text-sm text-gray-500 mt-1 mb-8">
          Personal operations system for Wealth, Household, and Health management.
        </p>
        <Link
          to="/signin"
          className="bg-indigo-600 text-white px-6 py-2.5 rounded-lg font-medium hover:bg-indigo-700 inline-block"
        >
          Sign In
        </Link>
      </div>
    </div>
  );
};
