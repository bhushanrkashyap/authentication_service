package com.example.authentication_module.DTO;

import jakarta.validation.constraints.Pattern;

public class UpdateProfileRequestDTO {
    private String username;
    private String city;
    private String country;
    private String state;
    private String address;



    public UpdateProfileRequestDTO(String username, String city, String country, String state, String address, String phoneNumber) {
        this.username = username;
        this.city = city;
        this.country = country;
        this.state = state;
        this.address = address;
        this.phoneNumber = phoneNumber;
    }
    public UpdateProfileRequestDTO() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Pattern(regexp = "^[0-9]{10}$")
    private String phoneNumber;

}
