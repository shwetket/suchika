import React from 'react';

export const AdminUsers = () => {
  return (
    <div className="p-8 max-w-6xl mx-auto">
      <h1 className="text-4xl font-bold mb-4">👤 User Management</h1>
      <p className="text-gray-600 mb-6">Manage system users and permissions</p>

      <div className="bg-yellow-50 border border-yellow-200 p-4 rounded-lg mb-6">
        <p className="text-yellow-800">⚠️ Admin Access: You have full control over user accounts</p>
      </div>

      <div className="bg-white border border-gray-200 p-6 rounded-lg">
        <h2 className="text-2xl font-bold mb-4">Users List</h2>
        <table className="w-full">
          <thead className="bg-gray-100">
            <tr>
              <th className="text-left p-3">Username</th>
              <th className="text-left p-3">Role</th>
              <th className="text-left p-3">Login Time</th>
              <th className="text-left p-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr className="border-t hover:bg-gray-50">
              <td className="p-3">Demo User</td>
              <td className="p-3">
                <span className="bg-blue-100 text-blue-800 px-2 py-1 rounded text-sm">admin</span>
              </td>
              <td className="p-3">Just now</td>
              <td className="p-3 flex gap-2">
                <button className="text-blue-600 hover:underline">Edit</button>
                <button className="text-red-600 hover:underline">Remove</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div className="mt-6">
        <button className="bg-green-600 text-white px-6 py-2 rounded-lg hover:bg-green-700">
          ➕ Add User
        </button>
      </div>
    </div>
  );
};
