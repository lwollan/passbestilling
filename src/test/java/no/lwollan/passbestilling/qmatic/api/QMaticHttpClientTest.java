package no.lwollan.passbestilling.qmatic.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import no.lwollan.passbestilling.qmatic.model.Passkontor;
import no.lwollan.passbestilling.qmatic.model.Politidistrikt;
import org.junit.jupiter.api.Test;

class QMaticHttpClientTest {

    @Test
    void should_query_politidistrikt_and_filter_on_id() {
        final QMaticHttpClient httpClient = new QMaticHttpClient();
        final List<Politidistrikt> politidistrikt = httpClient.getPolitidistrikt();
        assertThat(politidistrikt).isNotEmpty();

        final List<Passkontor> passkontors = politidistrikt.stream()
            .filter(p -> Objects.equals(57, p.getId()))
            .map(Politidistrikt::getBranches)
            .flatMap(Collection::stream)
            .map(p -> new Passkontor(p.get("id").toString(), null, p.get("name").toString()))
            .collect(Collectors.toList());

        System.out.println(passkontors);
    }

}