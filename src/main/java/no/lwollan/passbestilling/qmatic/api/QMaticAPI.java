package no.lwollan.passbestilling.qmatic.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import no.lwollan.passbestilling.qmatic.model.AvailableDate;
import no.lwollan.passbestilling.qmatic.model.AvailableSlot;
import no.lwollan.passbestilling.qmatic.model.Passkontor;
import no.lwollan.passbestilling.qmatic.model.Politidistrikt;

/**
 * 'Reverse engineered' api towards the qmatic booking service. There are more operations, but
 * these are a start.
 */
public interface QMaticAPI {

    /**
     * Liste av politidistrikt.
     */
    List<Politidistrikt> getPolitidistrikt() throws QMaticAPIException;

    /**
     * Returns the current configuration of the QMatic application.
     */
    Map<String, String> getConfiguration() throws QMaticAPIException;

    /**
     * Returns list of dates where there are available slots for a given passkontor.
     * @param passkontor which passkontor to query
     * @param slotSize size of slot to find, 10 is used in the web page
     */
    List<AvailableDate> findAvailableDates(Passkontor passkontor, int slotSize) throws QMaticAPIException;

    /**
     * Returns a list of time slots for a given date.
     *
     * @param passkontor which passkontor to query
     * @param localDate data to query
     * @param slotSize size of slot to find, 10 is used in the web page
     */
    List<AvailableSlot> findAvailableSlots(Passkontor passkontor, LocalDate localDate, int slotSize) throws QMaticAPIException;

    /**
     * Wrapper for any exception during communication with qmatic api.
     */
    class QMaticAPIException extends RuntimeException {

        public QMaticAPIException(String message, Exception e) {
            super(message, e);
        }
    }

}
