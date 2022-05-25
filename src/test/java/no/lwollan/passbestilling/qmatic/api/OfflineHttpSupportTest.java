package no.lwollan.passbestilling.qmatic.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import no.lwollan.passbestilling.qmatic.api.QMaticHttpClient.HttpSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OfflineHttpSupportTest {

    private HttpSupport httpSupport;

    @BeforeEach
    void setUp() {
        httpSupport = new OfflineHttpSupport();
    }

    @ParameterizedTest
    @ValueSource(strings = { "config", "branchGroups", "appointmentProfiles"})
    void should_return_existing_resource(String resource) throws IOException, InterruptedException {
        final HttpResponse<String> httpResponse = httpSupport.doGET(URI.create("http://somewhere/" + resource));
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(httpResponse.body()).isNotEmpty();
    }

    @Test
    void should_return_not_found_for_non_existing_resource() throws IOException, InterruptedException {
        final HttpResponse<String> httpResponse = httpSupport.doGET(URI.create("http://somewhere/does_not_exist"));
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(403);
        assertThat(httpResponse.body()).isNull();
    }

}