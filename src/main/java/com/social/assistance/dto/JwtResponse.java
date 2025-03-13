package com.social.assistance.dto;

import lombok.Data;

@Data
public class JwtResponse {
    private String token;
    private String username;
    private String role;
}
