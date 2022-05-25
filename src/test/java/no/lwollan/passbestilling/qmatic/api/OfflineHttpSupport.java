package no.lwollan.passbestilling.qmatic.api;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import no.lwollan.passbestilling.qmatic.api.QMaticHttpClient.HttpSupport;

/**
 * Will attempt to read resources from classpath, based on uri. This can be used for testing to
 * avoid annoying online service.
 */
public class OfflineHttpSupport implements HttpSupport {

    @Override
    public HttpResponse<String> doGET(URI uri)
        throws IOException {
        if (exisitsOnClasspath(uri)) {
            try {
                byte[] buffer = readFromClasspath(mapToClassPathResource(uri));
                return mockResponse(new String(buffer));
            } catch (URISyntaxException e) {
                throw new IOException("Unable to find mock resource " + uri.getPath(), e);
            }
        } else {
            // will return not found
            return mockResponse(null);
        }
    }

    static private byte[] readFromClasspath(String classPathResource) throws URISyntaxException, IOException {
        final URL resource = OfflineHttpSupport.class.getResource(classPathResource);
        final File file = new File(resource.toURI());
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        byte[] buffer = new byte[dis.available()];
        dis.readFully(buffer);
        return buffer;
    }

    static private boolean exisitsOnClasspath(URI uri) {
        return OfflineHttpSupport.class.getResource(mapToClassPathResource(uri)) != null;
    }

    static private String mapToClassPathResource(URI uri) {
        return uri.getPath() + ".json";
    }

    /**
     * Simple response mock, if body is non-null, status code is 200, else it will be 403. Value
     * of other properties may be unpredictable.
     */
    static private HttpResponse<String> mockResponse(String body) {

        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return body == null ? 403 : 200;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return null;
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public Version version() {
                return null;
            }
        };
    }
}
