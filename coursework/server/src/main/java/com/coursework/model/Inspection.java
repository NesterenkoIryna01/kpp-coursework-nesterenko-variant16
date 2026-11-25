package com.coursework.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Inspection {
    private Integer id;
    private Integer buildingObjectId;
    private Integer defectId;
    private LocalDate inspectionDate;
    private String inspectorName;
    private String findings;
    private BigDecimal measuredValue;
    private String unit;
    private String notes;
    private LocalDateTime createdAt;

    // display
    private String buildingObjectName;

    public Inspection() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getBuildingObjectId() { return buildingObjectId; }
    public void setBuildingObjectId(Integer buildingObjectId) { this.buildingObjectId = buildingObjectId; }

    public Integer getDefectId() { return defectId; }
    public void setDefectId(Integer defectId) { this.defectId = defectId; }

    public LocalDate getInspectionDate() { return inspectionDate; }
    public void setInspectionDate(LocalDate inspectionDate) { this.inspectionDate = inspectionDate; }

    public String getInspectorName() { return inspectorName; }
    public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }

    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }

    public BigDecimal getMeasuredValue() { return measuredValue; }
    public void setMeasuredValue(BigDecimal measuredValue) { this.measuredValue = measuredValue; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getBuildingObjectName() { return buildingObjectName; }
    public void setBuildingObjectName(String buildingObjectName) { this.buildingObjectName = buildingObjectName; }
}
