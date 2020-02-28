Add to Keycloak config, to server -> profile section 

```xml
<subsystem xmlns="urn:jboss:domain:keycloak-server:1.1">
...
<spi name="eventsListener">
  <provider name="profile-callback" enabled="true">
    <properties>
      <property name="callbackTo" value="https://blabla.com/post" />
    </properties>
  </provider>
</spi>
...
</subsystem>
```
