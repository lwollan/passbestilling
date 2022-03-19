package no.lwollan.passbestilling.qmatic.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * This represents a date where there is a time slot available.
 */
public class AvailableDate {

    public final Passkontor passkontor;
    public final LocalDate time;
    public final int slotSize;
    public final LocalDateTime checkedAt;

    public AvailableDate(Passkontor passkontor, LocalDate time, int slotSize, LocalDateTime checkedAt) {
        this.passkontor = passkontor;
        this.time = time;
        this.slotSize = slotSize;
        this.checkedAt = checkedAt;
    }

}
