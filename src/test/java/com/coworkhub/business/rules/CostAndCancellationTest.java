package com.coworkhub.business.rules;

import com.coworkhub.TestData;
import com.coworkhub.domain.BookingItem;
import com.coworkhub.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CostAndCancellationTest {

    private final CostCalculator calculator = new CostCalculator();
    private final CancellationPolicy policy = new CancellationPolicy();

    // ---------------- RN-07 Costos ----------------

    @Test
    @DisplayName("RN-07: horas cubiertas por el plan no se cobran")
    void includedHoursAreFree() {
        var cost = calculator.calculateRoomCost(new BigDecimal("2"), new BigDecimal("10"),
                new BigDecimal("50000"), new BigDecimal("0.15"));
        assertThat(cost.includedHours()).isEqualByComparingTo("2");
        assertThat(cost.overageHours()).isEqualByComparingTo("0");
        assertThat(cost.roomCost()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("RN-07: excedente = horas × tarifa × (1 − descuento)")
    void overageWithDiscount() {
        // 3 h, quedan 1 h incluidas -> 2 h excedentes × 50.000 × 0,85 = 85.000
        var cost = calculator.calculateRoomCost(new BigDecimal("3"), new BigDecimal("1"),
                new BigDecimal("50000"), new BigDecimal("0.15"));
        assertThat(cost.includedHours()).isEqualByComparingTo("1");
        assertThat(cost.overageHours()).isEqualByComparingTo("2");
        assertThat(cost.roomCost()).isEqualByComparingTo("85000.00");
    }

    @Test
    @DisplayName("RN-07: si las horas del plan se agotaron, todo es excedente")
    void noIncludedHoursLeft() {
        var cost = calculator.calculateRoomCost(new BigDecimal("1.5"), new BigDecimal("-2"),
                new BigDecimal("40000"), new BigDecimal("0"));
        assertThat(cost.includedHours()).isEqualByComparingTo("0");
        assertThat(cost.roomCost()).isEqualByComparingTo("60000.00");
    }

    @Test
    @DisplayName("RN-07: servicios = Σ cantidad × precio aplicado")
    void servicesCost() {
        var cafe = new BookingItem(TestData.service(1L, "8000"), 2);
        var refrigerio = new BookingItem(TestData.service(3L, "12000"), 3);
        assertThat(calculator.calculateServicesCost(List.of(cafe, refrigerio))).isEqualByComparingTo("52000.00");
    }

    @Test
    @DisplayName("RN-07: el ítem conserva el precio histórico aunque cambie el catálogo")
    void historicalPrice() {
        var service = TestData.service(1L, "8000");
        var item = new BookingItem(service, 1);
        service.setUnitPrice(new BigDecimal("9500"));
        assertThat(item.getAppliedUnitPrice()).isEqualByComparingTo("8000");
    }

    // ---------------- RN-08 Cancelaciones ----------------

    private final LocalDateTime now = LocalDateTime.of(2030, 1, 15, 8, 0);

    @Test
    @DisplayName("RN-08: con 30 h de anticipación es gratis y devuelve horas")
    void freeCancellation() {
        var result = policy.evaluate(now.plusHours(30), new BigDecimal("68000"), now);
        assertThat(result.fee()).isEqualByComparingTo("0");
        assertThat(result.refundIncludedHours()).isTrue();
    }

    @Test
    @DisplayName("RN-08: exactamente 24 h sigue siendo gratis")
    void exactly24Hours() {
        assertThat(policy.evaluate(now.plusHours(24), new BigDecimal("68000"), now).fee()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("RN-08: con 5 h de anticipación se cobra el 50 % de la sala")
    void halfPenalty() {
        var result = policy.evaluate(now.plusHours(5), new BigDecimal("68000"), now);
        assertThat(result.fee()).isEqualByComparingTo("34000.00");
        assertThat(result.refundIncludedHours()).isFalse();
    }

    @Test
    @DisplayName("RN-08: con menos de 2 h no se permite cancelar")
    void tooLate() {
        assertThatThrownBy(() -> policy.evaluate(now.plusHours(1), BigDecimal.TEN, now))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("RN-08");
    }

    @Test
    @DisplayName("RN-08: una reserva iniciada no se puede cancelar")
    void alreadyStarted() {
        assertThatThrownBy(() -> policy.evaluate(now.minusMinutes(10), BigDecimal.TEN, now))
                .extracting("code").isEqualTo("RN-08");
    }
}
