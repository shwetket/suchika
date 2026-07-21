import { render, screen, fireEvent, act } from '@testing-library/react';
import App from './App';

afterEach(() => {
  globalThis.localStorage.clear();
  delete globalThis.matchMedia;
});

test('renders application navigation with brand name', async () => {
  await act(async () => {
    render(<App />);
  });
  const matches = screen.getAllByText(/suchika/i);
  expect(matches.length).toBeGreaterThan(0);
});

test('applies saved dark theme from localStorage on mount', async () => {
  globalThis.localStorage.setItem('theme', 'dark');
  await act(async () => {
    render(<App />);
  });
  expect(document.documentElement.classList.contains('dark')).toBe(true);
});

test('falls back to system preference when saved theme is invalid', async () => {
  globalThis.localStorage.setItem('theme', 'not-a-real-theme');
  globalThis.matchMedia = jest.fn().mockReturnValue({ matches: true });
  await act(async () => {
    render(<App />);
  });
  expect(document.documentElement.classList.contains('dark')).toBe(true);
});

test('defaults to light when matchMedia is unavailable and no saved theme', async () => {
  delete globalThis.matchMedia;
  await act(async () => {
    render(<App />);
  });
  expect(document.documentElement.classList.contains('light')).toBe(true);
});

test('toggling the theme button switches the active theme and persists it', async () => {
  await act(async () => {
    render(<App />);
  });
  const toggleBtn = screen.getByRole('button', { name: /dark|light/i });
  await act(async () => {
    fireEvent.click(toggleBtn);
  });
  expect(globalThis.localStorage.getItem('theme')).toMatch(/dark|light/);
});
