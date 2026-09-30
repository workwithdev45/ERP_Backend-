package com.msmeerp.trade.entity;

/** A party can be a customer, a vendor, or both (e.g. a job worker you buy from and sell to). */
public enum PartyType {
    CUSTOMER,
    VENDOR,
    BOTH;

    public boolean isCustomer() {
        return this == CUSTOMER || this == BOTH;
    }

    public boolean isVendor() {
        return this == VENDOR || this == BOTH;
    }
}
