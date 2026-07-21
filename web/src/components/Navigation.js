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
        <span
          className="material-symbols-rounded text-[16px] leading-none transition-transform"
          style={{ transform: open ? 'rotate(180deg)' : 'none' }}
          aria-hidden="true"
        >
          expand_more
        </span>
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

function DropdownLink({ to, children, onClick, icon }) {
  return (
    <Link
      to={to}
      onClick={onClick}
      className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-blue-50 hover:text-blue-700 transition-colors"
    >
      {icon && (
        <span className="material-symbols-rounded text-[18px]" aria-hidden="true">
          {icon}
        </span>
      )}
      {children}
    </Link>
  );
}

DropdownLink.propTypes = {
  to: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
  onClick: PropTypes.func,
  icon: PropTypes.string,
};

DropdownLink.defaultProps = {
  onClick: undefined,
  icon: undefined,
};

export const Navigation = ({ theme, onToggleTheme }) => {
  const { user, logout, isAuthenticated, hasRole } = useAuth();

  return (
    <nav className="bg-gray-800 text-white px-6 py-3 sticky top-0 z-40 border-b border-white/10 shadow-md">
      <div className="flex justify-between items-center max-w-7xl mx-auto">
        <Link
          to="/"
          className="flex items-center gap-2 text-lg font-bold tracking-tight text-white btn-ripple p-1 rounded-md"
        >
          <span className="material-symbols-rounded" aria-hidden="true">
            check_circle
          </span>
          Suchika
        </Link>

        <div className="flex gap-5 items-center">
          <button
            type="button"
            onClick={onToggleTheme}
            className="flex items-center justify-center w-8 h-8 rounded-full text-gray-300 hover:text-white hover:bg-white/10 transition-colors"
            title={theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
          >
            <span className="material-symbols-rounded" aria-hidden="true">
              {theme === 'dark' ? 'light_mode' : 'dark_mode'}
            </span>
          </button>

          <Link
            to="/"
            className="flex items-center gap-1 text-gray-300 hover:text-white text-sm font-medium transition-colors"
          >
            <span className="material-symbols-rounded text-[18px]" aria-hidden="true">
              home
            </span>
            Home
          </Link>
          <Link
            to="/help"
            className="flex items-center gap-1 text-gray-300 hover:text-white text-sm font-medium transition-colors"
          >
            <span className="material-symbols-rounded text-[18px]" aria-hidden="true">
              help
            </span>
            Help
          </Link>

          {isAuthenticated && (
            <>
              <Link
                to="/dashboard"
                className="flex items-center gap-1 text-gray-300 hover:text-white text-sm font-medium transition-colors"
              >
                <span className="material-symbols-rounded text-[18px]" aria-hidden="true">
                  dashboard
                </span>
                Dashboard
              </Link>

              <Link
                to="/action-center"
                className="flex items-center gap-1 text-gray-300 hover:text-white text-sm font-medium transition-colors"
              >
                <span className="material-symbols-rounded text-[18px]" aria-hidden="true">
                  notifications_active
                </span>
                Action Center
              </Link>

              <Link
                to="/household/profiles"
                className="flex items-center gap-1 text-gray-300 hover:text-white text-sm font-medium transition-colors"
              >
                <span className="material-symbols-rounded text-[18px]" aria-hidden="true">
                  group
                </span>
                Profiles
              </Link>

              <NavDropdown label="Household">
                <DropdownLink to="/household/calendar" icon="event">
                  Calendar
                </DropdownLink>
                <DropdownLink to="/household/inventory" icon="inventory_2">
                  Inventory
                </DropdownLink>
                <DropdownLink to="/household/goals" icon="flag">
                  Goals
                </DropdownLink>
                <DropdownLink to="/household/vacation-planner" icon="flight">
                  Vacation Planner
                </DropdownLink>
              </NavDropdown>

              <NavDropdown label="Wealth">
                <DropdownLink to="/wealth/accounts" icon="account_balance">
                  Accounts
                </DropdownLink>
                <DropdownLink to="/wealth/transactions" icon="receipt_long">
                  Transactions
                </DropdownLink>
                <DropdownLink to="/wealth/physical-assets" icon="chair">
                  Physical Assets
                </DropdownLink>
                <DropdownLink to="/wealth/goal-plans" icon="savings">
                  Goal Plans
                </DropdownLink>
                <DropdownLink to="/wealth/insurance-policies" icon="health_and_safety">
                  Insurance Policies
                </DropdownLink>
              </NavDropdown>

              <NavDropdown label="Health">
                <DropdownLink to="/health/vitals" icon="monitor_heart">
                  Vitals
                </DropdownLink>
                <DropdownLink to="/health/doctors" icon="medical_services">
                  Doctor Visits
                </DropdownLink>
              </NavDropdown>

              {hasRole('admin') && (
                <NavDropdown label="Admin">
                  <DropdownLink to="/admin/setup" icon="settings_suggest">
                    Household Setup
                  </DropdownLink>
                  <DropdownLink to="/admin/policy" icon="policy">
                    Policy Settings
                  </DropdownLink>
                  <DropdownLink to="/admin/console" icon="terminal">
                    Application Console
                  </DropdownLink>
                </NavDropdown>
              )}
            </>
          )}

          {user ? (
            <div className="flex gap-3 items-center border-l border-gray-600 pl-4 ml-2">
              <span className="text-sm text-gray-300">
                {user.username}
                <span className="ml-1 text-xs bg-blue-700 px-2 py-0.5 rounded text-blue-100">
                  {user.role}
                </span>
              </span>
              <button
                type="button"
                onClick={logout}
                title="Logout"
                className="flex items-center justify-center w-8 h-8 rounded-full text-red-500 hover:text-white hover:bg-red-600 transition-colors btn-ripple"
              >
                <span className="material-symbols-rounded" aria-hidden="true">
                  power_settings_new
                </span>
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
