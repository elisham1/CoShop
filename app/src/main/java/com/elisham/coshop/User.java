package com.elisham.coshop;

public class User {
    private String name;
    private String familyName;
    private String email;
    private String userType; // "Supplier" or "Consumer"
    private String address;

    // Empty constructor (needed for Firebase)
    public User() {
    }

    public User(String name, String familyName, String email, String userType, String address) {
        this.name = name;
        this.familyName = familyName;
        this.email = email;
        this.userType = userType;
        this.address = address;
    }

    // Getters and setters (required for Firebase)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

