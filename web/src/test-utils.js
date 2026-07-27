import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { render } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';

const AllProviders = ({ children }) => {
  const [queryClient] = useState(
    () => new QueryClient({ defaultOptions: { queries: { retry: false } } })
  );
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>{children}</BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
};

const customRender = (ui, options = {}) => render(ui, { wrapper: AllProviders, ...options });

AllProviders.propTypes = {
  children: PropTypes.node.isRequired,
};

export * from '@testing-library/react';
export { customRender as render };
