package com.msmeerp.trade.entity;

/** Every purchase and sales document the document engine knows, with its number prefix. */
public enum DocumentType {
    PURCHASE_ORDER("PO", true),
    GOODS_RECEIPT("GRN", true),
    PURCHASE_BILL("BILL", true),
    DEBIT_NOTE("DN", true),
    QUOTATION("QT", false),
    SALES_ORDER("SO", false),
    DELIVERY_CHALLAN("DC", false),
    SALES_INVOICE("INV", false),
    CREDIT_NOTE("CN", false);

    private final String prefix;
    private final boolean purchase;

    DocumentType(String prefix, boolean purchase) {
        this.prefix = prefix;
        this.purchase = purchase;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isPurchase() {
        return purchase;
    }

    /** Documents that create a payable/receivable and are rounded to the rupee. */
    public boolean isBilling() {
        return this == PURCHASE_BILL || this == SALES_INVOICE || this == DEBIT_NOTE || this == CREDIT_NOTE;
    }
}
