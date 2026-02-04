package io.dropwizard.bundles.version;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VersionServletTest {
  private static final ObjectMapper OBJECT_MAPPER = Jackson.newObjectMapper();
  private static final String PATH = "/version";

  private Server server;
  private LocalConnector connector;
  private final VersionSupplier supplier = mock(VersionSupplier.class);

  @BeforeEach
  void setup() throws Exception {
    server = new Server();
    connector = new LocalConnector(server);
    server.addConnector(connector);

    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/");
    context.addServlet(new ServletHolder(new VersionServlet(supplier, OBJECT_MAPPER)), PATH);
    server.setHandler(context);
    server.start();
  }

  @AfterEach
  void teardown() throws Exception {
    server.stop();
  }

  @Test
  void testNonNullApplicationVersion() {
    when(supplier.getApplicationVersion()).thenReturn("version");

    HttpTester.Response response = get();
    assertEquals(200, response.getStatus());

    JsonNode root = fromJson(response.getContent());
    assertEquals("version", root.get("application").textValue());
  }

  @Test
  void testNullApplicationVersion() {
    when(supplier.getApplicationVersion()).thenReturn(null);

    HttpTester.Response response = get();
    assertEquals(200, response.getStatus());

    JsonNode root = fromJson(response.getContent());
    assertNull(root.get("application").textValue());
  }

  @Test
  void testThrowsApplicationVersionException() {
    when(supplier.getApplicationVersion()).thenThrow(new RuntimeException());

    HttpTester.Response response = get();
    assertEquals(500, response.getStatus());
  }

  @Test
  void testNonNullDependencyVersion() {
    when(supplier.getDependencyVersions()).thenReturn(Map.of("guava", "version"));

    HttpTester.Response response = get();
    assertEquals(200, response.getStatus());

    JsonNode root = fromJson(response.getContent());
    assertEquals("version", root.get("dependencies").get("guava").textValue());
  }

  @Test
  void testNullDependencyVersion() {
    when(supplier.getDependencyVersions()).thenReturn(mapWithNullValue("guava"));

    HttpTester.Response response = get();
    assertEquals(200, response.getStatus());

    JsonNode root = fromJson(response.getContent());
    assertNull(root.get("dependencies").get("guava").textValue());
  }

  @Test
  void testThrowsDependencyVersionException() {
    when(supplier.getDependencyVersions()).thenThrow(new RuntimeException());

    HttpTester.Response response = get();
    assertEquals(500, response.getStatus());
  }

  private HttpTester.Response get() {
    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod("GET");
    request.setVersion("HTTP/1.0");
    request.setURI(PATH);

    try {
      ByteBuffer rawResponse = connector.getResponse(request.generate());
      return HttpTester.parseResponse(rawResponse);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static JsonNode fromJson(String s) {
    try {
      return OBJECT_MAPPER.readTree(s);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, String> mapWithNullValue(String key) {
    java.util.HashMap<String, String> m = new java.util.HashMap<>();
    m.put(key, null);
    return m;
  }
}
