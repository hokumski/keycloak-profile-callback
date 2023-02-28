# Keycloak-Profile-Callback

This is an event listener, posting JSON with userdata after specific events to configured endpoints. 
- UPDATE_PROFILE
- DELETE_ACCOUNT
- CUSTOM_REQUIRED_ACTION
  - VERIFY_EMAIL_WITH_CODE

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
    </properties>
  </provider>
</spi>
...
</subsystem>
```

There is an issue with KK18: it is not possible to get values from scope config of eventsListener 
(but it's OK for other types of SPI!). However, it is possible to iterate keys with scope.getPropertyNames().

To make it be configurable somehow, we've added another method of writing values. 

in keycloak.conf.
```yaml
spi-eventsListener-profile-callback-callbackTo1=https://blabla.com/post
spi-eventsListener-profile-callback-timeout1=1000

# compatible-mode
spi-eventsListener-profile-callback-callbackTo1-https(semicolon)//blabla.com/post
spi-eventsListener-profile-callback-timeout1-1000
```
----

**IMPORTANT**: Don't forget to enable listener in Realm Events -> Config
