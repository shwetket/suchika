import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { getAdmin } from '../api/profiles';

export const SetupGate = ({ children }) => {
  const { user } = useAuth();
  const [checking, setChecking] = useState(true);
  const [setupComplete, setSetupComplete] = useState(true);

  useEffect(() => {
    let cancelled = false;

    if (user?.role !== 'admin') {
      setSetupComplete(true);
      setChecking(false);
      return undefined;
    }

    if (!user.admin_id) {
      setSetupComplete(false);
      setChecking(false);
      return undefined;
    }

    setChecking(true);
    getAdmin(user.admin_id)
      .then((admin) => {
        if (!cancelled) {
          setSetupComplete(admin?.policy_settings?.setup_completed === 'true');
          setChecking(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setSetupComplete(false);
          setChecking(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [user]);

  if (checking) {
    return <div className="flex justify-center items-center h-screen">Loading...</div>;
  }

  if (user?.role === 'admin' && !setupComplete) {
    return <Navigate to="/admin/setup" replace />;
  }

  return children;
};

SetupGate.propTypes = {
  children: PropTypes.node.isRequired,
};
