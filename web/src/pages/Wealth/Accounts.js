import React, { useState, useEffect } from 'react';

/**
 * Wealth Accounts Page - View and manage accounts
 */
export const Accounts = () => {
  const [loading, setLoading] = useState(true);
  const [error] = useState(null);

  useEffect(() => {
    setLoading(false);
  }, []);

  if (loading) return <div>Loading accounts...</div>;
  if (error) return <div className="text-red-600">Error: {error}</div>;

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Accounts</h1>
      {/* TODO: Add account list and management UI */}
      <p>Account management coming soon</p>
    </div>
  );
};

export default Accounts;
