import { render, screen, fireEvent } from '@testing-library/react';
import App from './App';

afterEach(() => {
  globalThis.localStorage.clear();
  delete globalThis.matchMedia;
});

test('renders application navigation with brand name', () => {
  render(<App />);
  const matches = screen.getAllByText(/suchika/i);
  expect(matches.length).toBeGreaterThan(0);
});

test('applies saved dark theme from localStorage on mount', () => {
  globalThis.localStorage.setItem('theme', 'dark');
  render(<App />);
  expect(document.documentElement.classList.contains('dark')).toBe(true);
});

test('falls back to system preference when saved theme is invalid', () => {
  globalThis.localStorage.setItem('theme', 'not-a-real-theme');
  globalThis.matchMedia = jest.fn().mockReturnValue({ matches: true });
  render(<App />);
  expect(document.documentElement.classList.contains('dark')).toBe(true);
});

test('defaults to light when matchMedia is unavailable and no saved theme', () => {
  delete globalThis.matchMedia;
  render(<App />);
  expect(document.documentElement.classList.contains('light')).toBe(true);
});

test('toggling the theme button switches the active theme and persists it', () => {
  render(<App />);
  const toggleBtn = screen.getByRole('button', { name: /dark|light/i });
  fireEvent.click(toggleBtn);
  expect(globalThis.localStorage.getItem('theme')).toMatch(/dark|light/);
});
