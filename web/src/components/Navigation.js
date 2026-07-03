import React, { useState, useRef, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

function NavDropdown({ label, children }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    function handleClickOutside(e) {
      if (ref.current && !ref.current.contains(e.target)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className="flex items-center gap-1 text-indigo-100 hover:text-white text-sm font-medium"
        aria-expanded={open}
      >
        {label}
        <svg
          className={`w-3 h-3 transition-transform ${open ? 'rotate-180' : ''}`}
          viewBox="0 0 12 12"
          fill="currentColor"
          aria-hidden="true"
        >
          <path d="M6 8L1 3h10z" />
        </svg>
      </button>
      {open && (
        <div className="absolute top-full left-0 mt-1 bg-white rounded-lg shadow-lg border border-gray-100 py-1 z-50 min-w-max">
          {children}
        </div>
      )}
    </div>
  );
}

NavDropdown.propTypes = {
  label: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
};

function DropdownLink({ to, children, onClick }) {
  return (
    <Link
      to={to}
      onClick={onClick}
      className="block px-4 py-2 text-sm text-gray-700 hover:bg-indigo-50 hover:text-indigo-700"
    >
      {children}
    </Link>
  );
}

DropdownLink.propTypes = {
  to: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
  onClick: PropTypes.func,
};

DropdownLink.defaultProps = {
  onClick: undefined,
};

export const Navigation = ({ theme, onToggleTheme }) => {
  const { user, logout, isAuthenticated, hasRole } = useAuth();

  return (
    <nav className="bg-indigo-900 text-white px-6 py-3">
      <div className="flex justify-between items-center max-w-7xl mx-auto">
        <Link to="/" className="text-lg font-bold tracking-tight text-white">
          Suchika
        </Link>

        <div className="flex gap-5 items-center">
          <button
            type="button"
            onClick={onToggleTheme}
            className="text-indigo-200 hover:text-white text-xs px-2 py-1 rounded border border-indigo-700 hover:border-indigo-500"
          >
            {theme === 'dark' ? 'Light' : 'Dark'}
          </button>

          <Link to="/" className="text-indigo-100 hover:text-white text-sm font-medium">
            Home
          </Link>

          {isAuthenticated && (
            <>
              <Link
                to="/dashboard"
                className="text-indigo-100 hover:text-white text-sm font-medium"
              >
                Dashboard
              </Link>

              <Link
                to="/action-center"
                className="text-indigo-100 hover:text-white text-sm font-medium"
              >
                Action Center
              </Link>

              <Link
                to="/household/profiles"
                className="text-indigo-100 hover:text-white text-sm font-medium"
              >
                Profiles
              </Link>

              <NavDropdown label="Household">
                <DropdownLink to="/household/calendar">Calendar</DropdownLink>
                <DropdownLink to="/household/inventory">Inventory</DropdownLink>
                <DropdownLink to="/household/goals">Goals</DropdownLink>
                <DropdownLink to="/household/vacation-planner">Vacation Planner</DropdownLink>
              </NavDropdown>

              <NavDropdown label="Wealth">
                <DropdownLink to="/wealth/accounts">Accounts</DropdownLink>
                <DropdownLink to="/wealth/transactions">Transactions</DropdownLink>
                <DropdownLink to="/wealth/physical-assets">Physical Assets</DropdownLink>
              </NavDropdown>

              <NavDropdown label="Health">
                <DropdownLink to="/health/vitals">Vitals</DropdownLink>
                <DropdownLink to="/health/doctors">Doctor Visits</DropdownLink>
              </NavDropdown>

              {hasRole('admin') && (
                <NavDropdown label="Admin">
                  <DropdownLink to="/admin/setup">Household Setup</DropdownLink>
                  <DropdownLink to="/admin/policy">Policy Settings</DropdownLink>
                </NavDropdown>
              )}
            </>
          )}

          {user ? (
            <div className="flex gap-3 items-center border-l border-indigo-700 pl-4 ml-2">
              <span className="text-sm text-indigo-200">
                {user.username}
                <span className="ml-1 text-xs bg-indigo-700 px-2 py-0.5 rounded text-indigo-100">
                  {user.role}
                </span>
              </span>
              <button
                type="button"
                onClick={logout}
                className="bg-red-600 text-white px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-red-700"
              >
                Logout
              </button>
            </div>
          ) : (
            <Link
              to="/signin"
              className="bg-indigo-600 text-white px-4 py-1.5 rounded-lg text-sm font-medium hover:bg-indigo-500"
            >
              Sign In
            </Link>
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
