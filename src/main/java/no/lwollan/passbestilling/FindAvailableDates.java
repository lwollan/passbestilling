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

public class FindAvailableDates {

    final static String ID_KORT = "8e859bd4c1752249665bf2363ea231e1678dbb7fc4decff862d9d41975a9a95a";
    final static String PASS =  "d1b043c75655a6756852ba9892255243c08688a071e3b58b64c892524f58d098";

    static final Logger logger = Logger.getLogger(FindAvailableDates.class.getName());

    public static void main(String[] args) {
        // default logging is not too pretty...
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] %5$s %n");

        int earliestHour;
        int maxMonthsAhead;
        int slotSize;
        String service;
        if (args.length > 0) {
            service = "i".equals(args[0]) ? ID_KORT : PASS;
        } else {
            service = PASS;
        }

        if (args.length > 1) {
            earliestHour = Integer.parseInt(args[1]);
        } else {
            earliestHour = 8;
        }
        if (args.length > 2) {
            maxMonthsAhead = Integer.parseInt(args[2]);
        } else {
            maxMonthsAhead = 3;
        }

        if (args.length > 3) {
            slotSize = Integer.parseInt(args[3]);
        } else {
            slotSize = 10;
        }

        logger.info(format("Looking for %s", service.equals(PASS) ? "pass" : "id-kort"));
        final List<Passkontor> passkontorToBeChecked = getPasskontor(service);

        FindAvailableDates app = new FindAvailableDates(passkontorToBeChecked);
        app.run(earliestHour, maxMonthsAhead, slotSize);
    }

    private final List<Passkontor> passkontorToBeChecked;
    private final QMaticAPI qmaticAPI;

    FindAvailableDates(List<Passkontor> passkontorsToCheck) {
        this.qmaticAPI = new QMaticHttpClient();
        this.passkontorToBeChecked = passkontorsToCheck;
    }

    void run(int earliestHour, int maxMonthsAhead, int slotSize) {
        logger.info(format("Looking for slots of %d minutes (10 is normal), after %d in the coming %d months.", slotSize, earliestHour, maxMonthsAhead));
        final List<AvailableSlot> availableSlots = findAvailableDates(
            passkontorToBeChecked,
            isSlotAfter(LocalTime.of(earliestHour, 0)),
            isAvailableDateBefore(LocalDate.now().plusMonths(maxMonthsAhead)), slotSize);

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
                Predicate<AvailableDate> dateFilter, int slotSize) {
        return passkontorToBeChecked.stream()
            .flatMap(passkontor -> qmaticAPI.findAvailableDates(passkontor, slotSize).stream())
            .filter(dateFilter)
            .map(dateWithSlot -> qmaticAPI.findAvailableSlots(dateWithSlot.passkontor, dateWithSlot.time, dateWithSlot.slotSize))
            .flatMap(Collection::stream)
            .filter(slotFilter)
            .peek(slot -> logger.log(FINE, format("Found slot at %s %s", slot.passkontor, slot.time)))
            .collect(Collectors.toList());
    }

    private static List<Passkontor> getPasskontor(String service) {
        Passkontor sandvika = new Passkontor(
            "e8cfd353ad2e1f03432faa6d1c1ea3401102eb0bf7e8f9e9fe0e2607f867a8a0",
                service,
            "Bærum politistasjon");

        Passkontor gronland = new Passkontor(
            "c4afbbf5b8fea8ae5b5749370eaa26ae1c405797f39e8b6bfec5f3f96255451e",
                service,
            "Grønland politistasjon");

        Passkontor hamar = new Passkontor(
                "a4bbbc39e65113ba59720574294a97734d3e809b5c53d6decc9be5dbdea65a56",
                service,
                "Hamar politistasjon");

        Passkontor lillehammer = new Passkontor(
                "a2a38ea1aaf8a6f7ff54c508993a691a2992ed049048fff085d069476ab7d136",
                service,
                "Lillehammer politistasjon");

        // gjøvik
        Passkontor gjovik = new Passkontor(
                "444a3fac353a44ab43bdc6d2b832a25d4ade4230dde93db2f92215d7faabb554",
                service,
                "Gjøvik politistasjon");

        // Eleverum
        Passkontor elverum = new Passkontor(
                "12b2d60319641ea9240b00a4baa7568988b7af327d8ab5a5596f2a2ef496f22a",
                service,
                "Elverum politistasjon");

        // Eleverum
        Passkontor kongsvinger = new Passkontor(
                "cfdc05f9ee15105147bba99804ee747092517b5be0c7676fd3816cd4238308f0",
                service,
                "Kongsvinger politistasjon");
        return List.of(sandvika, gronland, hamar, lillehammer, gjovik, elverum, kongsvinger);
    }

    private static Predicate<AvailableSlot> isSlotAfter(LocalTime localTime) {
        return slot -> slot.time.toLocalTime().isAfter(localTime);
    }

    private static Predicate<AvailableDate> isAvailableDateBefore(LocalDate date) {
        return slot -> slot.time.isBefore(date);
    }

}
