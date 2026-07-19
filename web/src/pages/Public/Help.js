import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';

export function Help() {
  const { docName } = useParams();
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const currentDoc = docName || 'README';
  const availableDocs = [
    'README',
    'ARCHITECTURE_GUIDELINES',
    'BUSINESS_REQUIREMENTS',
    'FRONTEND_GUIDELINES',
    'LOGGING_AND_EXCEPTIONS',
    'SCRIPTS',
  ];

  useEffect(() => {
    setLoading(true);
    fetch(`/api/v1/system/documents/${currentDoc}`)
      .then((res) => {
        if (!res.ok) throw new Error('Document not found');
        return res.text();
      })
      .then((text) => {
        setContent(text);
        setError(null);
      })
      .catch((err) => {
        setError(err.message);
        setContent('');
      })
      .finally(() => setLoading(false));
  }, [currentDoc]);
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Help & Documentation</h1>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mt-6">
        <div className="md:col-span-1 space-y-2">
          <div className="bg-white dark:bg-gray-800 shadow rounded-lg p-4">
            <h3 className="text-lg font-medium text-slate-900 dark:text-white mb-4">Documents</h3>
            <nav className="space-y-1">
              {availableDocs.map((doc) => (
                <Link
                  key={doc}
                  to={`/help/${doc}`}
                  className={`block px-3 py-2 rounded-md text-sm font-medium ${
                    currentDoc === doc
                      ? 'bg-indigo-50 text-indigo-700 dark:bg-indigo-900/50 dark:text-indigo-300'
                      : 'text-slate-700 hover:bg-slate-50 dark:text-slate-300 dark:hover:bg-slate-800'
                  }`}
                >
                  {doc.replace(/_/g, ' ')}
                </Link>
              ))}
            </nav>
          </div>
        </div>

        <div className="md:col-span-3">
          <div className="bg-white dark:bg-gray-800 shadow rounded-lg p-8 min-h-[600px]">
            {loading ? (
              <div className="flex justify-center items-center h-full">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
              </div>
            ) : error ? (
              <div className="text-center text-red-600 dark:text-red-400 mt-8">
                <p>{error}</p>
              </div>
            ) : (
              <div className="prose dark:prose-invert max-w-none prose-indigo overflow-hidden break-words">
                <ReactMarkdown>{content}</ReactMarkdown>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
