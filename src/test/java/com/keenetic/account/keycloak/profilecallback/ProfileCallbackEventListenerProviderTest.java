package com.keenetic.account.keycloak.profilecallback;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.jboss.logging.Logger;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProfileCallbackEventListenerProviderTest {

  @Test
  public void doPost() throws URISyntaxException, IOException {

    Logger logger = Logger.getLogger(ProfileCallbackEventListenerProviderFactory.class);

    HashMap<String, Object> setting1 = new HashMap<>();
    setting1.put("url", "https://postman-echo.com/post");
    setting1.put("realm", "*");
    setting1.put("authHeaderName", "X-KEYCLOAK-TOKEN");
    setting1.put("authHeaderValue", "test-token-12345");
    setting1.put("timeout", 10000); // good

    HashMap<String, Object> setting2 = new HashMap<>();
    setting2.put("url", "https://postman-echo.com/post");
    setting2.put("realm", "*");
    setting2.put("timeout", 1); // impossible to reach this timeout

    ArrayList<HashMap<String, Object>> callbacks = new ArrayList<>();
    callbacks.add(setting1);

    ProfileCallbackEventListenerProvider pcelp;
    pcelp = new ProfileCallbackEventListenerProvider(null, logger, callbacks);
    String answer = pcelp.postCallbacks("users", "{\"FirstName\": \"Кириллица\"}");
    // We don't analyze position, don't load json to object. string.contains is enough
    answer = answer.replaceAll("\n", "").replaceAll("\t", "");
    answer = answer.replace("{    ", "{").replace("  }", "}");
    //System.out.println(answer);

    assertTrue(answer.contains("\"data\": {\"FirstName\": \"Кириллица\"}"));
    assertTrue(answer.contains("\"x-keycloak-token\": \"test-token-12345\""));

    callbacks = new ArrayList<>();
    callbacks.add(setting2);

    pcelp = new ProfileCallbackEventListenerProvider(null, logger, callbacks);
    answer = pcelp.postCallbacks("users", "{\"this\": \"our test payload\"}");
    assertTrue(answer.contains("connection timeout for: "));

  }

  @Test
  public void convertToCamelCase(){

      assertEquals("RevokeGrant", ProfileCallbackEventListenerProvider.toCamelCase("revoke_grant"));
      assertEquals("RevokeGrant012", ProfileCallbackEventListenerProvider.toCamelCase("revoke GrAnt 0 1 2"));
  }

  @Test
  public void convertToSnakeCase(){

    assertEquals("revoke_grant", ProfileCallbackEventListenerProvider.toSnakeCase("RevokeGrant"));
    assertEquals("revoke_grant", ProfileCallbackEventListenerProvider.toSnakeCase("revoke_grant"));

  }

  @Test
  public void convertJson() throws IOException {

    Map<String, String> m = new HashMap<>();
    m.put("revoked_client", "keenetic.cloud");
    m.put("another_value", "0");

    assertEquals("{\"revoked_client\":\"keenetic.cloud\",\"another_value\":\"0\"}", ProfileCallbackEventListenerProvider.toJsonString(m));
  }
}
