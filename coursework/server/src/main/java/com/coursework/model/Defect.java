package com.coursework.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Defect {
    private Integer id;
    private Integer buildingObjectId;
    private String title;
    private String description;
    private BigDecimal severity;      // F1 0-100
    private BigDecimal areaScale;     // F2 0-100
    private BigDecimal safetyImpact;  // F3 0-100
    private BigDecimal integralScore; // calculated
    private DefectState state;
    private LocalDate discoveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // optional display fields
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getBuildingObjectName() { return buildingObjectName; }
    public void setBuildingObjectName(String buildingObjectName) { this.buildingObjectName = buildingObjectName; }

    /**
     * Розрахунок інтегрального показника I = 0.40*F1 + 0.35*F2 + 0.25*F3
     */
    public BigDecimal calculateIntegralScore() {
        BigDecimal f1 = severity != null ? severity : BigDecimal.ZERO;
        BigDecimal f2 = areaScale != null ? areaScale : BigDecimal.ZERO;
        BigDecimal f3 = safetyImpact != null ? safetyImpact : BigDecimal.ZERO;
        BigDecimal result = f1.multiply(new BigDecimal("0.40"))
                .add(f2.multiply(new BigDecimal("0.35")))
                .add(f3.multiply(new BigDecimal("0.25")));
        return result.setScale(2, RoundingMode.HALF_UP);
    }
}
