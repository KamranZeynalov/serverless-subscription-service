package com.company.model;

public class User {
    private String email;
    private String token;
    private boolean verified;


    public User(String email, String token, boolean verified) {
        this.email = email;
        this.token = token;
        this.verified = verified;
    }

    public String getEmail() { return email; }
    public String getToken() { return token; }
    public boolean isVerified() { return verified; }

}
