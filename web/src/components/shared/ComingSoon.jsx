import React from 'react';
import PropTypes from 'prop-types';

/**
 * Placeholder for pages that are not yet implemented.
 * Replace with the real page component when the feature is built.
 */
export const ComingSoon = ({ title, description }) => (
  <div className="p-6 max-w-2xl mx-auto">
    <h1 className="text-3xl font-bold mb-2">{title}</h1>
    {description && <p className="text-gray-500 mb-8">{description}</p>}
    <div className="rounded-xl border-2 border-dashed border-gray-200 p-16 text-center">
      <p className="text-4xl mb-4">🚧</p>
      <p className="text-lg font-medium text-gray-700">Coming soon</p>
      <p className="text-sm text-gray-400 mt-1">This feature is under development.</p>
    </div>
  </div>
);

ComingSoon.propTypes = {
  title: PropTypes.string.isRequired,
  description: PropTypes.string,
};

ComingSoon.defaultProps = {
  description: undefined,
};

export default ComingSoon;
