import React, { useState, useEffect } from 'react';

/**
 * Household Inventory Page - View and manage household items
 */
export const Inventory = () => {
  const [loading, setLoading] = useState(true);
  const [error] = useState(null);

  useEffect(() => {
    setLoading(false);
  }, []);

  if (loading) return <div>Loading inventory...</div>;
  if (error) return <div className="text-red-600">Error: {error}</div>;

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Household Inventory</h1>
      {/* TODO: Add inventory list and management UI */}
      <p>Inventory management coming soon</p>
    </div>
  );
};

export default Inventory;
