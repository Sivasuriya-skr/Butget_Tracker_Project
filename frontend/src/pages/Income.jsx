import React, { useState, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import Loader from '../components/Loader';
import { useAuth } from '../context/AuthContext';
import api from '../service/api';
import { toast } from 'react-toastify';
import { formatAmount } from '../utils/currencySymbols';
import './Income.css';

const Income = () => {
  const { user } = useAuth();
  const [incomes, setIncomes] = useState([]); // Initialize as empty array
  const [loading, setLoading] = useState(true);
  const [formData, setFormData] = useState({
    amount: '',
    category: 'Salary',
    source: '',
    date: new Date().toISOString().split('T')[0],
    note: ''
  });
  const [editingId, setEditingId] = useState(null);
  const [filterCategory, setFilterCategory] = useState('');
  const [filterStartDate, setFilterStartDate] = useState('');
  const [filterEndDate, setFilterEndDate] = useState('');

  const categories = ['Salary', 'Freelance', 'Business', 'Investment', 'Gift', 'Other'];

  useEffect(() => {
    fetchIncomes();
  }, []);

  const fetchIncomes = async () => {
    try {
      const params = {};
      if (filterStartDate) params.startDate = filterStartDate;
      if (filterEndDate) params.endDate = filterEndDate;
      if (filterCategory) params.category = filterCategory;

      const response = await api.get('/incomes', { params });
      
      // Ensure response.data is an array
      if (Array.isArray(response.data)) {
        setIncomes(response.data);
      } else {
        setIncomes([]);
        console.error('API did not return an array:', response.data);
      }
      
      setLoading(false);
    } catch (error) {
      console.error('Fetch incomes error:', error);
      toast.error('Failed to fetch incomes');
      setIncomes([]); // Set empty array on error
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.amount || !formData.source) {
      toast.error('Please fill in all required fields');
      return;
    }

    try {
      if (editingId) {
        await api.put(`/incomes/${editingId}`, formData);
        toast.success('Income updated successfully');
      } else {
        await api.post('/incomes', formData);
        toast.success('Income added successfully');
      }

      setFormData({
        amount: '',
        category: 'Salary',
        source: '',
        date: new Date().toISOString().split('T')[0],
        note: ''
      });
      setEditingId(null);
      fetchIncomes();
    } catch (error) {
      console.error('Save income error:', error);
      toast.error(error.response?.data?.error || 'Failed to save income');
    }
  };

  const handleEdit = (income) => {
    setFormData({
      amount: income.amount,
      category: income.category,
      source: income.source,
      date: income.date,
      note: income.note || ''
    });
    setEditingId(income.id);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this income?')) {
      try {
        await api.delete(`/incomes/${id}`);
        toast.success('Income deleted successfully');
        fetchIncomes();
      } catch (error) {
        console.error('Delete income error:', error);
        toast.error('Failed to delete income');
      }
    }
  };

  const handleFilterChange = () => {
    setLoading(true);
    fetchIncomes();
  };

  const clearFilters = () => {
    setFilterCategory('');
    setFilterStartDate('');
    setFilterEndDate('');
    setLoading(true);
    fetchIncomes();
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
        <div className="income-container">
          <h2>Income Management</h2>

          <div className="income-form-card">
            <h4>{editingId ? 'Edit Income' : 'Add New Income'}</h4>
            <form onSubmit={handleSubmit}>
              <div className="row">
                <div className="col-md-6 mb-3">
                  <label htmlFor="amount" className="form-label">Amount *</label>
                  <input
                    type="number"
                    className="form-control"
                    id="amount"
                    name="amount"
                    value={formData.amount}
                    onChange={handleChange}
                    step="0.01"
                    min="0"
                    required
                  />
                </div>

                <div className="col-md-6 mb-3">
                  <label htmlFor="category" className="form-label">Category *</label>
                  <select
                    className="form-select"
                    id="category"
                    name="category"
                    value={formData.category}
                    onChange={handleChange}
                    required
                  >
                    {categories.map((cat) => (
                      <option key={cat} value={cat}>{cat}</option>
                    ))}
                  </select>
                </div>

                <div className="col-md-6 mb-3">
                  <label htmlFor="source" className="form-label">Source *</label>
                  <input
                    type="text"
                    className="form-control"
                    id="source"
                    name="source"
                    value={formData.source}
                    onChange={handleChange}
                    required
                  />
                </div>

                <div className="col-md-6 mb-3">
                  <label htmlFor="date" className="form-label">Date *</label>
                  <input
                    type="date"
                    className="form-control"
                    id="date"
                    name="date"
                    value={formData.date}
                    onChange={handleChange}
                    required
                  />
                </div>

                <div className="col-12 mb-3">
                  <label htmlFor="note" className="form-label">Note</label>
                  <textarea
                    className="form-control"
                    id="note"
                    name="note"
                    value={formData.note}
                    onChange={handleChange}
                    rows="3"
                  ></textarea>
                </div>

                <div className="col-12">
                  <button type="submit" className="btn btn-primary me-2">
                    {editingId ? 'Update Income' : 'Add Income'}
                  </button>
                  {editingId && (
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => {
                        setEditingId(null);
                        setFormData({
                          amount: '',
                          category: 'Salary',
                          source: '',
                          date: new Date().toISOString().split('T')[0],
                          note: ''
                        });
                      }}
                    >
                      Cancel
                    </button>
                  )}
                </div>
              </div>
            </form>
          </div>

          <div className="income-table-card">
            <div className="table-header">
              <h4>Income History</h4>
              <div className="filters">
                <div className="filter-group">
                  <label>Category:</label>
                  <select
                    className="form-select"
                    value={filterCategory}
                    onChange={(e) => setFilterCategory(e.target.value)}
                  >
                    <option value="">All</option>
                    {categories.map((cat) => (
                      <option key={cat} value={cat}>{cat}</option>
                    ))}
                  </select>
                </div>

                <div className="filter-group">
                  <label>Start Date:</label>
                  <input
                    type="date"
                    className="form-control"
                    value={filterStartDate}
                    onChange={(e) => setFilterStartDate(e.target.value)}
                  />
                </div>

                <div className="filter-group">
                  <label>End Date:</label>
                  <input
                    type="date"
                    className="form-control"
                    value={filterEndDate}
                    onChange={(e) => setFilterEndDate(e.target.value)}
                  />
                </div>

                <button className="btn btn-primary" onClick={handleFilterChange}>
                  Apply Filters
                </button>
                <button className="btn btn-secondary" onClick={clearFilters}>
                  Clear
                </button>
              </div>
            </div>

            {incomes && incomes.length > 0 ? (
              <div className="table-responsive">
                <table className="table table-hover">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Category</th>
                      <th>Source</th>
                      <th>Amount</th>
                      <th>Note</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {incomes.map((income) => (
                      <tr key={income.id}>
                        <td>{new Date(income.date).toLocaleDateString()}</td>
                        <td><span className="badge bg-success">{income.category}</span></td>
                        <td>{income.source}</td>
                        <td className="text-success fw-bold">
                          {formatAmount(income.amount, user?.currency || 'USD')}
                        </td>
                        <td>{income.note || '-'}</td>
                        <td>
                          <button
                            className="btn btn-sm btn-outline-primary me-2"
                            onClick={() => handleEdit(income)}
                          >
                            Edit
                          </button>
                          <button
                            className="btn btn-sm btn-outline-danger"
                            onClick={() => handleDelete(income.id)}
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-center text-muted py-4">No income records found</p>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default Income;