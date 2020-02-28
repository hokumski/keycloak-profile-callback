package com.keenetic.account.keycloak.profilecallback;

import org.apache.http.conn.ConnectTimeoutException;
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
    setting1.put("timeout", 10000); // good

    HashMap<String, Object> setting2 = new HashMap<>();
    setting2.put("url", "https://postman-echo.com/post");
    setting2.put("timeout", 1); // impossible to reach this timeout

    ArrayList<HashMap<String, Object>> callbacks = new ArrayList<>();
    callbacks.add(setting1);

    ProfileCallbackEventListenerProvider pcelp =
        new ProfileCallbackEventListenerProvider(null, callbacks);
    String answer = pcelp.postCallbacks("{\"this\": \"our test payload\"}");
    assertTrue(answer.contains("our test payload"));

    callbacks = new ArrayList<>();
    callbacks.add(setting2);

    pcelp = new ProfileCallbackEventListenerProvider(null, callbacks);
    answer = pcelp.postCallbacks("{\"this\": \"our test payload\"}");
    assertTrue(answer.contains("connection timeout for: "));

  }

}
