package com.coursework.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RepairTask {
    private Integer id;
    private Integer defectId;
    private String title;
    private String description;
    private String assignedTo;
    private LocalDate plannedDate;
    private LocalDate completedDate;
    private String status;
    private BigDecimal costEstimate;
    private LocalDateTime createdAt;

    // display
    private String defectTitle;

    public RepairTask() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getDefectId() { return defectId; }
    public void setDefectId(Integer defectId) { this.defectId = defectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public LocalDate getPlannedDate() { return plannedDate; }
    public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }

    public LocalDate getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDate completedDate) { this.completedDate = completedDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getCostEstimate() { return costEstimate; }
    public void setCostEstimate(BigDecimal costEstimate) { this.costEstimate = costEstimate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getDefectTitle() { return defectTitle; }
    public void setDefectTitle(String defectTitle) { this.defectTitle = defectTitle; }
}
