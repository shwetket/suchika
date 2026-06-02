import { render, screen } from '@testing-library/react';
import App from './App';

test('renders application navigation and routes', () => {
  render(<App />);
  const homeLink = screen.getByText(/home/i);
  expect(homeLink).toBeInTheDocument();
});
