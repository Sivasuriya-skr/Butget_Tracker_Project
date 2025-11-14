import React, { useState, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import Loader from '../components/Loader';
import { useAuth } from '../context/AuthContext';
import api from '../service/api';
import { toast } from 'react-toastify';
import './Profile.css';

const Profile = () => {
  const { user, updateUser } = useAuth();
  const [loading, setLoading] = useState(false);
  const [profileData, setProfileData] = useState({
    name: '',
    email: ''
  });
  const [passwordData, setPasswordData] = useState({
    oldPassword: '',
    newPassword: ''
  });
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);

  useEffect(() => {
    if (user) {
      setProfileData({
        name: user.name,
        email: user.email
      });
    }
  }, [user]);

  const handleProfileChange = (e) => {
    setProfileData({
      ...profileData,
      [e.target.name]: e.target.value
    });
  };

  const handlePasswordChange = (e) => {
    setPasswordData({
      ...passwordData,
      [e.target.name]: e.target.value
    });
  };

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    
    if (file) {
      // Validate file type
      if (!file.type.match('image/jpeg') && !file.type.match('image/png')) {
        toast.error('Only JPG and PNG files are allowed');
        return;
      }

      // Validate file size (max 2MB)
      if (file.size > 2 * 1024 * 1024) {
        toast.error('File size must be less than 2MB');
        return;
      }

      setSelectedFile(file);
      
      // Create preview URL
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewUrl(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleProfileUpdate = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await api.put('/user/profile', profileData);
      updateUser(response.data);
      toast.success('Profile updated successfully');
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to update profile');
    } finally {
      setLoading(false);
    }
  };

  const handlePhotoUpload = async () => {
    if (!selectedFile) {
      toast.error('Please select a file');
      return;
    }

    setLoading(true);
    const formData = new FormData();
    formData.append('file', selectedFile);

    try {
      const response = await api.post('/user/profile/photo', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      updateUser(response.data);
      toast.success('Profile photo updated successfully');
      setSelectedFile(null);
      setPreviewUrl(null);
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to upload photo');
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordUpdate = async (e) => {
    e.preventDefault();

    if (!passwordData.oldPassword || !passwordData.newPassword) {
      toast.error('Please fill in all fields');
      return;
    }

    if (passwordData.newPassword.length < 6) {
      toast.error('New password must be at least 6 characters');
      return;
    }

    setLoading(true);

    try {
      await api.put('/user/password', passwordData);
      toast.success('Password changed successfully');
      setPasswordData({
        oldPassword: '',
        newPassword: ''
      });
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to change password');
    } finally {
      setLoading(false);
    }
  };

  const getProfilePhotoUrl = () => {
    if (previewUrl) {
      return previewUrl;
    }
    if (user?.profilePhoto) {
      return `http://localhost:8080/uploads/${user.profilePhoto}`;
    }
    return 'https://via.placeholder.com/150';
  };

  return (
    <>
      <Sidebar />
      <div className="main-content">
        <div className="profile-container">
          <h2>Profile</h2>

          <div className="profile-photo-card">
            <h4>Profile Photo</h4>
            <div className="photo-section">
              <div className="photo-preview">
                <img src={getProfilePhotoUrl()} alt="Profile" />
              </div>
              <div className="photo-controls">
                <input
                  type="file"
                  id="photoUpload"
                  accept="image/jpeg,image/png"
                  onChange={handleFileSelect}
                  style={{ display: 'none' }}
                />
                <label htmlFor="photoUpload" className="btn btn-outline-primary">
                  Choose Photo
                </label>
                {selectedFile && (
                  <>
                    <p className="selected-file">Selected: {selectedFile.name}</p>
                    <button
                      className="btn btn-primary"
                      onClick={handlePhotoUpload}
                      disabled={loading}
                    >
                      {loading ? 'Uploading...' : 'Upload Photo'}
                    </button>
                  </>
                )}
                <p className="text-muted small mt-2">
                  Maximum file size: 2MB. Allowed formats: JPG, PNG
                </p>
              </div>
            </div>
          </div>

          <div className="profile-info-card">
            <h4>Personal Information</h4>
            <form onSubmit={handleProfileUpdate}>
              <div className="mb-3">
                <label htmlFor="name" className="form-label">Full Name</label>
                <input
                  type="text"
                  className="form-control"
                  id="name"
                  name="name"
                  value={profileData.name}
                  onChange={handleProfileChange}
                  required
                />
              </div>

              <div className="mb-3">
                <label htmlFor="email" className="form-label">Email</label>
                <input
                  type="email"
                  className="form-control"
                  id="email"
                  name="email"
                  value={profileData.email}
                  onChange={handleProfileChange}
                  required
                />
              </div>

              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Updating...' : 'Update Profile'}
              </button>
            </form>
          </div>

          <div className="password-card">
            <h4>Change Password</h4>
            <form onSubmit={handlePasswordUpdate}>
              <div className="mb-3">
                <label htmlFor="oldPassword" className="form-label">Old Password</label>
                <input
                  type="password"
                  className="form-control"
                  id="oldPassword"
                  name="oldPassword"
                  value={passwordData.oldPassword}
                  onChange={handlePasswordChange}
                  required
                />
              </div>

              <div className="mb-3">
                <label htmlFor="newPassword" className="form-label">New Password</label>
                <input
                  type="password"
                  className="form-control"
                  id="newPassword"
                  name="newPassword"
                  value={passwordData.newPassword}
                  onChange={handlePasswordChange}
                  required
                  minLength="6"
                />
                <div className="form-text">Password must be at least 6 characters</div>
              </div>

              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Changing...' : 'Change Password'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </>
  );
};

export default Profile;