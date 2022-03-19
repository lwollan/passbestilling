package no.lwollan.passbestilling;

import static java.lang.String.format;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;

import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import no.lwollan.passbestilling.qmatic.api.QMaticAPI;
import no.lwollan.passbestilling.qmatic.api.QMaticHttpClient;
import no.lwollan.passbestilling.qmatic.model.AvailableSlot;
import no.lwollan.passbestilling.qmatic.model.Passkontor;

public class FindAvailableDatesInTheAfternoon {

    static final Logger logger = Logger.getLogger(FindAvailableDatesInTheAfternoon.class.getName());

    public static void main(String[] args) {
        // default logging is not too pretty...
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] %5$s %n");

        final List<Passkontor> passkontorToBeChecked = getPasskontor();

        FindAvailableDatesInTheAfternoon app = new FindAvailableDatesInTheAfternoon(passkontorToBeChecked);
        app.run();
    }

    private final List<Passkontor> passkontorToBeChecked;
    private final QMaticAPI qmaticAPI;

    FindAvailableDatesInTheAfternoon(List<Passkontor> passkontorsToCheck) {
        this.qmaticAPI = new QMaticHttpClient();
        this.passkontorToBeChecked = passkontorsToCheck;
    }

    void run() {
        final List<AvailableSlot> availableSlots = findAvailableDates(passkontorToBeChecked, greaterThan(HOUR_OF_DAY, 16));

        availableSlots
            .forEach(slot -> logger.log(INFO, format("Found slot at %s %s", slot.passkontor, slot.time)));
    }

    private List<AvailableSlot> findAvailableDates(List<Passkontor> passkontorToBeChecked, Predicate<AvailableSlot> slotFilter) {
        return passkontorToBeChecked.stream()
            .map(passkontor -> qmaticAPI.findAvailableDates(passkontor, 10))
            .peek(availableDates -> logger.log(FINE, format("Found %d slots.", availableDates.size())))
            .flatMap(Collection::stream)
            .map(dateWithSlot -> qmaticAPI.findAvailableSlots(dateWithSlot.passkontor, dateWithSlot.time, dateWithSlot.slotSize))
            .flatMap(Collection::stream)
            .filter(slotFilter)
            .peek(slot -> logger.log(FINE, format("Found slot at %s %s", slot.passkontor, slot.time)))
            .collect(Collectors.toList());
    }

    private static List<Passkontor> getPasskontor() {
        Passkontor sandvika = new Passkontor(
            "e8cfd353ad2e1f03432faa6d1c1ea3401102eb0bf7e8f9e9fe0e2607f867a8a0",
            "d1b043c75655a6756852ba9892255243c08688a071e3b58b64c892524f58d098",
            "Bærum politistasjon");

        Passkontor gronland = new Passkontor(
            "c4afbbf5b8fea8ae5b5749370eaa26ae1c405797f39e8b6bfec5f3f96255451e",
            "d1b043c75655a6756852ba9892255243c08688a071e3b58b64c892524f58d098",
            "Grønland politistasjon");

        return List.of(sandvika, gronland);
    }

    private static Predicate<AvailableSlot> greaterThan(ChronoField chronoField, int greaterThan) {
        return slot -> slot.time.get(chronoField) > greaterThan;
    }

}
