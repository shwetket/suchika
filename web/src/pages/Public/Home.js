import React from 'react';

export const Home = () => {
  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h1 className="text-4xl font-bold mb-4">Welcome to Suchika</h1>
      <p className="text-lg text-gray-600 mb-6">
        Personal operations system for Wealth, Household, and Health management.
      </p>
      <div className="bg-blue-50 border border-blue-200 p-6 rounded-lg">
        <p className="text-gray-700">
          Sign in to access your dashboard and manage your personal data.
        </p>
      </div>
    </div>
  );
};
