import React from 'react';

export const About = () => {
  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold text-gray-900 mb-4">About Suchika</h1>
      <p className="text-lg text-gray-600 mb-6">
        Suchika is a comprehensive personal operations system designed to help you manage all
        aspects of your life in one place.
      </p>
      <div className="space-y-4">
        <section>
          <h2 className="text-2xl font-bold mb-2">Features</h2>
          <ul className="list-disc list-inside space-y-2 text-gray-700">
            <li>Wealth & Asset Management</li>
            <li>Household Operations</li>
            <li>Health & Biometrics Tracking</li>
            <li>Role-Based Access Control</li>
          </ul>
        </section>
      </div>
    </div>
  );
};
