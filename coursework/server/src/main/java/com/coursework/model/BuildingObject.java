package com.coursework.model;

import java.time.LocalDateTime;

public class BuildingObject {
    private Integer id;
    private String name;
    private String address;
    private String objectType;
    private Integer yearBuilt;
    private Integer floors;
    private String status;
    private LocalDateTime createdAt;

    public BuildingObject() {}

    public BuildingObject(Integer id, String name, String address, String objectType,
                          Integer yearBuilt, Integer floors, String status) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.objectType = objectType;
        this.yearBuilt = yearBuilt;
        this.floors = floors;
        this.status = status;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }

    public Integer getYearBuilt() { return yearBuilt; }
    public void setYearBuilt(Integer yearBuilt) { this.yearBuilt = yearBuilt; }

    public Integer getFloors() { return floors; }
    public void setFloors(Integer floors) { this.floors = floors; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name + " (" + address + ")";
    }
}
