import { render, screen } from '@testing-library/react';
import App from './App';

test('renders application navigation with brand name', () => {
  render(<App />);
  const matches = screen.getAllByText(/suchika/i);
  expect(matches.length).toBeGreaterThan(0);
});
