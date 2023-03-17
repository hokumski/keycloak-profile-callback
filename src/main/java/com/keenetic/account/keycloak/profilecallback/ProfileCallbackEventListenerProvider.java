/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.keenetic.account.keycloak.profilecallback;

import java.io.IOException;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
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


/**
 * @author <a href="mailto:hokum@dived.me">Andrey Kotov</a>
 */
public class ProfileCallbackEventListenerProvider  implements EventListenerProvider {

  private KeycloakSession session;
  private JsonFactory jsonFactory;
  private ArrayList<HashMap<String, Object>> callbacks;
  protected Logger logger;

  ProfileCallbackEventListenerProvider(
          KeycloakSession session,
          Logger logger,
          ArrayList<HashMap<String, Object>> callbacks) {
    this.callbacks = callbacks;
    this.session = session;
    this.jsonFactory = new JsonFactory();
    this.logger = logger;
  }

  @Override
  public void onEvent(Event event) {

    switch (event.getType()) {
      case CUSTOM_REQUIRED_ACTION: {
        Map<String, String> eventDetails = event.getDetails();
        String customRequiredActionName = eventDetails.get("custom_required_action");
        // only 1 action now
        if (customRequiredActionName.equals("VERIFY_EMAIL_WITH_CODE")) {
          logger.debug("logged custom required action " + customRequiredActionName + " for " + event.getUserId());
          try {
            String userData = getUserInfo(event.getUserId(), customRequiredActionName, event.getDetails());
            String answer = postCallbacks(userData);
            if (!answer.equals("")) {
              logger.debug(answer);
            }
          } catch (IOException e) {
            logger.error("failed to callback for " + event.getType());
            logger.error(e);
          }
        }
        break;
      }
      case VERIFY_EMAIL:
      case DELETE_ACCOUNT:
      case UPDATE_PROFILE: {
        logger.debug("logged " + event.getType() + " for " + event.getUserId());
        try {
          String userData = getUserInfo(event.getUserId(), event.getType().toString(), event.getDetails());
          String answer = postCallbacks(userData);
          if (!answer.equals("")) {
            logger.debug(answer);
          }
        } catch (IOException e) {
          logger.error("failed to callback for " + event.getType());
          logger.error(e);
        }
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
  String getUserInfo(String userId, String eventType, Map<String,String> details) throws IOException {
    StringWriter jsonObjectWriter = new StringWriter();
    RealmModel realmModel = session.getContext().getRealm();
    UserModel userModel = session.users().getUserById(realmModel, userId);

    JsonGenerator generator = this.jsonFactory.createGenerator(jsonObjectWriter);
    generator.writeStartObject();

    generator.writeStringField("Type", eventType);
    generator.writeStringField("Id", userId);

    if (userModel == null) {
      // for DELETE_ACCOUNT, user is already deleted, so everything we can return,
      // is json with Id
      generator.writeStringField("IsUserMissing", "true");
    } else {

      Map<String, List<String>> userAttributes = userModel.getAttributes();
      generator.writeStringField("Date",
              java.time.ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")));
      generator.writeStringField("Email", userModel.getEmail());

      // Following NEW values could present in event details
      String firstName = userModel.getFirstName();
      if (details.containsKey("updated_first_name")) {
        firstName = details.get("updated_first_name");
      }

      String lastName = userModel.getLastName();
      if (details.containsKey("updated_last_name")) {
        lastName = details.get("updated_last_name");
      }

      String locale = "";
      if (userAttributes.containsKey("locale")) {
        locale = userAttributes.get("locale").get(0);
      }
      if (details.containsKey("updated_locale")) {
        locale = details.get("updated_locale");
      }

      String phone = "";
      if (userAttributes.containsKey("phone")) {
        phone = userAttributes.get("phone").get(0);
      }
      if (details.containsKey("updated_phone")) {
        phone = details.get("updated_phone");
      }

      generator.writeStringField("FirstName", firstName);
      generator.writeStringField("LastName", lastName);
      if (!locale.equals("")) {
        generator.writeStringField("Locale", locale);
      }
      if (!phone.equals("")) {
        generator.writeStringField("Phone", phone);
      }
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
      logger.debug("callback to " + url);
      try {
        HttpPost post = new HttpPost(url);

        if (callback.containsKey("timeout")) {
          int timeout = (int) callback.get("timeout");
          final RequestConfig params =
              RequestConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout).build();
          post.setConfig(params);
        }
        if (callback.containsKey("authHeaderName") && callback.containsKey("authHeaderValue")) {
          post.addHeader((String)callback.get("authHeaderName"), (String)callback.get("authHeaderValue"));
        }
        post.addHeader("content-type", "application/json; charset=utf-8");

        // send a JSON data
        post.setEntity(new StringEntity(payload, "UTF-8"));
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(post)) {
          String responseEntity = EntityUtils.toString(response.getEntity());
          if (responseEntity.equals("")) {
            responseEntity = "[empty response]";
          }
          sb.append(responseEntity);
          sb.append("\n");
        }
      } catch (UnknownHostException ignored) {
        sb.append("unknown host: ");
        sb.append(url);
        sb.append("\n");
        logger.error("callback to " + url + " failed: UnknownHostException");
      } catch (ConnectTimeoutException ignored) {
        sb.append("connection timeout for: ");
        sb.append(url);
        sb.append("\n");
        logger.error("callback to " + url + " failed: ConnectTimeoutException");
      } catch (Exception ignored) {
        sb.append("unknown error for: ");
        sb.append(url);
        sb.append("\n");
        logger.error("callback to " + url + " failed: Exception");
      }
    }
    return sb.toString();
  }

  @Override
  public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {

    // Idea: making callbacks from Admin API looks like callbacks, generated from User Account.
    // For now: converting DELETE of resource type USER to DELETE_ACCOUNT event

    if (Objects.requireNonNull(adminEvent.getResourceType()) == ResourceType.USER) {
      String[] resourcePathParts = adminEvent.getResourcePath().split("/");
      if (resourcePathParts.length == 2) {
        String userId = resourcePathParts[1];

        // Checking if second part of resource path is UUID
        try {
          UUID u = UUID.fromString(userId);
        } catch (IllegalArgumentException ignored) {
          logger.error(userId + " from resourcePath is not UUID");
          return;
        }

        if (Objects.requireNonNull(adminEvent.getOperationType()) == OperationType.DELETE) {
          try {
            logger.debug("logged admin event DELETE on USER for " + userId);
            String userData = getUserInfo(userId, "DELETE_ACCOUNT", null);
            String answer = postCallbacks(userData);
            if (!answer.equals("")) {
              logger.debug(answer);
            }
          } catch (IOException ignored) {
          }
        }

      }
    }
  }

  @Override
  public void close() {

  }
}
