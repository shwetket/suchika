import React from 'react';
import { render } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';

const AllProviders = ({ children }) => (
  <AuthProvider>
    <BrowserRouter>{children}</BrowserRouter>
  </AuthProvider>
);

const customRender = (ui, options = {}) => render(ui, { wrapper: AllProviders, ...options });

export * from '@testing-library/react';
export { customRender as render };
