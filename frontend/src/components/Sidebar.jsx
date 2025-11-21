import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Sidebar.css';

const Sidebar = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [isOpen, setIsOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const toggleSidebar = () => {
    setIsOpen(!isOpen);
  };

  const menuItems = [
    { path: '/dashboard', label: 'Dashboard', icon: '📊' },
    { path: '/income', label: 'Income', icon: '💰' },
    { path: '/expenses', label: 'Expenses', icon: '💸' },
    { path: '/analytics', label: 'Analytics', icon: '📈' },  // Add this line
    { path: '/profile', label: 'Profile', icon: '👤' },
    { path: '/settings', label: 'Settings', icon: '⚙️' }
  ];

  return (
    <>
      <button className="sidebar-toggle btn btn-primary d-md-none" onClick={toggleSidebar}>
        ☰
      </button>
      
      <div className={`sidebar ${isOpen ? 'open' : ''}`}>
        <div className="sidebar-header">
          <h3>Budget Tracker</h3>
          <button className="close-btn d-md-none" onClick={toggleSidebar}>×</button>
        </div>
        
        <ul className="sidebar-menu">
          {menuItems.map((item) => (
            <li key={item.path}>
              <Link
                to={item.path}
                className={location.pathname === item.path ? 'active' : ''}
                onClick={() => setIsOpen(false)}
              >
                <span className="icon">{item.icon}</span>
                <span className="label">{item.label}</span>
              </Link>
            </li>
          ))}
          <li>
            <button className="logout-btn" onClick={handleLogout}>
              <span className="icon">🚪</span>
              <span className="label">Logout</span>
            </button>
          </li>
        </ul>
      </div>
      
      {isOpen && <div className="sidebar-overlay d-md-none" onClick={toggleSidebar}></div>}
    </>
  );
};

export default Sidebar;