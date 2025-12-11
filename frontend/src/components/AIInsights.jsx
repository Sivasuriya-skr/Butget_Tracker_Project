import React, { useState, useEffect } from 'react';
import api from '../service/api';
import { toast } from 'react-toastify';
import './AIInsights.css';

const AIInsights = ({ month, year }) => {
  const [insights, setInsights] = useState(null);
  const [loading, setLoading] = useState(true);
  const [chatOpen, setChatOpen] = useState(false);
  const [chatMessages, setChatMessages] = useState([]);
  const [chatInput, setChatInput] = useState('');
  const [sendingMessage, setSendingMessage] = useState(false);

  useEffect(() => {
    fetchInsights();
  }, [month, year]);

  const fetchInsights = async () => {
    setLoading(true);
    try {
      const response = await api.get('/ai/insights', {
        params: { month, year }
      });
      setInsights(response.data);
      setLoading(false);
    } catch (error) {
      console.error('Fetch AI insights error:', error);
      toast.error('Failed to fetch AI insights');
      setLoading(false);
    }
  };

  const handleSendMessage = async () => {
    if (!chatInput.trim()) return;

    const userMessage = { role: 'user', content: chatInput };
    setChatMessages([...chatMessages, userMessage]);
    setChatInput('');
    setSendingMessage(true);

    try {
      const response = await api.post('/ai/chat', {
        query: chatInput,
        month,
        year
      });

      const aiMessage = { role: 'assistant', content: response.data.response };
      setChatMessages(prev => [...prev, aiMessage]);
    } catch (error) {
      console.error('Chat error:', error);
      toast.error('Failed to get AI response');
    } finally {
      setSendingMessage(false);
    }
  };

  const getHealthScoreColor = (score) => {
    if (score >= 80) return '#28a745';
    if (score >= 60) return '#ffc107';
    return '#dc3545';
  };

  if (loading) {
    return (
      <div className="ai-insights-loading">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading AI Insights...</span>
        </div>
        <p>Generating AI insights...</p>
      </div>
    );
  }

  return (
    <div className="ai-insights-container">
      {/* Financial Health Score */}
      <div className="health-score-card">
        <h5>Financial Health Score</h5>
        <div className="score-circle" style={{ borderColor: getHealthScoreColor(insights?.financialHealthScore) }}>
          <span className="score-value">{insights?.financialHealthScore?.toFixed(0)}</span>
          <span className="score-label">/100</span>
        </div>
        <div className="score-bar">
          <div 
            className="score-fill" 
            style={{ 
              width: `${insights?.financialHealthScore}%`,
              backgroundColor: getHealthScoreColor(insights?.financialHealthScore)
            }}
          ></div>
        </div>
      </div>

      {/* AI Insight */}
      <div className="insight-card">
        <div className="card-header">
          <span className="icon">🤖</span>
          <h5>AI Analysis</h5>
        </div>
        <p className="insight-text">{insights?.insight}</p>
      </div>

      {/* Recommendations */}
      {insights?.recommendations && insights.recommendations.length > 0 && (
        <div className="recommendations-card">
          <div className="card-header">
            <span className="icon">💡</span>
            <h5>Recommendations</h5>
          </div>
          <ul className="recommendations-list">
            {insights.recommendations.map((rec, index) => (
              <li key={index}>{rec}</li>
            ))}
          </ul>
        </div>
      )}

      {/* Spending Pattern */}
      {insights?.spendingPattern && (
        <div className="pattern-card">
          <div className="card-header">
            <span className="icon">📊</span>
            <h5>Spending Pattern</h5>
          </div>
          <p>{insights.spendingPattern}</p>
        </div>
      )}

      {/* Saving Tip */}
      {insights?.savingTip && (
        <div className="tip-card">
          <div className="card-header">
            <span className="icon">💰</span>
            <h5>Saving Tip</h5>
          </div>
          <p>{insights.savingTip}</p>
        </div>
      )}

      {/* Risk Assessment */}
      {insights?.riskAssessment && (
        <div className="risk-card">
          <div className="card-header">
            <span className="icon">⚠️</span>
            <h5>Risk Assessment</h5>
          </div>
          <p>{insights.riskAssessment}</p>
        </div>
      )}

      {/* AI Chat Button */}
      <button 
        className="ai-chat-button"
        onClick={() => setChatOpen(!chatOpen)}
      >
        <span className="chat-icon">💬</span>
        Ask AI Assistant
      </button>

      {/* AI Chat Modal */}
      {chatOpen && (
        <div className="chat-modal-overlay" onClick={() => setChatOpen(false)}>
          <div className="chat-modal" onClick={(e) => e.stopPropagation()}>
            <div className="chat-header">
              <h5>🤖 AI Financial Assistant</h5>
              <button className="close-button" onClick={() => setChatOpen(false)}>×</button>
            </div>

            <div className="chat-messages">
              {chatMessages.length === 0 && (
                <div className="chat-welcome">
                  <p>👋 Hi! I'm your AI financial assistant.</p>
                  <p>Ask me anything about your finances, budgeting tips, or financial advice!</p>
                </div>
              )}
              
              {chatMessages.map((msg, index) => (
                <div key={index} className={`chat-message ${msg.role}`}>
                  <div className="message-content">
                    {msg.content}
                  </div>
                </div>
              ))}

              {sendingMessage && (
                <div className="chat-message assistant">
                  <div className="message-content typing">
                    <span></span><span></span><span></span>
                  </div>
                </div>
              )}
            </div>

            <div className="chat-input-area">
              <input
                type="text"
                className="chat-input"
                placeholder="Ask me anything..."
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
                disabled={sendingMessage}
              />
              <button 
                className="send-button"
                onClick={handleSendMessage}
                disabled={sendingMessage || !chatInput.trim()}
              >
                ➤
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AIInsights;