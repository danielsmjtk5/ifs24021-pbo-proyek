package org.delcom.app.dto;

import org.springframework.web.multipart.MultipartFile;

public class DonationForm {
    private String name;
    private String location;
    
    // --- TAMBAHAN BARU (Wajib ada biar tidak error di Service) ---
    private Double latitude;
    private Double longitude;
    // -------------------------------------------------------------

    private String category;
    private Boolean isHalal;
    private Integer portion;
    private String expiredTime; // String dari HTML input datetime-local
    private String description;
    
    // Fitur: Upload Gambar
    private MultipartFile photo; 

    // --- CONSTRUCTOR KOSONG ---
    public DonationForm() {}

    // --- GETTERS AND SETTERS ---
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    // --- GETTER & SETTER LATITUDE/LONGITUDE ---
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    // ------------------------------------------

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getIsHalal() {
        return isHalal;
    }

    public void setIsHalal(Boolean isHalal) {
        this.isHalal = isHalal;
    }

    public Integer getPortion() {
        return portion;
    }

    public void setPortion(Integer portion) {
        this.portion = portion;
    }

    public String getExpiredTime() {
        return expiredTime;
    }

    public void setExpiredTime(String expiredTime) {
        this.expiredTime = expiredTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MultipartFile getPhoto() {
        return photo;
    }

    public void setPhoto(MultipartFile photo) {
        this.photo = photo;
    }
}