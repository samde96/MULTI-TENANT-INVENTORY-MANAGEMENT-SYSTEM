package com.keen.erp.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_request_items")
public class StockRequestItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonBackReference
    private StockRequest request;

    @ManyToOne(fetch = FetchType.EAGER)
    private Product product;

    private int quantityRequested;

    public StockRequest getRequest() {
        return request;
    }

    public void setRequest(StockRequest request) {
        this.request = request;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantityRequested() {
        return quantityRequested;
    }

    public void setQuantityRequested(int quantityRequested) {
        this.quantityRequested = quantityRequested;
    }
}
