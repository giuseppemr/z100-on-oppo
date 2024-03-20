package com.vuzix.ultralite.sample;

import java.util.List;

public class LoginResponse {
    private String token;
    private String id;
    private String name;
    private String surname;
    private String email;
    private List<String> roles;

    public LoginResponse(String token, String id, String name, String surname, String email, List<String> roles) {
        this.token = token;
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }
}
