import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { Help } from './Help';

// Mock react-markdown so we don't have to deal with its internals in a simple test
jest.mock('react-markdown', () => (props) => <div>{props.children}</div>);

describe('Help Component', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  test('renders default document (README) when no route param is provided', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      text: () => Promise.resolve('# Mock README Content'),
    });

    render(
      <MemoryRouter initialEntries={['/help']}>
        <Routes>
          <Route path="/help" element={<Help />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText(/Help & Documentation/i)).toBeInTheDocument();
    
    // Wait for fetch to complete and content to render
    await waitFor(() => {
      expect(screen.getByText('# Mock README Content')).toBeInTheDocument();
    });

    expect(global.fetch).toHaveBeenCalledWith('/api/v1/system/documents/README');
  });

  test('renders specific document when route param is provided', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      text: () => Promise.resolve('# Mock Architecture Content'),
    });

    render(
      <MemoryRouter initialEntries={['/help/ARCHITECTURE_GUIDELINES']}>
        <Routes>
          <Route path="/help/:docName" element={<Help />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('# Mock Architecture Content')).toBeInTheDocument();
    });

    expect(global.fetch).toHaveBeenCalledWith('/api/v1/system/documents/ARCHITECTURE_GUIDELINES');
  });

  test('renders error state on fetch failure', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: false,
    });

    render(
      <MemoryRouter initialEntries={['/help/NON_EXISTENT']}>
        <Routes>
          <Route path="/help/:docName" element={<Help />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Document not found')).toBeInTheDocument();
    });
  });
});
