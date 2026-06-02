import React, { useEffect, useState } from 'react';

export const AdminReports = () => {
  const [profileCount, setProfileCount] = useState(0);
  const [apiStatus, setApiStatus] = useState('Pending');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadReports() {
      try {
        // TODO: Hook up shared backend contract data via generated OpenAPI client
        setProfileCount(0);
        setApiStatus('Healthy');
      } catch (err) {
        setError(err.message || 'Unable to load backend contract data');
        setApiStatus('Unavailable');
      } finally {
        setLoading(false);
      }
    }

    loadReports();
  }, []);

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
            <p>
              Health Profiles Loaded:{' '}
              <span className="font-bold text-gray-900">{loading ? 'Loading…' : profileCount}</span>
            </p>
            <p>
              API Contract: <span className="font-bold text-gray-900">/v1/health-profiles</span>
            </p>
            <p>
              Data Source: <span className="font-bold text-gray-900">Shared backend contract</span>
            </p>
          </div>
        </div>

        <div className="bg-white border border-gray-200 p-6 rounded-lg">
          <h2 className="text-2xl font-bold mb-4">System Health</h2>
          <div className="space-y-2 text-gray-600">
            <p>
              API Status:{' '}
              <span
                className={`font-bold ${apiStatus === 'Healthy' ? 'text-green-600' : 'text-red-600'}`}
              >
                {apiStatus === 'Pending'
                  ? 'Checking…'
                  : apiStatus === 'Healthy'
                    ? '✓ Healthy'
                    : '✕ Unavailable'}
              </span>
            </p>
            <p>
              Backend Response:{' '}
              <span className="font-bold text-gray-900">{error ? error : 'OK'}</span>
            </p>
            <p>
              Contract Mode:{' '}
              <span className="font-bold text-gray-900">Shared OpenAPI / typed client</span>
            </p>
          </div>
        </div>
      </div>

      <div className="mt-6">
        <button
          className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700"
          disabled={loading}
        >
          📥 Export Report
        </button>
      </div>
    </div>
  );
};
