package com.keenetic.account.keycloak.profilecallback;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProfileCallbackEventListenerProviderTest {

  @Test
  public void doPost() throws URISyntaxException, IOException {

    HashMap<String, Object> setting1 = new HashMap<>();
    setting1.put("url", "https://postman-echo.com/post");
    setting1.put("authHeaderName", "X-KEYCLOAK-TOKEN");
    setting1.put("authHeaderValue", "test-token-12345");
    setting1.put("timeout", 10000); // good

    HashMap<String, Object> setting2 = new HashMap<>();
    setting2.put("url", "https://postman-echo.com/post");
    setting2.put("timeout", 1); // impossible to reach this timeout

    ArrayList<HashMap<String, Object>> callbacks = new ArrayList<>();
    callbacks.add(setting1);

    ProfileCallbackEventListenerProvider pcelp;
    pcelp = new ProfileCallbackEventListenerProvider(null, callbacks);
    String answer = pcelp.postCallbacks("{\"FirstName\": \"Кириллица\"}");
    // We don't analyze position, don't load json to object. string.contains is enough
    // System.out.println(answer);
    assertTrue(answer.contains("\"data\":{\"FirstName\":\"Кириллица\"}"));
    assertTrue(answer.contains("\"x-keycloak-token\":\"test-token-12345\""));

    callbacks = new ArrayList<>();
    callbacks.add(setting2);

    pcelp = new ProfileCallbackEventListenerProvider(null, callbacks);
    answer = pcelp.postCallbacks("{\"this\": \"our test payload\"}");
    assertTrue(answer.contains("connection timeout for: "));

  }

}
