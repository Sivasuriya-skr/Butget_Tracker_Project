import React, { useState, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import AIInsights from '../components/AIInsights';
import Loader from '../components/Loader';
import { useAuth } from '../context/AuthContext';
import api from '../service/api';
import { toast } from 'react-toastify';
import { formatAmount } from '../utils/currencySymbols';
import {
  Chart as ChartJS,
  ArcElement,
  CategoryScale,
  LinearScale,
  BarElement,
  LineElement,
  PointElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js';
import { Pie, Line } from 'react-chartjs-2';
import './Analytics.css';

ChartJS.register(
  ArcElement,
  CategoryScale,
  LinearScale,
  BarElement,
  LineElement,
  PointElement,
  Title,
  Tooltip,
  Legend
);

const Analytics = () => {
  const { user } = useAuth();
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());
  const [budgetAmount, setBudgetAmount] = useState('');
  const [showBudgetModal, setShowBudgetModal] = useState(false);
  const [savingBudget, setSavingBudget] = useState(false);

  const months = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  const years = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - 2 + i);

  useEffect(() => {
    fetchAnalytics();
  }, [selectedMonth, selectedYear]);

  const fetchAnalytics = async () => {
    try {
      const response = await api.get('/analytics', {
        params: {
          month: selectedMonth,
          year: selectedYear
        }
      });
      setAnalytics(response.data);
      setLoading(false);
    } catch (error) {
      console.error('Fetch analytics error:', error);
      toast.error('Failed to fetch analytics data');
      setLoading(false);
    }
  };

  const handleSetBudget = async () => {
    if (!budgetAmount || parseFloat(budgetAmount) <= 0) {
      toast.error('Please enter a valid budget amount');
      return;
    }

    setSavingBudget(true);
    try {
      await api.post('/analytics/budget', {
        amount: parseFloat(budgetAmount),
        month: selectedMonth,
        year: selectedYear
      });
      toast.success('Budget set successfully');
      setShowBudgetModal(false);
      setBudgetAmount('');
      fetchAnalytics();
    } catch (error) {
      console.error('Set budget error:', error);
      toast.error('Failed to set budget');
    } finally {
      setSavingBudget(false);
    }
  };

  const openBudgetModal = () => {
    setBudgetAmount(analytics?.budget > 0 ? analytics.budget : '');
    setShowBudgetModal(true);
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

  // Financial Overview Pie Chart
  const financialOverviewData = {
    labels: ['Income', 'Expenses', 'Savings'],
    datasets: [{
      data: [
        analytics?.totalIncome || 0,
        analytics?.totalExpense || 0,
        analytics?.savings || 0
      ],
      backgroundColor: [
        'rgba(75, 192, 192, 0.8)',
        'rgba(255, 99, 132, 0.8)',
        'rgba(54, 162, 235, 0.8)'
      ],
      borderColor: [
        'rgba(75, 192, 192, 1)',
        'rgba(255, 99, 132, 1)',
        'rgba(54, 162, 235, 1)'
      ],
      borderWidth: 2
    }]
  };

  // Budget vs Expense Pie Chart
  const budgetData = {
    labels: ['Used', 'Remaining'],
    datasets: [{
      data: [
        analytics?.totalExpense || 0,
        Math.max(0, (analytics?.budgetRemaining || 0))
      ],
      backgroundColor: [
        analytics?.budgetUsedPercentage > 100 ? 'rgba(255, 99, 132, 0.8)' : 'rgba(255, 159, 64, 0.8)',
        'rgba(75, 192, 192, 0.8)'
      ],
      borderColor: [
        analytics?.budgetUsedPercentage > 100 ? 'rgba(255, 99, 132, 1)' : 'rgba(255, 159, 64, 1)',
        'rgba(75, 192, 192, 1)'
      ],
      borderWidth: 2
    }]
  };

  // Income by Category
  const incomeCategoryData = {
    labels: Object.keys(analytics?.incomeByCategory || {}),
    datasets: [{
      data: Object.values(analytics?.incomeByCategory || {}),
      backgroundColor: [
        'rgba(75, 192, 192, 0.8)',
        'rgba(54, 162, 235, 0.8)',
        'rgba(153, 102, 255, 0.8)',
        'rgba(255, 206, 86, 0.8)',
        'rgba(255, 159, 64, 0.8)',
        'rgba(201, 203, 207, 0.8)'
      ],
      borderWidth: 2
    }]
  };

  // Expense by Category
  const expenseCategoryData = {
    labels: Object.keys(analytics?.expenseByCategory || {}),
    datasets: [{
      data: Object.values(analytics?.expenseByCategory || {}),
      backgroundColor: [
        'rgba(255, 99, 132, 0.8)',
        'rgba(255, 159, 64, 0.8)',
        'rgba(255, 205, 86, 0.8)',
        'rgba(75, 192, 192, 0.8)',
        'rgba(54, 162, 235, 0.8)',
        'rgba(153, 102, 255, 0.8)',
        'rgba(201, 203, 207, 0.8)',
        'rgba(255, 99, 71, 0.8)'
      ],
      borderWidth: 2
    }]
  };

  // Monthly Trend Line Chart
  const monthlyTrendData = {
    labels: analytics?.monthlyTrend?.map(m => m.month) || [],
    datasets: [
      {
        label: 'Income',
        data: analytics?.monthlyTrend?.map(m => m.income) || [],
        borderColor: 'rgba(75, 192, 192, 1)',
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        tension: 0.4
      },
      {
        label: 'Expense',
        data: analytics?.monthlyTrend?.map(m => m.expense) || [],
        borderColor: 'rgba(255, 99, 132, 1)',
        backgroundColor: 'rgba(255, 99, 132, 0.2)',
        tension: 0.4
      },
      {
        label: 'Balance',
        data: analytics?.monthlyTrend?.map(m => m.balance) || [],
        borderColor: 'rgba(54, 162, 235, 1)',
        backgroundColor: 'rgba(54, 162, 235, 0.2)',
        tension: 0.4
      }
    ]
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          padding: 15,
          font: {
            size: 12
          }
        }
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            let label = context.label || '';
            if (label) {
              label += ': ';
            }
            label += formatAmount(context.parsed || context.raw, user?.currency || 'USD');
            return label;
          }
        }
      }
    }
  };

  const lineChartOptions = {
    ...chartOptions,
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          callback: function(value) {
            return formatAmount(value, user?.currency || 'USD');
          }
        }
      }
    }
  };

  return (
    <>
      <Sidebar />
      <div className="main-content">
        <div className="analytics-container">
          <div className="analytics-header">
            <h2>Analytics & Budget</h2>
            <div className="filters">
              <select
                className="form-select"
                value={selectedMonth}
                onChange={(e) => setSelectedMonth(parseInt(e.target.value))}
              >
                {months.map((month, index) => (
                  <option key={index} value={index + 1}>{month}</option>
                ))}
              </select>

              <select
                className="form-select"
                value={selectedYear}
                onChange={(e) => setSelectedYear(parseInt(e.target.value))}
              >
                {years.map((year) => (
                  <option key={year} value={year}>{year}</option>
                ))}
              </select>

              <button className="btn btn-primary" onClick={openBudgetModal}>
                {analytics?.budget > 0 ? 'Update Budget' : 'Set Budget'}
              </button>
            </div>
          </div>

          {/* Summary Cards */}
          <div className="row mb-4">
            <div className="col-lg-3 col-md-6 mb-3">
              <div className="summary-card income-card">
                <div className="card-icon">💰</div>
                <div className="card-content">
                  <h6>Total Income</h6>
                  <h4>{formatAmount(analytics?.totalIncome || 0, user?.currency || 'USD')}</h4>
                </div>
              </div>
            </div>

            <div className="col-lg-3 col-md-6 mb-3">
              <div className="summary-card expense-card">
                <div className="card-icon">💸</div>
                <div className="card-content">
                  <h6>Total Expenses</h6>
                  <h4>{formatAmount(analytics?.totalExpense || 0, user?.currency || 'USD')}</h4>
                </div>
              </div>
            </div>

            <div className="col-lg-3 col-md-6 mb-3">
              <div className="summary-card savings-card">
                <div className="card-icon">🏦</div>
                <div className="card-content">
                  <h6>Savings</h6>
                  <h4>{formatAmount(analytics?.savings || 0, user?.currency || 'USD')}</h4>
                </div>
              </div>
            </div>

            <div className="col-lg-3 col-md-6 mb-3">
              <div className="summary-card budget-card">
                <div className="card-icon">📊</div>
                <div className="card-content">
                  <h6>Budget</h6>
                  <h4>{formatAmount(analytics?.budget || 0, user?.currency || 'USD')}</h4>
                  {analytics?.budget > 0 && (
                    <div className="budget-progress">
                      <div className="progress">
                        <div 
                          className={`progress-bar ${analytics?.budgetUsedPercentage > 100 ? 'bg-danger' : analytics?.budgetUsedPercentage > 80 ? 'bg-warning' : 'bg-success'}`}
                          style={{ width: `${Math.min(analytics?.budgetUsedPercentage, 100)}%` }}
                        ></div>
                      </div>
                      <small>{analytics?.budgetUsedPercentage?.toFixed(1)}% used</small>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* AI INSIGHTS SECTION - ADD THIS */}
          <div className="ai-section">
            <div className="section-header">
              <h3>🤖 AI-Powered Insights</h3>
              <p>Get personalized financial advice powered by artificial intelligence</p>
            </div>
            <AIInsights month={selectedMonth} year={selectedYear} />
          </div>

          {/* Charts Section */}
          <div className="charts-section">
            <h3 className="section-title">Financial Charts</h3>
            <div className="row">
              {/* Financial Overview Pie Chart */}
              <div className="col-lg-6 mb-4">
                <div className="chart-card">
                  <h5>Financial Overview</h5>
                  <div className="chart-container">
                    <Pie data={financialOverviewData} options={chartOptions} />
                  </div>
                </div>
              </div>

              {/* Budget vs Expense */}
              {analytics?.budget > 0 && (
                <div className="col-lg-6 mb-4">
                  <div className="chart-card">
                    <h5>Budget Status</h5>
                    <div className="chart-container">
                      <Pie data={budgetData} options={chartOptions} />
                    </div>
                    <div className="budget-details">
                      <p>Budget: {formatAmount(analytics?.budget, user?.currency || 'USD')}</p>
                      <p>Spent: {formatAmount(analytics?.totalExpense, user?.currency || 'USD')}</p>
                      <p className={analytics?.budgetRemaining < 0 ? 'text-danger' : 'text-success'}>
                        {analytics?.budgetRemaining < 0 ? 'Over Budget: ' : 'Remaining: '}
                        {formatAmount(Math.abs(analytics?.budgetRemaining || 0), user?.currency || 'USD')}
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {/* Income by Category */}
              {Object.keys(analytics?.incomeByCategory || {}).length > 0 && (
                <div className="col-lg-6 mb-4">
                  <div className="chart-card">
                    <h5>Income by Category</h5>
                    <div className="chart-container">
                      <Pie data={incomeCategoryData} options={chartOptions} />
                    </div>
                  </div>
                </div>
              )}

              {/* Expense by Category */}
              {Object.keys(analytics?.expenseByCategory || {}).length > 0 && (
                <div className="col-lg-6 mb-4">
                  <div className="chart-card">
                    <h5>Expenses by Category</h5>
                    <div className="chart-container">
                      <Pie data={expenseCategoryData} options={chartOptions} />
                    </div>
                  </div>
                </div>
              )}

              {/* Monthly Trend */}
              <div className="col-12 mb-4">
                <div className="chart-card">
                  <h5>6-Month Trend</h5>
                  <div className="chart-container-large">
                    <Line data={monthlyTrendData} options={lineChartOptions} />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Budget Modal */}
      {showBudgetModal && (
        <div className="modal-overlay" onClick={() => setShowBudgetModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h5>Set Monthly Budget</h5>
              <button className="close-button" onClick={() => setShowBudgetModal(false)}>
                ×
              </button>
            </div>
            <div className="modal-body">
              <p>Set your budget for {months[selectedMonth - 1]} {selectedYear}</p>
              <div className="mb-3">
                <label htmlFor="budgetAmount" className="form-label">Budget Amount</label>
                <input
                  type="number"
                  className="form-control"
                  id="budgetAmount"
                  value={budgetAmount}
                  onChange={(e) => setBudgetAmount(e.target.value)}
                  placeholder="Enter budget amount"
                  step="0.01"
                  min="0"
                />
              </div>
            </div>
            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => setShowBudgetModal(false)}
              >
                Cancel
              </button>
              <button
                className="btn btn-primary"
                onClick={handleSetBudget}
                disabled={savingBudget}
              >
                {savingBudget ? 'Saving...' : 'Set Budget'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Analytics;