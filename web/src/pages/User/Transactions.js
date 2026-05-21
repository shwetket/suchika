import React from 'react';

export const Transactions = () => {
  return (
    <div className="p-8 max-w-6xl mx-auto">
      <h1 className="text-4xl font-bold mb-4">💰 Transactions</h1>
      <p className="text-gray-600 mb-6">Manage your wealth and transactions</p>

      <div className="bg-white border border-gray-200 p-6 rounded-lg">
        <h2 className="text-2xl font-bold mb-4">Transaction List</h2>
        <div className="text-gray-600 p-4 bg-gray-50 rounded">
          No transactions yet. Start uploading CSV files to track your wealth.
        </div>
      </div>

      <div className="mt-6">
        <button className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700">
          📤 Upload CSV
        </button>
      </div>
    </div>
  );
};
