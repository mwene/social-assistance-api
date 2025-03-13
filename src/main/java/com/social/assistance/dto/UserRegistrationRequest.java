package com.social.assistance.dto;

import lombok.Data;

@Data
public class UserRegistrationRequest {
    private String username;
    private String password;
    private String name;
    private String role;
    private String email;
    private String phone;
}
