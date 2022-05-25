package no.lwollan.passbestilling;

import no.lwollan.passbestilling.qmatic.api.QMaticAPI;
import no.lwollan.passbestilling.qmatic.api.QMaticHttpClient;
import no.lwollan.passbestilling.qmatic.model.AvailableDate;
import no.lwollan.passbestilling.qmatic.model.AvailableSlot;
import no.lwollan.passbestilling.qmatic.model.Passkontor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.stream.Collectors.groupingBy;

public class FindAvailableDates {

    static final Logger logger = Logger.getLogger(FindAvailableDates.class.getName());

    public static void main(String[] args) throws FileNotFoundException {
        // default logging is not too pretty...
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] %5$s %n");

        if (args.length == 0) {
            printUsage();
        } else {

            String service;

            if ("idkort".equals(args[0])) {
                service = ServiceId.ID_KORT;
            } else {
                service = ServiceId.PASS;
            }

            Map<String, String> commandLineArguments = Arrays.stream(args)
                    .map(parmeter -> parmeter.split("="))
                    .filter(a -> a.length > 1)
                    .collect(Collectors.toMap(a -> a[0], a -> a[1]));

            int earliest = Integer.parseInt(commandLineArguments.getOrDefault("earliest", "8"));
            int months = Integer.parseInt(commandLineArguments.getOrDefault("months", "8"));
            int slot = Integer.parseInt(commandLineArguments.getOrDefault("slot", "10"));
            String passkontorConfig = commandLineArguments.getOrDefault("config", "");

            List<Passkontor> passkontorToBeChecked;
            if (passkontorConfig.isEmpty()) {
                logger.info("Bruker standard passkontorliste");
                passkontorToBeChecked = defaultPasskontor();
            } else {
                logger.info("Bruker egen passkontorliste");
                passkontorToBeChecked = loadPasskontor(new FileInputStream(passkontorConfig));
            }

            FindAvailableDates app = new FindAvailableDates(passkontorToBeChecked, service);
            app.run(earliest, months, slot);
        }
    }

    private static List<Passkontor> defaultPasskontor() {
        return loadPasskontor(FindAvailableDates.class.getResourceAsStream("/passkontor.properties"));
    }

    private static List<Passkontor> loadPasskontor(InputStream propertiesStream) {
        try {
            Properties properties = new Properties();
            properties.load(propertiesStream);
            return properties.keySet().stream()
                    .map(key -> new Passkontor(key.toString(), properties.getProperty(key.toString())))
                    .sorted(comparing(kontor -> kontor.name))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printUsage() {
        System.out.println("Passbestillingshelper");
        System.out.println("Leter igjennom tilgengelige avtaler på:");
        System.out.println(" " + defaultPasskontor().stream().map(p -> p.name).collect(Collectors.joining(", ")));
        System.out.println("Bruk: ");
        System.out.println(" [pass|idkort] earliest=<tidligste tidspunt på time i time> months=<antall måneder å sjekke fram i tid>");
        System.out.println();
        System.out.println("f.eks: pass earliest=10 months=2");
        System.out.println("  søker etter passtimer etter klokka 10:00 fra nå og 2 måneder fram i tid.");
        System.out.println();
        System.out.println("Husk at du må bestille timen selv via https://pass-og-id.politiet.no/timebestilling/");
        System.out.println();
    }

    private final List<Passkontor> passkontorToBeChecked;
    private final QMaticAPI qmaticAPI;

    private final String serviceId;

    FindAvailableDates(List<Passkontor> passkontorsToCheck, String serviceId) {
        this.qmaticAPI = new QMaticHttpClient();
        this.passkontorToBeChecked = passkontorsToCheck;
        this.serviceId = serviceId;
    }

    void run(int earliestHour, int maxMonthsAhead, int slotSize) {
        String lookingFor = serviceId.equals(ServiceId.PASS) ? "pass" : "id-kort";
        logger.info(format("Looking for %s slots of %d minutes (10 is normal), after %d in the coming %d months.", lookingFor, slotSize, earliestHour, maxMonthsAhead));
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
                .limit(25)
                .collect(Collectors.toList()))));
    }

    private List<AvailableSlot> findAvailableDates
        (
                List<Passkontor> passkontorToBeChecked,
                Predicate<AvailableSlot> slotFilter,
                Predicate<AvailableDate> dateFilter, int slotSize) {
        return passkontorToBeChecked.stream()
            .flatMap(passkontor -> qmaticAPI.findAvailableDates(passkontor, serviceId, slotSize).stream())
            .filter(dateFilter)
            .map(dateWithSlot -> qmaticAPI.findAvailableSlots(dateWithSlot.passkontor, serviceId, dateWithSlot.time, dateWithSlot.slotSize))
            .flatMap(Collection::stream)
            .filter(slotFilter)
            .peek(slot -> logger.log(FINE, format("Found slot at %s %s", slot.passkontor, slot.time)))
            .collect(Collectors.toList());
    }
    private static Predicate<AvailableSlot> isSlotAfter(LocalTime localTime) {
        return slot -> slot.time.toLocalTime().isAfter(localTime);
    }
    private static Predicate<AvailableDate> isAvailableDateBefore(LocalDate date) {
        return slot -> slot.time.isBefore(date);
    }
    static class ServiceId {
        final static String ID_KORT = "8e859bd4c1752249665bf2363ea231e1678dbb7fc4decff862d9d41975a9a95a";
        final static String PASS = "d1b043c75655a6756852ba9892255243c08688a071e3b58b64c892524f58d098";

    }

}
