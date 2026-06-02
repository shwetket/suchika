import React, { useState, useEffect } from 'react';

/**
 * Household Calendar Page - View and manage events
 */
export const Calendar = () => {
  const [loading, setLoading] = useState(true);
  const [error] = useState(null);

  useEffect(() => {
    setLoading(false);
  }, []);

  if (loading) return <div>Loading calendar...</div>;
  if (error) return <div className="text-red-600">Error: {error}</div>;

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Household Calendar</h1>
      {/* TODO: Add calendar UI and event management */}
      <p>Calendar view coming soon</p>
    </div>
  );
};

export default Calendar;
