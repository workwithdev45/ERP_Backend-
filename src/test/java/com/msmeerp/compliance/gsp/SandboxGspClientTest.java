package com.msmeerp.compliance.gsp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SandboxGspClientTest {

    private final SandboxGspClient gsp = new SandboxGspClient(new ObjectMapper());

    private static Map<String, Object> einvoice(String sellerGstin, String buyerGstin, String docNo, String hsn, String buyerPin) {
        Map<String, Object> seller = new HashMap<>(Map.of("Gstin", sellerGstin, "Addr1", "Plot 12", "Loc", "Pune", "Pin", "411026"));
        Map<String, Object> buyer = new HashMap<>(Map.of("Gstin", buyerGstin, "Addr1", "Gala 4", "Loc", "Nashik"));
        if (buyerPin != null) buyer.put("Pin", buyerPin);
        Map<String, Object> item = new HashMap<>(Map.of("SlNo", "1", "IsServc", "N", "AssAmt", new BigDecimal("9120.00")));
        if (hsn != null) item.put("HsnCd", hsn);
        return new HashMap<>(Map.of(
                "DocDtls", Map.of("Typ", "INV", "No", docNo, "Dt", LocalDate.of(2026, 9, 30).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                "SellerDtls", seller,
                "BuyerDtls", buyer,
                "ItemList", List.of(item),
                "ValDtls", Map.of("AssVal", new BigDecimal("9120.00"), "TotInvVal", new BigDecimal("10762.00"))));
    }

    @Test
    void generateIrn_derivesTheIrnLikeTheIrp() {
        GspClient.IrnResult result = gsp.generateIrn(einvoice("27AAKCS1234M1Z2", "27AAKFN2211C1Z4", "INV-0003", "8413", "422010"));

        // SHA-256(supplier GSTIN + financial year + document type + document number)
        assertThat(result.irn()).isEqualTo(SandboxGspClient.sha256("27AAKCS1234M1Z22026-27INVINV-0003"));
        assertThat(result.irn()).hasSize(64);
        assertThat(result.ackNo()).hasSize(15);
        assertThat(result.signedQr().split("\\.")).hasSize(3);
    }

    @Test
    void generateIrn_reportsEveryProblemAtOnce() {
        assertThatThrownBy(() -> gsp.generateIrn(einvoice("27AAKCS1234M1Z2", "27AAKCS1234M1Z2", "0INV", null, null)))
                .isInstanceOf(GspException.class)
                .hasMessageContaining("2212")
                .hasMessageContaining("2179")
                .hasMessageContaining("2265")
                .hasMessageContaining("Recipient Pin");
    }

    @Test
    void financialYear_runsAprilToMarch() {
        assertThat(SandboxGspClient.financialYear(LocalDate.of(2026, 4, 1))).isEqualTo("2026-27");
        assertThat(SandboxGspClient.financialYear(LocalDate.of(2027, 3, 31))).isEqualTo("2026-27");
        assertThat(SandboxGspClient.financialYear(LocalDate.of(2099, 12, 1))).isEqualTo("2099-00");
    }

    @Test
    void generateEwayBill_givesOneDayPer200KmAndChecksTheVehicle() {
        Map<String, Object> ewb = new HashMap<>(Map.of("TransMode", "1", "Distance", 210, "VehNo", "MH15GT4412"));
        GspClient.EwayBillResult result = gsp.generateEwayBill(ewb);
        LocalDate validDate = result.validUntil().atZone(SandboxGspClient.IST).toLocalDate();
        assertThat(validDate).isEqualTo(LocalDate.now(SandboxGspClient.IST).plusDays(2));
        assertThat(result.ewbNo()).hasSize(12);

        ewb.put("VehNo", "12345");
        assertThatThrownBy(() -> gsp.generateEwayBill(ewb)).hasMessageContaining("4010");
    }
}
