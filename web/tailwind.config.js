module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}', './public/index.html'],
  theme: {
    extend: {
      fontFamily: {
        sans: [
          'Inter',
          'ui-sans-serif',
          'system-ui',
          '-apple-system',
          'BlinkMacSystemFont',
          'Segoe UI',
          'Roboto',
          'Helvetica Neue',
          'Arial',
          'sans-serif',
        ],
        display: ['Outfit', 'Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      colors: {
        /* Primary Color Scheme */
        'primary-color': 'var(--primary-color)',
        'primary-background-color': 'var(--primary-background-color)',
        'primary-text-color': 'var(--primary-text-color)',
        'primary-border-color': 'var(--primary-border-color)',
        'primary-hover-color': 'var(--primary-hover-color)',
        'primary-focus-color': 'var(--primary-focus-color)',

        /* Secondary Color Scheme */
        'secondary-color': 'var(--secondary-color)',
        'secondary-background-color': 'var(--secondary-background-color)',
        'secondary-text-color': 'var(--secondary-text-color)',
        'secondary-border-color': 'var(--secondary-border-color)',
        'secondary-hover-color': 'var(--secondary-hover-color)',
        'secondary-focus-color': 'var(--secondary-focus-color)',

        /* Tertiary Color Scheme */
        'tertiary-color': 'var(--tertiary-color)',
        'tertiary-background-color': 'var(--tertiary-background-color)',
        'tertiary-text-color': 'var(--tertiary-text-color)',
        'tertiary-border-color': 'var(--tertiary-border-color)',
        'tertiary-hover-color': 'var(--tertiary-hover-color)',
        'tertiary-focus-color': 'var(--tertiary-focus-color)',

        /* Dark Primary Color Scheme */
        'dark-primary-color': 'var(--dark-primary-color)',
        'dark-primary-background-color': 'var(--dark-primary-background-color)',
        'dark-primary-text-color': 'var(--dark-primary-text-color)',
        'dark-primary-border-color': 'var(--dark-primary-border-color)',
        'dark-primary-hover-color': 'var(--dark-primary-hover-color)',
        'dark-primary-focus-color': 'var(--dark-primary-focus-color)',

        /* Dark Secondary Color Scheme */
        'dark-secondary-color': 'var(--dark-secondary-color)',
        'dark-secondary-background-color': 'var(--dark-secondary-background-color)',
        'dark-secondary-text-color': 'var(--dark-secondary-text-color)',
        'dark-secondary-border-color': 'var(--dark-secondary-border-color)',
        'dark-secondary-hover-color': 'var(--dark-secondary-hover-color)',
        'dark-secondary-focus-color': 'var(--dark-secondary-focus-color)',

        /* Dark Tertiary Color Scheme */
        'dark-tertiary-color': 'var(--dark-tertiary-color)',
        'dark-tertiary-background-color': 'var(--dark-tertiary-background-color)',
        'dark-tertiary-text-color': 'var(--dark-tertiary-text-color)',
        'dark-tertiary-border-color': 'var(--dark-tertiary-border-color)',
        'dark-tertiary-hover-color': 'var(--dark-tertiary-hover-color)',
        'dark-tertiary-focus-color': 'var(--dark-tertiary-focus-color)',

        /* Additional Colors */
        'hover-color': 'var(--hover-color)',
        'hover-text-color': 'var(--hover-text-color)',
        'hover-background-color': 'var(--hover-background-color)',
        'hover-border-color': 'var(--hover-border-color)',

        'active-color': 'var(--active-color)',
        'active-text-color': 'var(--active-text-color)',
        'active-background-color': 'var(--active-background-color)',
        'active-border-color': 'var(--active-border-color)',

        'disabled-color': 'var(--disabled-color)',
        'disabled-text-color': 'var(--disabled-text-color)',
        'disabled-background-color': 'var(--disabled-background-color)',
        'disabled-border-color': 'var(--disabled-border-color)',

        'focus-color': 'var(--focus-color)',
        'focus-text-color': 'var(--focus-text-color)',
        'focus-background-color': 'var(--focus-background-color)',
        'focus-border-color': 'var(--focus-border-color)',

        'error-color': 'var(--error-color)',
        'error-text-color': 'var(--error-text-color)',
        'error-background-color': 'var(--error-background-color)',
        'error-border-color': 'var(--error-border-color)',

        'success-color': 'var(--success-color)',
        'success-text-color': 'var(--success-text-color)',
        'success-background-color': 'var(--success-background-color)',
        'success-border-color': 'var(--success-border-color)',

        'warning-color': 'var(--warning-color)',
        'warning-text-color': 'var(--warning-text-color)',
        'warning-background-color': 'var(--warning-background-color)',
        'warning-border-color': 'var(--warning-border-color)',

        'info-color': 'var(--info-color)',
        'info-text-color': 'var(--info-text-color)',
        'info-background-color': 'var(--info-background-color)',
        'info-border-color': 'var(--info-border-color)',

        borderColor: {
          custom1: 'var(--primary-border-color)',
          custom2: 'var(--secondary-border-color)',
          custom3: 'var(--tertiary-border-color)',
        },
      },
    },
  },
};
