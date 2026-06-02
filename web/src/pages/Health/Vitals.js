import React, { useState, useEffect } from 'react';

/**
 * Health Vitals Page - View vital readings and trends
 */
export const Vitals = () => {
  const [loading, setLoading] = useState(true);
  const [error] = useState(null);

  useEffect(() => {
    setLoading(false);
  }, []);

  if (loading) return <div>Loading vitals...</div>;
  if (error) return <div className="text-red-600">Error: {error}</div>;

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Health Vitals</h1>
      {/* TODO: Add vital type filter and readings display */}
      <p>Vitals tracking coming soon</p>
    </div>
  );
};

export default Vitals;
