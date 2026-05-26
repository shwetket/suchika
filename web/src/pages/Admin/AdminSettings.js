import React from 'react';

export const AdminSettings = () => {
  return (
    <div className="p-8 max-w-6xl mx-auto">
      <h1 className="text-4xl font-bold mb-4">⚙️ System Settings</h1>
      <p className="text-gray-600 mb-6">Configure system-wide settings and preferences</p>

      <div className="bg-yellow-50 border border-yellow-200 p-4 rounded-lg mb-6">
        <p className="text-yellow-800">⚠️ Admin Access: Changes here affect all users</p>
      </div>

      <div className="space-y-6">
        <div className="bg-white border border-gray-200 p-6 rounded-lg">
          <h2 className="text-2xl font-bold mb-4">Application Settings</h2>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-3 bg-gray-50 rounded">
              <label className="font-semibold">Enable User Registration</label>
              <input type="checkbox" defaultChecked className="w-5 h-5" />
            </div>
            <div className="flex items-center justify-between p-3 bg-gray-50 rounded">
              <label className="font-semibold">Require Email Verification</label>
              <input type="checkbox" className="w-5 h-5" />
            </div>
            <div className="flex items-center justify-between p-3 bg-gray-50 rounded">
              <label className="font-semibold">Enable Demo Mode</label>
              <input type="checkbox" defaultChecked className="w-5 h-5" />
            </div>
          </div>
        </div>

        <div className="bg-white border border-gray-200 p-6 rounded-lg">
          <h2 className="text-2xl font-bold mb-4">Database Settings</h2>
          <p className="text-gray-600 text-sm mb-4">Current DB: PostgreSQL + MongoDB</p>
          <button className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700">
            🔄 Sync Database
          </button>
        </div>
      </div>

      <div className="mt-6">
        <button className="bg-green-600 text-white px-6 py-2 rounded-lg hover:bg-green-700">
          💾 Save Changes
        </button>
      </div>
    </div>
  );
};
