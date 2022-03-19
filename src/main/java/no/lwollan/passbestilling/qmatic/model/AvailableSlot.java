package no.lwollan.passbestilling.qmatic.model;

import java.time.LocalDateTime;

/**
 * This is a slot that should be possible to book in the web application.
 */
public class AvailableSlot {

    public final String passkontor;
    public final LocalDateTime time;
    public final int slotSize;
    public final LocalDateTime checkedAt;

    public AvailableSlot(String passkontor, LocalDateTime time, int slotSize,
        LocalDateTime checkedAt) {
        this.passkontor = passkontor;
        this.time = time;
        this.slotSize = slotSize;
        this.checkedAt = checkedAt;
    }

}
