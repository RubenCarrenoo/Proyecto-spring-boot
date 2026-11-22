package com.coworkhub.business.rules;

import com.coworkhub.domain.BookingItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * RN-07. Cálculo de costos de una reserva:
 * <pre>
 * horasIncluidas = min(duración, horas del plan aún disponibles en el mes)
 * horasExcedentes = duración - horasIncluidas
 * costoSala      = horasExcedentes × tarifaHora × (1 − descuentoPlan)
 * costoServicios = Σ cantidad × precioUnitarioAplicado
 * costoTotal     = costoSala + costoServicios
 * </pre>
 */
@Component
public class CostCalculator {

    public static final int MONEY_SCALE = 2;

    public RoomCost calculateRoomCost(BigDecimal durationHours, BigDecimal availableIncludedHours,
                                      BigDecimal hourlyRate, BigDecimal overageDiscount) {
        BigDecimal available = availableIncludedHours.max(BigDecimal.ZERO);
        BigDecimal included = durationHours.min(available);
        BigDecimal overage = durationHours.subtract(included);
        BigDecimal factor = BigDecimal.ONE.subtract(overageDiscount);
        BigDecimal cost = overage.multiply(hourlyRate).multiply(factor).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new RoomCost(included.setScale(2, RoundingMode.HALF_UP), overage.setScale(2, RoundingMode.HALF_UP), cost);
    }

    public BigDecimal calculateServicesCost(Collection<BookingItem> items) {
        return items.stream()
                .map(BookingItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Resultado del cálculo del costo de la sala. */
    public record RoomCost(BigDecimal includedHours, BigDecimal overageHours, BigDecimal roomCost) {
    }
}
