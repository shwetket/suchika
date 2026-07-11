import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { InstitutionSelect } from './InstitutionSelect';

function Wrapper({ initialValue }) {
  const [value, setValue] = React.useState(initialValue);
  return (
    <InstitutionSelect
      name="institution_name"
      value={value}
      onChange={(e) => setValue(e.target.value)}
    />
  );
}

describe('InstitutionSelect', () => {
  it('renders the curated dropdown with a blank default option', () => {
    render(<Wrapper initialValue="" />);
    const select = screen.getByRole('combobox');
    expect(select.value).toBe('');
    expect(screen.getByRole('option', { name: 'HDFC Bank' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'SBI Mutual Fund' })).toBeInTheDocument();
  });

  it('selecting a known institution updates the value and does not show a text input', () => {
    render(<Wrapper initialValue="" />);
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Axis Bank' } });
    expect(screen.getByRole('combobox').value).toBe('Axis Bank');
    expect(screen.queryByPlaceholderText(/employer name/i)).not.toBeInTheDocument();
  });

  it('selecting "Other" reveals a free-text input', () => {
    render(<Wrapper initialValue="" />);
    fireEvent.change(screen.getByRole('combobox'), { target: { value: '__OTHER__' } });
    const textInput = screen.getByPlaceholderText(/employer name/i);
    expect(textInput).toBeInTheDocument();
    fireEvent.change(textInput, { target: { value: 'Goldman Sachs' } });
    expect(textInput.value).toBe('Goldman Sachs');
  });

  it('switching from a known institution to "Other" keeps the text input visible (regression: value going known -> "" must not hide it)', () => {
    render(<Wrapper initialValue="" />);
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'ICICI Bank' } });
    expect(screen.getByRole('combobox').value).toBe('ICICI Bank');

    fireEvent.change(screen.getByRole('combobox'), { target: { value: '__OTHER__' } });
    const textInput = screen.getByPlaceholderText(/employer name/i);
    expect(textInput).toBeInTheDocument();
    fireEvent.change(textInput, { target: { value: 'My Local Credit Union' } });
    expect(textInput.value).toBe('My Local Credit Union');
  });

  it('clearing the "Other" text box does not hide the input itself', () => {
    render(<Wrapper initialValue="" />);
    fireEvent.change(screen.getByRole('combobox'), { target: { value: '__OTHER__' } });
    const textInput = screen.getByPlaceholderText(/employer name/i);
    fireEvent.change(textInput, { target: { value: 'Some Name' } });
    fireEvent.change(textInput, { target: { value: '' } });
    expect(screen.getByPlaceholderText(/employer name/i)).toBeInTheDocument();
  });

  it('pre-fills the text input and selects "Other" when the initial value is not on the curated list', () => {
    render(<Wrapper initialValue="Goldman Sachs" />);
    const textInput = screen.getByPlaceholderText(/employer name/i);
    expect(textInput.value).toBe('Goldman Sachs');
    expect(screen.getByRole('combobox').value).toBe('__OTHER__');
  });

  it('a known initial value selects it directly with no text input shown', () => {
    render(<Wrapper initialValue="HDFC Bank" />);
    expect(screen.getByRole('combobox').value).toBe('HDFC Bank');
    expect(screen.queryByPlaceholderText(/employer name/i)).not.toBeInTheDocument();
  });
});
