package com.budget.backend.service;


import com.budget.backend.dto.ChangePasswordRequest;
import com.budget.backend.dto.UpdateProfileRequest;
import com.budget.backend.entity.User;
import com.budget.backend.exception.BadRequestException;
import com.budget.backend.exception.ResourceNotFoundException;
import com.budget.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private FileUploadService fileUploadService;
    
    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    public User updateProfile(String email, UpdateProfileRequest request) {
        User user = getCurrentUser(email);
        
        // Check if new email already exists (and it's not the current user's email)
        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }
        
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        
        return userRepository.save(user);
    }
    
    public User uploadProfilePhoto(String email, MultipartFile file) {
        User user = getCurrentUser(email);
        
        // Delete old photo if exists
        if (user.getProfilePhoto() != null) {
            fileUploadService.deleteFile(user.getProfilePhoto());
        }
        
        // Upload new photo
        String filename = fileUploadService.uploadFile(file);
        user.setProfilePhoto(filename);
        
        return userRepository.save(user);
    }
    
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getCurrentUser(email);
        
        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Old password is incorrect");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
    
    public User updateCurrency(String email, String currency) {
        User user = getCurrentUser(email);
        user.setCurrency(currency);
        return userRepository.save(user);
    }
    
    public void deleteAccount(String email) {
        User user = getCurrentUser(email);
        
        // Delete profile photo if exists
        if (user.getProfilePhoto() != null) {
            fileUploadService.deleteFile(user.getProfilePhoto());
        }
        
        userRepository.delete(user);
    }
}
