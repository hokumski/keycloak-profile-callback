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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:hokum@dived.me">Andrey Kotov</a>
 */
public class ProfileCallbackEventListenerProviderFactory  implements EventListenerProviderFactory {

  public static final String ID = "profile-callback";
  static ArrayList<HashMap<String, Object>> callbacks = new ArrayList<>();

  @Override
  public EventListenerProvider create(KeycloakSession keycloakSession) {
    return new ProfileCallbackEventListenerProvider(keycloakSession, callbacks);
  }

  /**
   * Loads callback settings from keycloak config
   *
   * @param scope part of keycloak config with listener settings
   * @param postfix empty string or string with number, to seek in profile-callback config
   * @return HashMap with callback settings
   */
  private HashMap<String, Object> getCallbackSettings(Config.Scope scope, String postfix) {

    String callbackToURL = scope.get("callbackTo" + postfix, "");
    if (!callbackToURL.equals("")) {
      HashMap<String, Object> result = new HashMap<>();
      try {
        String callbackTo = new URI(callbackToURL).toString();
        result.put("url", callbackTo);
      } catch (URISyntaxException ignored) {
        System.out.println("Error: malformed URL for profile-callback");
        return null;
      }
      int timeout = scope.getInt("timeout" + postfix, -1);
      if (timeout > 0) {
        result.put("timeout", timeout);
      }
      String authHeaderName = scope.get("authHeaderName" + postfix, "");
      String authHeaderValue = scope.get("authHeaderValue" + postfix, "");
      if (!authHeaderName.equals("")) { // no need to check value, as empty string could be legal value
        result.put("authHeaderName", authHeaderName);
        result.put("authHeaderValue", authHeaderValue);
      }
      return result;
    }
    return null;
  }

  /**
   * Loading all callback parameters from scope config
   * @param scope event listener provider config
   */
  @Override
  public void init(Config.Scope scope) {
    HashMap<String, Object> simpleConfig = getCallbackSettings(scope, "");
    if (simpleConfig != null) {
      callbacks.add(simpleConfig);
    } else {
      // iterating until have some
      for (int i = 1; i<=10; i++) {
        HashMap<String, Object> positionalConfig = getCallbackSettings(scope, Integer.toString(i));
        if (positionalConfig != null) {
          callbacks.add(positionalConfig);
        } else {
          break;
        }
      }
    }
  }

  @Override
  public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

  }

  @Override
  public void close() {

  }

  @Override
  public String getId() {
    return ID;
  }
}
