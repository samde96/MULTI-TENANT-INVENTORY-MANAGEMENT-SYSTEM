package com.keen.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_transfers")
public class StockTransfer extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String transferNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    private StockRequest request;

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
    private AppUser approvedBy;

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
    private AppUser dispatchedBy;

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
    private AppUser receivedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TransferStatus status = TransferStatus.DRAFT;

    @Column(length = 1000)
    private String note;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<StockTransferItem> items = new ArrayList<>();

    private Instant sentAt;
    private Instant receivedAt;

    public String getTransferNumber() {
        return transferNumber;
    }

    public void setTransferNumber(String transferNumber) {
        this.transferNumber = transferNumber;
    }

    public StockRequest getRequest() {
        return request;
    }

    public void setRequest(StockRequest request) {
        this.request = request;
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
    public AppUser getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(AppUser approvedBy) {
        this.approvedBy = approvedBy;
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
    public AppUser getDispatchedBy() {
        return dispatchedBy;
    }

    public void setDispatchedBy(AppUser dispatchedBy) {
        this.dispatchedBy = dispatchedBy;
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
    public AppUser getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(AppUser receivedBy) {
        this.receivedBy = receivedBy;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<StockTransferItem> getItems() {
        return items;
    }

    public void setItems(List<StockTransferItem> items) {
        this.items = items;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}
