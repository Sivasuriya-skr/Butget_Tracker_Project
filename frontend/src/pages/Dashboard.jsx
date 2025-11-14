import React, { useState, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import Loader from '../components/Loader';
import { useAuth } from '../context/AuthContext';
import api from '../service/api';
import { toast } from 'react-toastify';
import { formatAmount } from '../utils/currencySymbols';
import './Dashboard.css';

const Dashboard = () => {
  const { user, updateUser } = useAuth();
  const [dashboard, setDashboard] = useState({
    totalIncome: 0,
    totalExpense: 0,
    balance: 0,
    recentTransactions: []
  });
  const [loading, setLoading] = useState(true);
  const [selectedCurrency, setSelectedCurrency] = useState(user?.currency || 'INR');

  const currencies = ['USD', 'EUR', 'INR', 'GBP', 'JPY', 'CAD'];

  useEffect(() => {
    fetchDashboardData();
  }, []);

  useEffect(() => {
    if (user?.currency) {
      setSelectedCurrency(user.currency);
    }
  }, [user]);

  const fetchDashboardData = async () => {
    try {
      const response = await api.get('/user/dashboard');
      
      if (response.data) {
        setDashboard({
          totalIncome: response.data.totalIncome || 0,
          totalExpense: response.data.totalExpense || 0,
          balance: response.data.balance || 0,
          recentTransactions: Array.isArray(response.data.recentTransactions) 
            ? response.data.recentTransactions 
            : []
        });
      }
      
      setLoading(false);
    } catch (error) {
      console.error('Fetch dashboard error:', error);
      toast.error('Failed to fetch dashboard data');
      setDashboard({
        totalIncome: 0,
        totalExpense: 0,
        balance: 0,
        recentTransactions: []
      });
      setLoading(false);
    }
  };

  const handleCurrencyChange = async (e) => {
    const newCurrency = e.target.value;
    setSelectedCurrency(newCurrency);

    try {
      const response = await api.put('/user/currency', { currency: newCurrency });
      updateUser(response.data);
      toast.success('Currency updated successfully');
    } catch (error) {
      console.error('Update currency error:', error);
      toast.error('Failed to update currency');
      // Revert to previous currency on error
      setSelectedCurrency(user?.currency || 'INR');
    }
  };

  if (loading) {
    return (
      <>
        <Sidebar />
        <div className="main-content">
          <Loader />
        </div>
      </>
    );
  }

  return (
    <>
      <Sidebar />
      <div className="main-content">
        <div className="dashboard-container">
          <div className="dashboard-header">
            <h2>Dashboard</h2>
            <div className="currency-selector">
              <label htmlFor="currency">Currency:</label>
              <select
                id="currency"
                className="form-select"
                value={selectedCurrency}
                onChange={handleCurrencyChange}
              >
                {currencies.map((curr) => (
                  <option key={curr} value={curr}>{curr}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="row">
            <div className="col-md-4 mb-4">
              <div className="stat-card income-card">
                <div className="stat-icon">💰</div>
                <div className="stat-info">
                  <h6>Total Income</h6>
                  <h3>{formatAmount(dashboard.totalIncome, selectedCurrency)}</h3>
                </div>
              </div>
            </div>

            <div className="col-md-4 mb-4">
              <div className="stat-card expense-card">
                <div className="stat-icon">💸</div>
                <div className="stat-info">
                  <h6>Total Expenses</h6>
                  <h3>{formatAmount(dashboard.totalExpense, selectedCurrency)}</h3>
                </div>
              </div>
            </div>

            <div className="col-md-4 mb-4">
              <div className="stat-card balance-card">
                <div className="stat-icon">💼</div>
                <div className="stat-info">
                  <h6>Current Balance</h6>
                  <h3>{formatAmount(dashboard.balance, selectedCurrency)}</h3>
                </div>
              </div>
            </div>
          </div>

          <div className="recent-transactions">
            <h4>Recent Transactions</h4>
            {dashboard.recentTransactions && dashboard.recentTransactions.length > 0 ? (
              <div className="table-responsive">
                <table className="table table-hover">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Type</th>
                      <th>Category</th>
                      <th>Description</th>
                      <th>Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dashboard.recentTransactions.map((transaction, index) => (
                      <tr key={`${transaction.type}-${transaction.id}-${index}`}>
                        <td>{new Date(transaction.date).toLocaleDateString()}</td>
                        <td>
                          <span className={`badge ${transaction.type === 'income' ? 'bg-success' : 'bg-danger'}`}>
                            {transaction.type}
                          </span>
                        </td>
                        <td>{transaction.category}</td>
                        <td>{transaction.description}</td>
                        <td className={transaction.type === 'income' ? 'text-success fw-bold' : 'text-danger fw-bold'}>
                          {transaction.type === 'income' ? '+' : '-'}
                          {formatAmount(transaction.amount, selectedCurrency)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="text-center py-5">
                <p className="text-muted">No transactions yet</p>
                <p className="text-muted small">Start by adding your first income or expense!</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default Dashboard;