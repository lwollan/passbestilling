package no.lwollan.passbestilling.qmatic.api;

import static org.junit.jupiter.api.Assertions.*;

import no.lwollan.passbestilling.qmatic.api.QMaticHttpClient.OnlineHttpSupport;
import org.junit.jupiter.api.Test;

class QMaticHttpClientIT {

    @Test
    void should_fetch_list_of_politidistrikt() {
        final QMaticHttpClient httpClient = new QMaticHttpClient(new OnlineHttpSupport());
        httpClient.getPolitidistrikt();
    }

}