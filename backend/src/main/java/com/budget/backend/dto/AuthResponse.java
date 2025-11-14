package com.budget.backend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String name;
    private String email;
    private String currency;
    private String profilePhoto;
    
    public AuthResponse(String token, Long id, String name, String email, String currency, String profilePhoto) {
        this.token = token;
        this.id = id;
        this.name = name;
        this.email = email;
        this.currency = currency;
        this.profilePhoto = profilePhoto;
    }
}
