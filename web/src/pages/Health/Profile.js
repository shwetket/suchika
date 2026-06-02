import React, { useState, useEffect } from 'react';

/**
 * Health Profile Page - Manage personal health profile
 */
export const HealthProfile = () => {
  const [loading, setLoading] = useState(true);
  const [error] = useState(null);

  useEffect(() => {
    setLoading(false);
  }, []);

  if (loading) return <div>Loading health profile...</div>;
  if (error) return <div className="text-red-600">Error: {error}</div>;

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Health Profile</h1>
      {/* TODO: Add health profile display and edit UI */}
      <p>Health profile management coming soon</p>
    </div>
  );
};

export default HealthProfile;
