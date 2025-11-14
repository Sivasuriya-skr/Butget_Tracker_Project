import React, { useState, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import Loader from '../components/Loader';
import { useAuth } from '../context/AuthContext';
import api from '../service/api';
import { toast } from 'react-toastify';
import { formatAmount } from '../utils/currencySymbols';
import './Expenses.css';

const Expenses = () => {
  const { user } = useAuth();
  const [expenses, setExpenses] = useState([]); // Initialize as empty array
  const [loading, setLoading] = useState(true);
  const [formData, setFormData] = useState({
    amount: '',
    category: 'Food',
    description: '',
    date: new Date().toISOString().split('T')[0],
    note: ''
  });
  const [editingId, setEditingId] = useState(null);
  const [filterCategory, setFilterCategory] = useState('');
  const [filterStartDate, setFilterStartDate] = useState('');
  const [filterEndDate, setFilterEndDate] = useState('');

  const categories = ['Food', 'Transport', 'Shopping', 'Bills', 'Entertainment', 'Health', 'Rent', 'Other'];

  useEffect(() => {
    fetchExpenses();
  }, []);

  const fetchExpenses = async () => {
    try {
      const params = {};
      if (filterStartDate) params.startDate = filterStartDate;
      if (filterEndDate) params.endDate = filterEndDate;
      if (filterCategory) params.category = filterCategory;

      const response = await api.get('/expenses', { params });
      
      // Ensure response.data is an array
      if (Array.isArray(response.data)) {
        setExpenses(response.data);
      } else {
        setExpenses([]);
        console.error('API did not return an array:', response.data);
      }
      
      setLoading(false);
    } catch (error) {
      console.error('Fetch expenses error:', error);
      toast.error('Failed to fetch expenses');
      setExpenses([]); // Set empty array on error
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

    if (!formData.amount || !formData.description) {
      toast.error('Please fill in all required fields');
      return;
    }

    try {
      if (editingId) {
        await api.put(`/expenses/${editingId}`, formData);
        toast.success('Expense updated successfully');
      } else {
        await api.post('/expenses', formData);
        toast.success('Expense added successfully');
      }

      setFormData({
        amount: '',
        category: 'Food',
        description: '',
        date: new Date().toISOString().split('T')[0],
        note: ''
      });
      setEditingId(null);
      fetchExpenses();
    } catch (error) {
      console.error('Save expense error:', error);
      toast.error(error.response?.data?.error || 'Failed to save expense');
    }
  };

  const handleEdit = (expense) => {
    setFormData({
      amount: expense.amount,
      category: expense.category,
      description: expense.description,
      date: expense.date,
      note: expense.note || ''
    });
    setEditingId(expense.id);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this expense?')) {
      try {
        await api.delete(`/expenses/${id}`);
        toast.success('Expense deleted successfully');
        fetchExpenses();
      } catch (error) {
        console.error('Delete expense error:', error);
        toast.error('Failed to delete expense');
      }
    }
  };

  const handleFilterChange = () => {
    setLoading(true);
    fetchExpenses();
  };

  const clearFilters = () => {
    setFilterCategory('');
    setFilterStartDate('');
    setFilterEndDate('');
    setLoading(true);
    fetchExpenses();
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
        <div className="expenses-container">
          <h2>Expense Management</h2>

          <div className="expenses-form-card">
            <h4>{editingId ? 'Edit Expense' : 'Add New Expense'}</h4>
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
                  <label htmlFor="description" className="form-label">Description *</label>
                  <input
                    type="text"
                    className="form-control"
                    id="description"
                    name="description"
                    value={formData.description}
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
                    {editingId ? 'Update Expense' : 'Add Expense'}
                  </button>
                  {editingId && (
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => {
                        setEditingId(null);
                        setFormData({
                          amount: '',
                          category: 'Food',
                          description: '',
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

          <div className="expenses-table-card">
            <div className="table-header">
              <h4>Expense History</h4>
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

            {expenses && expenses.length > 0 ? (
              <div className="table-responsive">
                <table className="table table-hover">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Category</th>
                      <th>Description</th>
                      <th>Amount</th>
                      <th>Note</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {expenses.map((expense) => (
                      <tr key={expense.id}>
                        <td>{new Date(expense.date).toLocaleDateString()}</td>
                        <td><span className="badge bg-danger">{expense.category}</span></td>
                        <td>{expense.description}</td>
                        <td className="text-danger fw-bold">
                          {formatAmount(expense.amount, user?.currency || 'USD')}
                        </td>
                        <td>{expense.note || '-'}</td>
                        <td>
                          <button
                            className="btn btn-sm btn-outline-primary me-2"
                            onClick={() => handleEdit(expense)}
                          >
                            Edit
                          </button>
                          <button
                            className="btn btn-sm btn-outline-danger"
                            onClick={() => handleDelete(expense.id)}
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
              <p className="text-center text-muted py-4">No expense records found</p>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default Expenses;