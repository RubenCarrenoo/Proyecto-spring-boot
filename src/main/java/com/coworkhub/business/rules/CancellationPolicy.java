package com.coworkhub.business.rules;

import com.coworkhub.exception.BusinessRuleException;
import com.coworkhub.exception.ErrorCodes;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * RN-08. Política de cancelación según la anticipación respecto al inicio:
 * <ul>
 *   <li>&ge; 24 h: gratis y se devuelven las horas incluidas del plan.</li>
 *   <li>Entre 24 h y 2 h: penalización del 50 % del costo de la sala.</li>
 *   <li>&lt; 2 h o reserva ya iniciada: no se permite cancelar.</li>
 * </ul>
 */
@Component
public class CancellationPolicy {

    public static final Duration FREE_CANCELLATION = Duration.ofHours(24);
    public static final Duration MIN_CANCELLATION = Duration.ofHours(2);
    public static final BigDecimal PENALTY_RATE = new BigDecimal("0.50");

    public Result evaluate(LocalDateTime bookingStart, BigDecimal roomCost, LocalDateTime now) {
        if (!now.isBefore(bookingStart)) {
            throw new BusinessRuleException(ErrorCodes.RN_08, "No se puede cancelar una reserva que ya inició");
        }
        Duration advance = Duration.between(now, bookingStart);
        if (advance.compareTo(MIN_CANCELLATION) < 0) {
            throw new BusinessRuleException(ErrorCodes.RN_08,
                    "No se puede cancelar con menos de 2 horas de anticipación");
        }
        if (advance.compareTo(FREE_CANCELLATION) >= 0) {
            return new Result(BigDecimal.ZERO.setScale(2), true);
        }
        BigDecimal fee = roomCost.multiply(PENALTY_RATE).setScale(2, RoundingMode.HALF_UP);
        return new Result(fee, false);
    }

    /**
     * @param fee               cargo por cancelación
     * @param refundIncludedHours true si se devuelven al plan las horas incluidas usadas
     */
    public record Result(BigDecimal fee, boolean refundIncludedHours) {
    }
}
