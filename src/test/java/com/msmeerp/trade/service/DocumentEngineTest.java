package com.msmeerp.trade.service;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.inventory.entity.ItemType;
import com.msmeerp.trade.dto.DocumentLineRequest;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.TradeDocument;
import com.msmeerp.trade.entity.TradeDocumentLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** GST calculation and source-line picking — pure logic, so the engine's collaborators aren't needed. */
class DocumentEngineTest {

    private final DocumentEngine engine = new DocumentEngine(null, null, null, null, null, null, null);

    private static TradeDocumentLine line(long id, int qty, String rate, String discount, String gst) {
        TradeDocumentLine line = TradeDocumentLine.builder()
                .productId(1L).productName("Item " + id).itemType(ItemType.STOCK)
                .quantity(qty).rate(new BigDecimal(rate))
                .discountPercent(new BigDecimal(discount)).gstRate(new BigDecimal(gst))
                .build();
        line.setId(id);
        return line;
    }

    private static TradeDocument document(DocumentType type, boolean interState, boolean reverseCharge, TradeDocumentLine... lines) {
        TradeDocument document = TradeDocument.builder().docType(type).docNumber("X-1").interState(interState).reverseCharge(reverseCharge).build();
        for (TradeDocumentLine line : lines) {
            document.addLine(line);
        }
        return document;
    }

    @Test
    void recalculate_splitsIntraStateGstAndRoundsBillToRupee() {
        TradeDocument bill = document(DocumentType.PURCHASE_BILL, false, false,
                line(1, 100, "5.20", "0", "18"), line(2, 1, "1500", "0", "18"));

        engine.recalculate(bill);

        assertThat(bill.getTaxableAmount()).isEqualByComparingTo("2020.00");
        assertThat(bill.getCgstAmount()).isEqualByComparingTo("181.80");
        assertThat(bill.getSgstAmount()).isEqualByComparingTo("181.80");
        assertThat(bill.getIgstAmount()).isEqualByComparingTo("0");
        assertThat(bill.getRoundOff()).isEqualByComparingTo("0.40");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("2384.00");
    }

    @Test
    void recalculate_chargesIgstInterStateAfterDiscountWithoutRoundingAnOrder() {
        TradeDocument quotation = document(DocumentType.QUOTATION, true, false,
                line(1, 5, "4500", "2", "18"), line(2, 5, "800", "0", "18"));

        engine.recalculate(quotation);

        assertThat(quotation.getTaxableAmount()).isEqualByComparingTo("26050.00");
        assertThat(quotation.getIgstAmount()).isEqualByComparingTo("4689.00");
        assertThat(quotation.getCgstAmount().add(quotation.getSgstAmount())).isEqualByComparingTo("0");
        assertThat(quotation.getRoundOff()).isEqualByComparingTo("0");
        assertThat(quotation.getTotalAmount()).isEqualByComparingTo("30739.00");
    }

    @Test
    void recalculate_putsTheOddPaisaOnCgstSoTheSplitAddsUp() {
        TradeDocument receipt = document(DocumentType.GOODS_RECEIPT, false, false, line(1, 300, "4.90", "2", "18"));

        engine.recalculate(receipt);

        // 1440.60 × 18% = 259.31 → 129.66 + 129.65
        assertThat(receipt.getCgstAmount()).isEqualByComparingTo("129.66");
        assertThat(receipt.getSgstAmount()).isEqualByComparingTo("129.65");
    }

    @Test
    void recalculate_leavesGstOutOfTheVendorTotalUnderReverseCharge() {
        TradeDocument bill = document(DocumentType.PURCHASE_BILL, false, true, line(1, 1, "2000", "0", "18"));

        engine.recalculate(bill);

        assertThat(bill.getCgstAmount().add(bill.getSgstAmount())).isEqualByComparingTo("360.00");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("2000.00");
    }

    @Test
    void pickSourceLines_rejectsMoreThanIsPendingOnTheSource() {
        TradeDocumentLine orderLine = line(7, 500, "17.50", "0", "12");
        orderLine.setFulfilledQuantity(300);
        TradeDocument order = document(DocumentType.PURCHASE_ORDER, true, false, orderLine);

        List<DocumentLineRequest> tooMany = List.of(DocumentLineRequest.builder().sourceLineId(7L).quantity(201).build());
        assertThatThrownBy(() -> engine.pickSourceLines(order, tooMany, TradeDocumentLine::getPendingQuantity))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only 200 left");

        Map<TradeDocumentLine, Integer> picked = engine.pickSourceLines(order,
                List.of(DocumentLineRequest.builder().sourceLineId(7L).quantity(200).build()), TradeDocumentLine::getPendingQuantity);
        assertThat(picked).containsEntry(orderLine, 200);
    }

    @Test
    void pickSourceLines_rejectsALineFromAnotherDocument() {
        TradeDocument order = document(DocumentType.SALES_ORDER, false, false, line(1, 4, "100", "0", "18"));

        List<DocumentLineRequest> foreign = List.of(DocumentLineRequest.builder().sourceLineId(99L).quantity(1).build());
        assertThatThrownBy(() -> engine.pickSourceLines(order, foreign, TradeDocumentLine::getPendingQuantity))
                .isInstanceOf(BadRequestException.class);
    }
}
