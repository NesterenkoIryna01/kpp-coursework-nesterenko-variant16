package com.coursework.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Defect {
    private Integer id;
    private Integer buildingObjectId;
    private String title;
    private String description;
    private BigDecimal severity;
    private BigDecimal areaScale;
    private BigDecimal safetyImpact;
    private BigDecimal integralScore;
    private DefectState state;
    private LocalDate discoveredAt;
    private String buildingObjectName;

    public Defect() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getBuildingObjectId() { return buildingObjectId; }
    public void setBuildingObjectId(Integer buildingObjectId) { this.buildingObjectId = buildingObjectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getSeverity() { return severity; }
    public void setSeverity(BigDecimal severity) { this.severity = severity; }
    public BigDecimal getAreaScale() { return areaScale; }
    public void setAreaScale(BigDecimal areaScale) { this.areaScale = areaScale; }
    public BigDecimal getSafetyImpact() { return safetyImpact; }
    public void setSafetyImpact(BigDecimal safetyImpact) { this.safetyImpact = safetyImpact; }
    public BigDecimal getIntegralScore() { return integralScore; }
    public void setIntegralScore(BigDecimal integralScore) { this.integralScore = integralScore; }
    public DefectState getState() { return state; }
    public void setState(DefectState state) { this.state = state; }
    public LocalDate getDiscoveredAt() { return discoveredAt; }
    public void setDiscoveredAt(LocalDate discoveredAt) { this.discoveredAt = discoveredAt; }
    public String getBuildingObjectName() { return buildingObjectName; }
    public void setBuildingObjectName(String buildingObjectName) { this.buildingObjectName = buildingObjectName; }
}
