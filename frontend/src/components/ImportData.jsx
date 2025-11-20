import React, { useState } from 'react';
import api from '../service/api';
import { toast } from 'react-toastify';
import './ImportData.css';

const ImportData = ({ onImportSuccess }) => {
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [importResult, setImportResult] = useState(null);

  const handleFileSelect = (e) => {
    const selectedFile = e.target.files[0];
    
    if (selectedFile) {
      const fileName = selectedFile.name.toLowerCase();
      
      if (!fileName.endsWith('.csv') && !fileName.endsWith('.xlsx') && !fileName.endsWith('.xls')) {
        toast.error('Only CSV and Excel files are supported');
        return;
      }

      if (selectedFile.size > 10 * 1024 * 1024) {
        toast.error('File size must be less than 10MB');
        return;
      }

      setFile(selectedFile);
    }
  };

  const handleImport = async () => {
    if (!file) {
      toast.error('Please select a file');
      return;
    }

    setLoading(true);
    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await api.post('/import/transactions', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });

      setImportResult(response.data);
      
      if (response.data.failedCount === 0) {
        toast.success(`Successfully imported ${response.data.successCount} transactions`);
      } else {
        toast.warning(`Imported ${response.data.successCount} transactions with ${response.data.failedCount} errors`);
      }

      setFile(null);
      setShowModal(false);
      
      if (onImportSuccess) {
        onImportSuccess();
      }
    } catch (error) {
      console.error('Import error:', error);
      toast.error(error.response?.data?.error || 'Failed to import file');
    } finally {
      setLoading(false);
    }
  };

  const downloadTemplate = () => {
    const csvContent = `Date,Type,Category,Description,Amount,Note
2024-01-01,Income,Salary,Monthly Salary,5000,
2024-01-02,Expense,Food,Grocery Shopping,150,Weekly groceries
2024-01-03,Income,Freelance,Web Development,1200,Client project
2024-01-04,Expense,Transport,Fuel,50,Car fuel`;

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'budget_tracker_template.csv';
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  };

  return (
    <>
      <button className="btn btn-success" onClick={() => setShowModal(true)}>
        📥 Import Data
      </button>

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content import-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h5>Import Transactions</h5>
              <button className="close-button" onClick={() => setShowModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <div className="import-instructions">
                <h6>Instructions:</h6>
                <ol>
                  <li>Download the template CSV file to see the required format</li>
                  <li>Fill in your transaction data</li>
                  <li>Upload the CSV or Excel file</li>
                  <li>Review the import results</li>
                </ol>

                <div className="format-info">
                  <strong>Required Columns:</strong>
                  <ul>
                    <li><strong>Date:</strong> Format: YYYY-MM-DD, DD/MM/YYYY, or MM/DD/YYYY</li>
                    <li><strong>Type:</strong> Income or Expense</li>
                    <li><strong>Category:</strong> Valid category name</li>
                    <li><strong>Description:</strong> Transaction description</li>
                    <li><strong>Amount:</strong> Numeric value (without currency symbol)</li>
                    <li><strong>Note:</strong> Optional notes</li>
                  </ul>
                </div>

                <button className="btn btn-outline-primary mb-3" onClick={downloadTemplate}>
                  📄 Download Template
                </button>
              </div>

              <div className="file-upload-section">
                <input
                  type="file"
                  id="importFile"
                  accept=".csv,.xlsx,.xls"
                  onChange={handleFileSelect}
                  style={{ display: 'none' }}
                />
                <label htmlFor="importFile" className="btn btn-outline-secondary">
                  Choose File
                </label>
                
                {file && (
                  <div className="selected-file">
                    <p>Selected: <strong>{file.name}</strong></p>
                    <p className="file-size">Size: {(file.size / 1024).toFixed(2)} KB</p>
                  </div>
                )}
              </div>

              {importResult && (
                <div className="import-result">
                  <h6>Import Results:</h6>
                  <p className="text-success">✓ Successfully imported: {importResult.successCount}</p>
                  {importResult.failedCount > 0 && (
                    <>
                      <p className="text-danger">✗ Failed: {importResult.failedCount}</p>
                      {importResult.errors && importResult.errors.length > 0 && (
                        <div className="error-list">
                          <strong>Errors:</strong>
                          <ul>
                            {importResult.errors.slice(0, 10).map((error, index) => (
                              <li key={index}>{error}</li>
                            ))}
                            {importResult.errors.length > 10 && (
                              <li>... and {importResult.errors.length - 10} more errors</li>
                            )}
                          </ul>
                        </div>
                      )}
                    </>
                  )}
                </div>
              )}
            </div>

            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setShowModal(false);
                  setFile(null);
                  setImportResult(null);
                }}
              >
                Cancel
              </button>
              <button
                className="btn btn-primary"
                onClick={handleImport}
                disabled={!file || loading}
              >
                {loading ? 'Importing...' : 'Import'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default ImportData;