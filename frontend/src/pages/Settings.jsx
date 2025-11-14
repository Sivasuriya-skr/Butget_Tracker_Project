import React, { useState } from 'react';
import Sidebar from '../components/Sidebar';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import api from '../service/api';
import { toast } from 'react-toastify';
import './Settings.css';

const Settings = () => {
  const { user, updateUser, logout } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [selectedCurrency, setSelectedCurrency] = useState(user?.currency || 'USD');
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const currencies = [
    { code: 'USD', name: 'US Dollar ($)', symbol: '$' },
    { code: 'EUR', name: 'Euro (€)', symbol: '€' },
    { code: 'INR', name: 'Indian Rupee (₹)', symbol: '₹' },
    { code: 'GBP', name: 'British Pound (£)', symbol: '£' },
    { code: 'JPY', name: 'Japanese Yen (¥)', symbol: '¥' },
    { code: 'CAD', name: 'Canadian Dollar (C$)', symbol: 'C$' }
  ];

  const handleCurrencyChange = async (e) => {
    const newCurrency = e.target.value;
    setSelectedCurrency(newCurrency);

    try {
      const response = await api.put('/user/currency', { currency: newCurrency });
      updateUser(response.data);
      toast.success('Currency updated successfully');
    } catch (error) {
      toast.error('Failed to update currency');
    }
  };

  const handleExportData = async () => {
    setLoading(true);
    try {
      const response = await api.get('/user/export', {
        responseType: 'blob'
      });

      // Create a blob from the response
      const blob = new Blob([response.data], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `transactions_${new Date().toISOString().split('T')[0]}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      toast.success('Data exported successfully');
    } catch (error) {
      toast.error('Failed to export data');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteAccount = async () => {
    setLoading(true);
    try {
      await api.delete('/user/account');
      toast.success('Account deleted successfully');
      await logout();
      navigate('/login');
    } catch (error) {
      toast.error('Failed to delete account');
    } finally {
      setLoading(false);
      setShowDeleteModal(false);
    }
  };

  return (
    <>
      <Sidebar />
      <div className="main-content">
        <div className="settings-container">
          <h2>Settings</h2>

          <div className="settings-card">
            <h4>Currency Settings</h4>
            <p className="text-muted">
              Select your preferred currency. This will affect how all amounts are displayed throughout the application.
            </p>
            <div className="currency-selection">
              <label htmlFor="currency" className="form-label">Default Currency</label>
              <select
                id="currency"
                className="form-select"
                value={selectedCurrency}
                onChange={handleCurrencyChange}
              >
                {currencies.map((curr) => (
                  <option key={curr.code} value={curr.code}>
                    {curr.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="settings-card">
            <h4>Export Data</h4>
            <p className="text-muted">
              Download all your transactions (income and expenses) as a CSV file for backup or analysis.
            </p>
            <button
              className="btn btn-primary"
              onClick={handleExportData}
              disabled={loading}
            >
              {loading ? 'Exporting...' : 'Export as CSV'}
            </button>
          </div>

          <div className="settings-card danger-zone">
            <h4>Danger Zone</h4>
            <p className="text-muted">
              Once you delete your account, there is no going back. This will permanently delete all your data.
            </p>
            <button
              className="btn btn-danger"
              onClick={() => setShowDeleteModal(true)}
            >
              Delete Account
            </button>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="modal-overlay" onClick={() => setShowDeleteModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h5>Delete Account</h5>
              <button
                className="close-button"
                onClick={() => setShowDeleteModal(false)}
              >
                ×
              </button>
            </div>
            <div className="modal-body">
              <p>Are you sure you want to delete your account?</p>
              <p className="text-danger fw-bold">
                This action cannot be undone. All your data will be permanently deleted.
              </p>
            </div>
            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => setShowDeleteModal(false)}
              >
                Cancel
              </button>
              <button
                className="btn btn-danger"
                onClick={handleDeleteAccount}
                disabled={loading}
              >
                {loading ? 'Deleting...' : 'Yes, Delete My Account'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Settings;