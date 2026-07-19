// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom';

// Mock react-markdown globally because Jest doesn't support ES modules well out of the box
jest.mock('react-markdown', () => (props) => <div>{props.children}</div>);
