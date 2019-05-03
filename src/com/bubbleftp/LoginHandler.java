package com.bubbleftp;

public class LoginHandler {
    private static final String SECRET_PASS = "mypass";
    private static final String SECRET_USER = "bolhoso";

    private String user;
    public boolean isLoggedIn;

    public LoginHandler() {
        this.isLoggedIn = false;
    }

    public void setUser(String user) {
        this.user = user;
        this.isLoggedIn = false;
    }

    public String getUser() {
        return this.user;
    }

    public boolean authUser (String pass) {
        if (pass != null && user.equals(SECRET_USER) && pass.equals(SECRET_PASS)) {
            this.isLoggedIn = true;
        }

        return isLoggedIn;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }
}
