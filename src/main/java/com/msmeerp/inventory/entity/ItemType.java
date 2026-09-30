package com.msmeerp.inventory.entity;

/** W7: whether an item is physically stocked, tracked without stock control, or a pure service line. */
public enum ItemType {
    STOCK,
    NON_STOCK,
    SERVICE
}
