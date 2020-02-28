package com.keenetic.account.keycloak.profilecallback;

import java.io.IOException;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.RealmModel;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectTimeoutException;


public class ProfileCallbackEventListenerProvider  implements EventListenerProvider {

  private KeycloakSession session;
  private JsonFactory jsonFactory;
  private ArrayList<HashMap<String, Object>> callbacks;

  ProfileCallbackEventListenerProvider(KeycloakSession session, ArrayList<HashMap<String, Object>> callbacks) {
    this.callbacks = callbacks;
    this.session = session;
    this.jsonFactory = new JsonFactory();
  }

  @Override
  public void onEvent(Event event) {

    switch (event.getType()) {
      case UPDATE_EMAIL:
      case UPDATE_PROFILE:
        {
          System.out.println("logged email/profile update for " + event.getUserId());
          try {
            String userData = getUserInfo(event.getUserId());
            System.out.println(userData);
            postCallbacks(userData);
          } catch (IOException ignored) {}
          break;
        }
    }
  }

  /**
   * Return JSON with user data
   *
   * @param userId keycloak user id
   * @return json as string,
   *    like {"Id": "b14bd453-2708-4713-82b7-5b2a317264f7", "Email": "user@server.com",
   *    "FirstName": "First", "LastName": "Last"}
   * @throws IOException
   */
  String getUserInfo(String userId) throws IOException {
    StringWriter jsonObjectWriter = new StringWriter();
    RealmModel realmModel = session.getContext().getRealm();
    UserModel userModel = session.userCache().getUserById(userId, realmModel);
    Map<String, List<String>> userAttributes = userModel.getAttributes();

    JsonGenerator generator = this.jsonFactory.createGenerator(jsonObjectWriter);
    generator.writeStartObject();
    generator.writeStringField("Id", userId);
    generator.writeStringField("Email", userModel.getEmail());
    generator.writeStringField("FirstName", userModel.getFirstName());
    generator.writeStringField("LastName", userModel.getLastName());
    if (userAttributes.containsKey("locale")) {
      generator.writeStringField("Locale", userAttributes.get("locale").get(0));
    }
    if (userAttributes.containsKey("phone")) {
      generator.writeStringField("Phone", userAttributes.get("phone").get(0));
    }
    generator.writeEndObject();
    generator.close();
    return jsonObjectWriter.toString();
  }

  /**
   * Posts payload to callback URL
   *
   * @param payload - string
   * @return - answer from server
   * @throws IOException
   */
  String postCallbacks(String payload) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (HashMap<String, Object> callback : this.callbacks) {
      String url = (String) callback.get("url");
      try {
        HttpPost post = new HttpPost(url);

        if (callback.containsKey("timeout")) {
          int timeout = (int) callback.get("timeout");
          final RequestConfig params =
              RequestConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout).build();
          post.setConfig(params);
        }

        // send a JSON data
        post.setEntity(new StringEntity(payload));
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(post)) {
          sb.append(EntityUtils.toString(response.getEntity()));
          sb.append("\n");
        }
      } catch (UnknownHostException ignored) {
        sb.append("unknown host: ");
        sb.append(url);
        sb.append("\n");
      } catch (ConnectTimeoutException ignored) {
        sb.append("connection timeout for: ");
        sb.append(url);
        sb.append("\n");
      } catch (Exception ignored) {
        sb.append("unknown error for: ");
        sb.append(url);
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  @Override
  public void onEvent(AdminEvent adminEvent, boolean b) {
  }

  @Override
  public void close() {

  }
}
