import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export const Navigation = ({ theme, onToggleTheme }) => {
  const { user, logout, hasRole } = useAuth();

  return (
    <nav className="bg-gray-800 text-white p-4">
      <div className="flex justify-between items-center max-w-7xl mx-auto">
        <Link to="/" className="text-xl font-bold">
          Suchika
        </Link>

        <div className="flex gap-4 items-center">
          <button
            type="button"
            onClick={onToggleTheme}
            className="bg-slate-700 text-white px-3 py-1 rounded hover:bg-slate-600"
          >
            {theme === 'dark' ? 'Light Mode' : 'Dark Mode'}
          </button>
          {/* Public Links */}
          <Link to="/" className="hover:text-gray-300">
            Home
          </Link>
          <Link to="/about" className="hover:text-gray-300">
            About
          </Link>

          {/* User Links - visible if user OR admin is logged in */}
          {user && hasRole('user') && (
            <>
              <Link to="/dashboard" className="hover:text-gray-300">
                Dashboard
              </Link>
              <Link to="/transactions" className="hover:text-gray-300">
                Transactions
              </Link>
              <Link to="/health" className="hover:text-gray-300">
                Health
              </Link>
            </>
          )}

          {/* Admin Links - visible only if admin is logged in */}
          {user && hasRole('admin') && (
            <>
              <Link to="/admin/users" className="hover:text-gray-300 font-semibold">
                👤 Users
              </Link>
              <Link to="/admin/settings" className="hover:text-gray-300 font-semibold">
                ⚙️ Settings
              </Link>
              <Link to="/admin/reports" className="hover:text-gray-300 font-semibold">
                📊 Reports
              </Link>
            </>
          )}

          {/* Auth Section */}
          {user ? (
            <div className="flex gap-3 items-center border-l pl-4 ml-4">
              <span className="text-sm">
                {user.username}{' '}
                <span className="text-xs bg-blue-600 px-2 py-1 rounded">{user.role}</span>
              </span>
              <button onClick={logout} className="bg-red-600 px-3 py-1 rounded hover:bg-red-700">
                Logout
              </button>
            </div>
          ) : (
            <>
              <Link to="/signin" className="bg-blue-600 px-3 py-1 rounded hover:bg-blue-700">
                Sign In
              </Link>
              <Link to="/signup" className="bg-green-600 px-3 py-1 rounded hover:bg-green-700">
                Sign Up
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
};

Navigation.propTypes = {
  theme: PropTypes.string.isRequired,
  onToggleTheme: PropTypes.func.isRequired,
};
