package com.homeops.backend.vehicle;

import com.homeops.backend.household.Household;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "household_id",
            nullable = false,
            columnDefinition = "uuid",
            foreignKey = @ForeignKey(name = "fk_vehicles_household")
    )
    private Household household;

    @Column(name = "make", nullable = false, length = 200)
    private String make;

    @Column(name = "model", nullable = false, length = 200)
    private String model;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "vin", length = 17)
    private String vin;

    @Column(name = "notes", length = 2_000)
    private String notes;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_cost", precision = 12, scale = 2)
    private BigDecimal purchaseCost;

    @Column(name = "current_mileage")
    private Integer currentMileage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Vehicle() {
    }

    public Vehicle(
            UUID id,
            Household household,
            String make,
            String model,
            Integer year,
            String vin,
            String notes,
            LocalDate purchaseDate,
            BigDecimal purchaseCost,
            Integer currentMileage,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.household = household;
        this.make = make;
        this.model = model;
        this.year = year;
        this.vin = vin;
        this.notes = notes;
        this.purchaseDate = purchaseDate;
        this.purchaseCost = purchaseCost;
        this.currentMileage = currentMileage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public Household getHousehold() {
        return household;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public Integer getYear() {
        return year;
    }

    public String getVin() {
        return vin;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public BigDecimal getPurchaseCost() {
        return purchaseCost;
    }

    public Integer getCurrentMileage() {
        return currentMileage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}