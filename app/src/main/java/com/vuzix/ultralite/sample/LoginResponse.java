package com.vuzix.ultralite.sample;

import java.util.List;

public class LoginResponse {
    private final String token;
    private final String id;
    private final String name;
    private final String surname;
    private final String email;
    private final List<String> roles;

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
