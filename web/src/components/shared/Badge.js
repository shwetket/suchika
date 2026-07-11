import React from 'react';
import PropTypes from 'prop-types';

export const Badge = ({ children, variant }) => {
  const baseClasses = 'px-2.5 py-0.5 rounded-full text-xs font-semibold tracking-wide border';

  const variants = {
    success: 'bg-green-50 text-green-700 border-green-200/50 shadow-sm',
    danger: 'bg-red-50 text-red-700 border-red-200/50 shadow-sm',
    warning: 'bg-yellow-50 text-yellow-700 border-yellow-200/50 shadow-sm',
    neutral: 'bg-gray-50 text-gray-600 border-gray-200/50 shadow-sm',
    info: 'bg-blue-50 text-blue-700 border-blue-200/50 shadow-sm',
  };

  const variantClass = variants[variant] || variants.neutral;

  return <span className={`${baseClasses} ${variantClass}`}>{children}</span>;
};

Badge.propTypes = {
  children: PropTypes.node.isRequired,
  variant: PropTypes.oneOf(['success', 'danger', 'warning', 'neutral', 'info']),
};

Badge.defaultProps = {
  variant: 'neutral',
};
