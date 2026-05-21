import React from 'react';

export const Health = () => {
  return (
    <div className="p-8 max-w-6xl mx-auto">
      <h1 className="text-4xl font-bold mb-4">❤️ Health & Biometrics</h1>
      <p className="text-gray-600 mb-6">Track your health data and fitness profiles</p>

      <div className="grid grid-cols-2 gap-6">
        <div className="bg-white border border-gray-200 p-6 rounded-lg">
          <h2 className="text-2xl font-bold mb-4">Biometric Entries</h2>
          <div className="text-gray-600 p-4 bg-gray-50 rounded">
            No entries yet. Start logging your health data.
          </div>
        </div>

        <div className="bg-white border border-gray-200 p-6 rounded-lg">
          <h2 className="text-2xl font-bold mb-4">Health Profiles</h2>
          <div className="text-gray-600 p-4 bg-gray-50 rounded">
            Create or view your health profiles.
          </div>
        </div>
      </div>

      <div className="mt-6 flex gap-4">
        <button className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700">
          ➕ Log Entry
        </button>
        <button className="bg-green-600 text-white px-6 py-2 rounded-lg hover:bg-green-700">
          ➕ New Profile
        </button>
      </div>
    </div>
  );
};
