import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

const DOMAIN_CARDS = [
  {
    title: 'Profiles',
    description: 'Manage household members',
    to: '/household/profiles',
  },
  {
    title: 'Wealth',
    description: 'Track accounts, transactions and uploads',
    to: '/wealth/accounts',
  },
  {
    title: 'Health',
    description: 'Log vitals and doctor visits',
    to: '/health/vitals',
  },
];

export const Dashboard = () => {
  const { user } = useAuth();

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold text-gray-900">Welcome back, {user?.username}</h1>
        <p className="text-sm text-gray-500 mt-1">What would you like to manage today?</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
        {DOMAIN_CARDS.map(({ title, description, to }) => (
          <Link
            key={to}
            to={to}
            className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 hover:shadow-md hover:border-indigo-200 transition-all block"
          >
            <h2 className="text-base font-semibold text-gray-900 mb-1">{title}</h2>
            <p className="text-sm text-gray-500">{description}</p>
          </Link>
        ))}
      </div>

      <p className="text-xs text-gray-400">Household (Calendar, Inventory) coming in v0.3</p>
    </div>
  );
};
