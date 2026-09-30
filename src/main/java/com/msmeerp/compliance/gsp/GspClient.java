package com.msmeerp.compliance.gsp;

import java.time.Instant;
import java.util.Map;

/**
 * W13: the GST Suvidha Provider that talks to the IRP (e-invoice) and the e-way bill system.
 * {@link SandboxGspClient} stands in until a real GSP account is configured; a real client only
 * has to implement this interface.
 */
public interface GspClient {

    /** Provider name stored on every record, e.g. "SANDBOX". */
    String name();

    /** Registers an e-invoice (INV-01 JSON) with the IRP and returns its IRN, acknowledgement and signed QR. */
    IrnResult generateIrn(Map<String, Object> einvoice);

    /** Cancels an IRN. The IRP only allows this within 24 hours of the acknowledgement. */
    void cancelIrn(String irn, int reasonCode, String remark);

    EwayBillResult generateEwayBill(Map<String, Object> ewayBill);

    /** Cancels an e-way bill, likewise only within 24 hours of generation. */
    void cancelEwayBill(String ewbNo, int reasonCode, String remark);

    record IrnResult(String irn, String ackNo, Instant ackDate, String signedQr) {
    }

    record EwayBillResult(String ewbNo, Instant ewbDate, Instant validUntil) {
    }
}
