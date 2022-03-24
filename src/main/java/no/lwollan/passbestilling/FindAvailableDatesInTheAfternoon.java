package no.lwollan.passbestilling;

import static java.lang.String.format;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.stream.Collectors.groupingBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import no.lwollan.passbestilling.qmatic.api.QMaticAPI;
import no.lwollan.passbestilling.qmatic.api.QMaticHttpClient;
import no.lwollan.passbestilling.qmatic.model.AvailableDate;
import no.lwollan.passbestilling.qmatic.model.AvailableSlot;
import no.lwollan.passbestilling.qmatic.model.Passkontor;

public class FindAvailableDatesInTheAfternoon {

    static final Logger logger = Logger.getLogger(FindAvailableDatesInTheAfternoon.class.getName());

    public static void main(String[] args) {
        // default logging is not too pretty...
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] %5$s %n");

        int earliestHour;
        int maxMonthsAhead;
        if (args.length == 2) {
            earliestHour = Integer.parseInt(args[0]);
            maxMonthsAhead = Integer.parseInt(args[1]);
        } else {
            earliestHour = 16;
            maxMonthsAhead = 6;
        }

        final List<Passkontor> passkontorToBeChecked = getPasskontor();

        FindAvailableDatesInTheAfternoon app = new FindAvailableDatesInTheAfternoon(passkontorToBeChecked);
        app.run(earliestHour, maxMonthsAhead);
    }

    private final List<Passkontor> passkontorToBeChecked;
    private final QMaticAPI qmaticAPI;

    FindAvailableDatesInTheAfternoon(List<Passkontor> passkontorsToCheck) {
        this.qmaticAPI = new QMaticHttpClient();
        this.passkontorToBeChecked = passkontorsToCheck;
    }

    void run(int earliestHour, int maxMonthsAhead) {
        logger.info(format("Looking for slots after %d in the coming %d monts.", earliestHour, maxMonthsAhead));
        final List<AvailableSlot> availableSlots = findAvailableDates(
            passkontorToBeChecked,
            isSlotAfter(LocalTime.of(earliestHour, 0)),
            isAvailableDateBefore(LocalDate.now().plusMonths(maxMonthsAhead)));

        // Group available slots by passkontor
        availableSlots
            .stream()
            .collect(groupingBy(slot -> slot.passkontor))
            .forEach((kontor, slots) -> logger.log(INFO, format("Found slot at %s: %s", kontor, slots.stream()
                .map(ss -> ss.time)
                .sorted()
                .collect(Collectors.toList()))));
    }

    private List<AvailableSlot> findAvailableDates
        (
            List<Passkontor> passkontorToBeChecked,
            Predicate<AvailableSlot> slotFilter,
            Predicate<AvailableDate> dateFilter) {
        return passkontorToBeChecked.stream()
            .flatMap(passkontor -> qmaticAPI.findAvailableDates(passkontor, 10).stream())
            .filter(dateFilter)
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

    private static Predicate<AvailableSlot> isSlotAfter(LocalTime localTime) {
        return slot -> slot.time.toLocalTime().isAfter(localTime);
    }

    private static Predicate<AvailableDate> isAvailableDateBefore(LocalDate date) {
        return slot -> slot.time.isBefore(date);
    }

}
