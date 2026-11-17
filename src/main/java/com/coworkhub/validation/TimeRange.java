package com.coworkhub.validation;

/**
 * Contrato para DTOs que tienen un inicio y un fin (horario de sede, reserva).
 * Lo usa {@link ValidTimeRange} para comprobar que inicio &lt; fin.
 */
public interface TimeRange<T extends Comparable<? super T>> {

    T rangeStart();

    T rangeEnd();
}
