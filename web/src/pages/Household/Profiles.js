import React, { useState, useEffect } from 'react';

/**
 * Household Profiles Page - Manage household members
 */
export const Profiles = () => {
  const [loading, setLoading] = useState(true);
  const [error] = useState(null);

  useEffect(() => {
    setLoading(false);
  }, []);

  if (loading) return <div>Loading profiles...</div>;
  if (error) return <div className="text-red-600">Error: {error}</div>;

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Household Profiles</h1>
      {/* TODO: Add profile list and management UI */}
      <p>Profile management coming soon</p>
    </div>
  );
};

export default Profiles;
