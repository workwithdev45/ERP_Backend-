package com.msmeerp.compliance.gsp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Local stand-in for a GSP sandbox. It applies the IRP's structural checks, derives the IRN exactly as
 * the IRP does — SHA-256 of supplier GSTIN + financial year + document type + document number — and
 * returns a signed QR JWT with the IRP's QR fields. Nothing is sent anywhere; swap in a real
 * {@link GspClient} to go live. Real IRPs also verify GSTIN checksums and registration status.
 */
@Component
@RequiredArgsConstructor
public class SandboxGspClient implements GspClient {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final Pattern GSTIN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");
    private static final Pattern DOC_NO = Pattern.compile("^[1-9A-Z][0-9A-Z/-]{0,15}$");
    private static final Pattern VEHICLE_NO = Pattern.compile("^[A-Z]{2}[0-9]{1,2}[A-Z]{0,3}[0-9]{4}$");
    private static final DateTimeFormatter DOC_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final SecretKey QR_KEY = Keys.hmacShaKeyFor("msme-erp-sandbox-irp-qr-signing-key-not-for-production".getBytes(StandardCharsets.UTF_8));
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "SANDBOX";
    }

    @Override
    @SuppressWarnings("unchecked")
    public IrnResult generateIrn(Map<String, Object> einvoice) {
        Map<String, Object> doc = (Map<String, Object>) einvoice.get("DocDtls");
        Map<String, Object> seller = (Map<String, Object>) einvoice.get("SellerDtls");
        Map<String, Object> buyer = (Map<String, Object>) einvoice.get("BuyerDtls");
        Map<String, Object> values = (Map<String, Object>) einvoice.get("ValDtls");
        List<Map<String, Object>> items = (List<Map<String, Object>>) einvoice.get("ItemList");

        List<String> errors = new ArrayList<>();
        String sellerGstin = (String) seller.get("Gstin");
        String buyerGstin = (String) buyer.get("Gstin");
        String docNo = (String) doc.get("No");
        if (sellerGstin == null || !GSTIN.matcher(sellerGstin).matches()) errors.add("2117: Supplier GSTIN is invalid");
        if (buyerGstin == null || !GSTIN.matcher(buyerGstin).matches()) errors.add("2211: Recipient GSTIN is invalid");
        if (sellerGstin != null && sellerGstin.equals(buyerGstin)) errors.add("2212: Supplier and recipient GSTIN are the same");
        if (docNo == null || !DOC_NO.matcher(docNo).matches()) errors.add("2179: Document number must be 1–16 characters: capitals, digits, / or -, not starting with 0");
        LocalDate docDate = LocalDate.parse((String) doc.get("Dt"), DOC_DATE);
        if (docDate.isAfter(LocalDate.now(IST))) errors.add("2163: Document date can't be in the future");
        if (items == null || items.isEmpty()) errors.add("2182: Add at least one item");
        BigDecimal assessable = BigDecimal.ZERO;
        for (Map<String, Object> item : items == null ? List.<Map<String, Object>>of() : items) {
            String hsn = (String) item.get("HsnCd");
            if (hsn == null || !hsn.matches("^[0-9]{4,8}$")) errors.add("2265: Item " + item.get("SlNo") + " needs a 4–8 digit HSN/SAC code");
            else if ("Y".equals(item.get("IsServc")) && !hsn.startsWith("99")) errors.add("2229: Item " + item.get("SlNo") + " is a service but its SAC doesn't start with 99");
            assessable = assessable.add((BigDecimal) item.get("AssAmt"));
        }
        if (assessable.compareTo((BigDecimal) values.get("AssVal")) != 0) errors.add("2189: Total assessable value doesn't match the items");
        for (String field : List.of("Addr1", "Loc", "Pin")) {
            if (buyer.get(field) == null) errors.add("2240: Recipient " + field + " is required");
            if (seller.get(field) == null) errors.add("2240: Supplier " + field + " is required");
        }
        if (!errors.isEmpty()) {
            throw new GspException(errors);
        }

        String irn = sha256(sellerGstin + financialYear(docDate) + doc.get("Typ") + docNo);
        Instant ackDate = Instant.now();
        String ackNo = "1" + digits(14);

        Map<String, Object> qr = new LinkedHashMap<>();
        qr.put("SellerGstin", sellerGstin);
        qr.put("BuyerGstin", buyerGstin);
        qr.put("DocNo", docNo);
        qr.put("DocTyp", doc.get("Typ"));
        qr.put("DocDt", doc.get("Dt"));
        qr.put("TotInvVal", values.get("TotInvVal"));
        qr.put("ItemCnt", items.size());
        qr.put("MainHsnCode", items.get(0).get("HsnCd"));
        qr.put("Irn", irn);
        qr.put("IrnDt", ackDate.atZone(IST).toLocalDateTime().withNano(0).toString().replace('T', ' '));
        String signedQr = Jwts.builder().issuer("NIC Sandbox").claim("data", toJson(qr)).signWith(QR_KEY).compact();

        return new IrnResult(irn, ackNo, ackDate, signedQr);
    }

    @Override
    public void cancelIrn(String irn, int reasonCode, String remark) {
        if (reasonCode < 1 || reasonCode > 4) {
            throw new GspException("2270: Cancellation reason must be 1 (duplicate), 2 (data entry mistake), 3 (order cancelled) or 4 (other)");
        }
    }

    @Override
    public EwayBillResult generateEwayBill(Map<String, Object> ewayBill) {
        List<String> errors = new ArrayList<>();
        int distance = (Integer) ewayBill.get("Distance");
        String mode = (String) ewayBill.get("TransMode");
        String vehicle = (String) ewayBill.get("VehNo");
        if (distance < 1 || distance > 4000) errors.add("702: Distance must be between 1 and 4000 km");
        if ("1".equals(mode) && vehicle == null && ewayBill.get("TransId") == null) {
            errors.add("4011: Road transport needs a vehicle number or a transporter ID");
        }
        if (vehicle != null && !VEHICLE_NO.matcher(vehicle).matches()) errors.add("4010: Vehicle number format is invalid (e.g. MH12AB1234)");
        if (!"1".equals(mode) && ewayBill.get("TransDocNo") == null) errors.add("4013: Rail, air and ship transport need a transport document number");
        Object transId = ewayBill.get("TransId");
        if (transId != null && !GSTIN.matcher((String) transId).matches()) errors.add("4012: Transporter ID must be a valid GSTIN / TRANSIN");
        if (!errors.isEmpty()) {
            throw new GspException(errors);
        }

        Instant now = Instant.now();
        // Normal cargo: one day per 200 km (or part); each day runs to midnight after the generation date.
        int days = (distance + 199) / 200;
        Instant validUntil = LocalDate.now(IST).plusDays(days).atTime(LocalTime.of(23, 59, 59)).atZone(IST).toInstant();
        return new EwayBillResult("3" + digits(11), now, validUntil);
    }

    @Override
    public void cancelEwayBill(String ewbNo, int reasonCode, String remark) {
        if (reasonCode < 1 || reasonCode > 4) {
            throw new GspException("312: Cancellation reason must be 1 (duplicate), 2 (order cancelled), 3 (data entry mistake) or 4 (other)");
        }
    }

    /** e.g. 2026-27 for any date from 1 April 2026 to 31 March 2027. */
    static String financialYear(LocalDate date) {
        int start = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return start + "-" + String.format("%02d", (start + 1) % 100);
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String digits(int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
