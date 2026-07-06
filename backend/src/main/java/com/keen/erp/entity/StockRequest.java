package com.keen.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_requests")
public class StockRequest extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    private Location sourceLocation;

    @ManyToOne(fetch = FetchType.EAGER)
    private Location destinationLocation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnoreProperties({
            "roles",
            "locations",
            "authorities",
            "accountNonExpired",
            "accountNonLocked",
            "credentialsNonExpired",
            "enabled",
            "password",
            "hibernateLazyInitializer",
            "handler"
    })
    private AppUser requestedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnoreProperties({
            "roles",
            "locations",
            "authorities",
            "accountNonExpired",
            "accountNonLocked",
            "credentialsNonExpired",
            "enabled",
            "password",
            "hibernateLazyInitializer",
            "handler"
    })
    private AppUser reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RequestStatus status = RequestStatus.DRAFT;

    @Column(length = 1000)
    private String note;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<StockRequestItem> items = new ArrayList<>();

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public Location getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(Location sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Location destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    @JsonIgnoreProperties({
            "roles",
            "locations",
            "authorities",
            "accountNonExpired",
            "accountNonLocked",
            "credentialsNonExpired",
            "enabled",
            "password",
            "hibernateLazyInitializer",
            "handler"
    })
    public AppUser getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(AppUser requestedBy) {
        this.requestedBy = requestedBy;
    }

    @JsonIgnoreProperties({
            "roles",
            "locations",
            "authorities",
            "accountNonExpired",
            "accountNonLocked",
            "credentialsNonExpired",
            "enabled",
            "password",
            "hibernateLazyInitializer",
            "handler"
    })
    public AppUser getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(AppUser reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<StockRequestItem> getItems() {
        return items;
    }

    public void setItems(List<StockRequestItem> items) {
        this.items = items;
    }
}
