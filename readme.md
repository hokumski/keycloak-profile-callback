# Keycloak-Profile-Callback

This is an event listener, posting JSON with userdata after 
*UPDATE_EMAIL* and *UPDATE_PROFILE* events to configured endpoints. 

----

Add to Keycloak config, to server -> profile section 

You can add several callbacks, use consecutive postfixes "1", "2" up to "10" to set request parameters for each callback.

If only one is needed, you can omit "1" postfix (like "callbackTo", "timeout", ...)

*callbackTo* is mandatory, timeout (in milliseconds) and authHeaders are optional.
```xml
<subsystem xmlns="urn:jboss:domain:keycloak-server:1.1">
...
<spi name="eventsListener">
  <provider name="profile-callback" enabled="true">
    <properties>
      <property name="callbackTo1" value="https://blabla.com/post" />
      <property name="timeout1" value="1000" />
      <property name="authHeaderName1" value="Authentication" />
      <property name="authHeaderValue1" value="Bearer of blabla" />
      ...
      <property name="enforceRequiredActionOnEmailChange" value="VERIFY_EMAIL_WITH_CODE" />
    </properties>
  </provider>
</spi>
...
</subsystem>
```

----

**IMPORTANT**: Don't forget to enable listener in Realm Events -> Config
