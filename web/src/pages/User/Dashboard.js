import React from 'react';
import { useAuth } from '../../hooks/useAuth';

export const Dashboard = () => {
  const { user } = useAuth();

  return (
    <div className="p-8 max-w-6xl mx-auto">
      <h1 className="text-4xl font-bold mb-4">Dashboard</h1>
      <p className="text-gray-600 mb-6">Welcome back, {user?.username}!</p>

      <div className="grid grid-cols-3 gap-4 mb-8">
        <div className="bg-blue-50 border border-blue-200 p-6 rounded-lg">
          <h2 className="text-xl font-bold mb-2">💰 Wealth</h2>
          <p className="text-gray-600">Manage transactions and assets</p>
        </div>
        <div className="bg-green-50 border border-green-200 p-6 rounded-lg">
          <h2 className="text-xl font-bold mb-2">🏠 Household</h2>
          <p className="text-gray-600">Schedule and inventory</p>
        </div>
        <div className="bg-red-50 border border-red-200 p-6 rounded-lg">
          <h2 className="text-xl font-bold mb-2">❤️ Health</h2>
          <p className="text-gray-600">Biometrics and fitness</p>
        </div>
      </div>

      <div className="bg-gray-50 border border-gray-200 p-6 rounded-lg">
        <h2 className="text-2xl font-bold mb-4">Quick Stats</h2>
        <p className="text-gray-600">Data will appear here when you add information.</p>
      </div>
    </div>
  );
};
