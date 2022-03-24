package no.lwollan.passbestilling.qmatic.api;

import static java.lang.String.format;
import static java.time.LocalDate.from;
import static java.time.LocalDateTime.now;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import no.lwollan.passbestilling.qmatic.model.AvailableDate;
import no.lwollan.passbestilling.qmatic.model.AvailableSlot;
import no.lwollan.passbestilling.qmatic.model.Passkontor;
import no.lwollan.passbestilling.qmatic.model.Politidistrikt;

public class QMaticHttpClient implements QMaticAPI {

    private static final Logger logger = Logger.getLogger(QMaticHttpClient.class.getName());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String BASE_URL = "https://pass-og-id.politiet.no";
    private static final String QMATIC_SCHEDULE_API_BASE_URL = "/qmaticwebbooking/rest/schedule";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm");

    private final HttpSupport httpSupport;

    public QMaticHttpClient() {
        httpSupport = new OnlineHttpSupport();
    }

    QMaticHttpClient(final HttpSupport httpSupport) {
        this.httpSupport = httpSupport;
    }

    @Override
    public Map<String, String> getConfiguration() throws QMaticAPIException {
        try {
            logger.log(Level.INFO, "Getting configuration");
            final URI availableDates = URI.create(
                format("%s%s/configuration", BASE_URL, QMATIC_SCHEDULE_API_BASE_URL));
            final HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder()
                    .uri(availableDates)
                    .header("Accept", " application/json")
                    .build(), BodyHandlers.ofString());
            return OBJECT_MAPPER.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new QMaticAPIException("Failed to get configuration.", e);
        }
    }

    @Override
    public List<AvailableDate> findAvailableDates(Passkontor passkontor, int slotSize) throws QMaticAPIException {
        try {
            logger.log(Level.INFO, format("Checking for available dates at %s", passkontor.name));
            final URI availableDatesURI = URI.create(
                format("%s%s/branches/%s/dates;servicePublicId=%s;customSlotLength=%d",
                    BASE_URL, QMATIC_SCHEDULE_API_BASE_URL, passkontor.branchId,
                    passkontor.onlyPassId, slotSize));

            final HttpResponse<String> apiResponse = httpSupport.doGET(availableDatesURI);

            if (apiResponse.statusCode() == 200) {
                return OBJECT_MAPPER.<List<Map<String, String>>>readValue(apiResponse.body(), new TypeReference<>() {})
                    .stream()
                    .map(map -> from(DATE_FORMATTER.parse(map.get("date"))))
                    .map(datetime -> new AvailableDate(passkontor, datetime, slotSize, now()))
                    .collect(Collectors.toList());
            } else {
                logger.log(Level.WARNING, format(
                    "Unable to read available dates from server. Status was %s response body %s",
                    apiResponse.statusCode(), apiResponse.body()));
                return List.of();
            }
        } catch (Exception e) {
            throw new QMaticAPIException("Failed to get findAvailableDates.", e);
        }
    }

    @Override
    public List<AvailableSlot> findAvailableSlots(Passkontor passkontor, LocalDate localDate, int slotSize) throws QMaticAPIException {
        try {
            logger.log(Level.INFO, format("Checking for available slots at %s", passkontor.name));
            final URI availableTimes = URI.create(
                format("%s%s/branches/%s/dates/%s/times;servicePublicId=%s;customSlotLength=%d",
                    BASE_URL, QMATIC_SCHEDULE_API_BASE_URL, passkontor.branchId, localDate, passkontor.onlyPassId,
                    slotSize));

            final HttpResponse<String> apiResponse = httpSupport.doGET(availableTimes);

            if (apiResponse.statusCode() == 200) {
                return OBJECT_MAPPER.<List<Map<String, String>>>readValue(apiResponse.body(), new TypeReference<>() {})
                    .stream()
                    .map(map -> toLocalDateTime(map.get("date"), map.get("time")))
                    .map(availableTime -> new AvailableSlot(passkontor.name, availableTime, slotSize, now()))
                    .collect(Collectors.toList());
            } else {
                logger.log(Level.WARNING, format("Unable to read available dates from server. Status was %s response body %s", apiResponse.statusCode(), apiResponse.body()));
                return List.of();
            }

        } catch (Exception e) {
            throw new QMaticAPIException("Failed to get findAvailableSlots.", e);
        }
    }

    @Override
    public List<Politidistrikt> getPolitidistrikt() throws QMaticAPIException {
        try {
            logger.log(Level.INFO, "Retrieving list of politidistrikt");
            final URI availableTimes = URI.create(
                format("%s%s/branchGroups/", BASE_URL, QMATIC_SCHEDULE_API_BASE_URL));

            final HttpResponse<String> apiResponse = httpSupport.doGET(availableTimes);

            if (apiResponse.statusCode() == 200) {
                return OBJECT_MAPPER.readValue(apiResponse.body(), new TypeReference<>() {});
            } else {
                logger.log(Level.WARNING, format("Unable to read available dates from server. Status was %s response body %s", apiResponse.statusCode(), apiResponse.body()));
                return List.of();
            }

        } catch (Exception e) {
            throw new QMaticAPIException("Failed to get findAvailableSlots.", e);
        }
    }

    private static LocalDateTime toLocalDateTime(String date, String time) {
        return LocalDateTime.from(DATE_TIME_FORMATTER.parse(format("%s %s", date, time)));
    }

    public interface HttpSupport {

        HttpResponse<String> doGET(URI uri) throws IOException, InterruptedException;
    }

     static class OnlineHttpSupport implements HttpSupport {

        @Override
        public HttpResponse<String> doGET(URI uri) throws IOException, InterruptedException {
            logger.log(Level.INFO, format("Sending request to %s", uri));
            final HttpResponse<String> httpResponse = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Accept", " application/json")
                    .build(), BodyHandlers.ofString());

            logger.log(Level.FINE,
                format("Got response. statusCode=%d headers=%s", httpResponse.statusCode(),
                    httpResponse.headers()));
            return httpResponse;
        }

    }

}
