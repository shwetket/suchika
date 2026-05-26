import React from 'react';

export const AdminReports = () => {
  return (
    <div className="p-8 max-w-6xl mx-auto">
      <h1 className="text-4xl font-bold mb-4">📊 System Reports</h1>
      <p className="text-gray-600 mb-6">View analytics and system health reports</p>

      <div className="bg-yellow-50 border border-yellow-200 p-4 rounded-lg mb-6">
        <p className="text-yellow-800">⚠️ Admin Access: Sensitive data visible only to admins</p>
      </div>

      <div className="grid grid-cols-2 gap-6">
        <div className="bg-white border border-gray-200 p-6 rounded-lg">
          <h2 className="text-2xl font-bold mb-4">User Activity</h2>
          <div className="space-y-2 text-gray-600">
            <p>Active Users: <span className="font-bold text-gray-900">1</span></p>
            <p>Total Transactions: <span className="font-bold text-gray-900">0</span></p>
            <p>Health Entries: <span className="font-bold text-gray-900">0</span></p>
          </div>
        </div>

        <div className="bg-white border border-gray-200 p-6 rounded-lg">
          <h2 className="text-2xl font-bold mb-4">System Health</h2>
          <div className="space-y-2 text-gray-600">
            <p>API Status: <span className="font-bold text-green-600">✓ Healthy</span></p>
            <p>Database: <span className="font-bold text-green-600">✓ Connected</span></p>
            <p>Uptime: <span className="font-bold text-gray-900">24h</span></p>
          </div>
        </div>
      </div>

      <div className="mt-6">
        <button className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700">
          📥 Export Report
        </button>
      </div>
    </div>
  );
};
