## Lockerbox Client - sample Java client application for Lockerbox/Pochtomat system

Application is a sample command line app that demonstrates how to query Lockerbox/Pochtomat API server
('Overmind') for data.
System uses OAuth 2.0 for authorization protocol. Before any data can be queried, client needs to get access token.

### Data schema
For data schema we use Google Protocol Buffers, version 2 (https://developers.google.com/protocol-buffers/).
Proto definition file: src/resources/proto/lockerbox.proto
Generated Java file: src/generated/main/java/com/zpaslab/lockerbox/LockerboxProtos.java

### Running the application
Configuration:
1. configure **config.properties** file

    ```
    # Authorization server settings
    oauth-server=https://localhost:8080/oauth/token
    oauth-client=
    oauth-secret=
    # SSL/TLS settings: Let's Encrypt root CA is not updated to Java's cert pool:
    # https://community.letsencrypt.org/t/will-the-cross-root-cover-trust-by-the-default-list-in-the-jdk-jre/134
    root-ca=
    ```
   (NOTE: please contact system admins/dev for testing/production server address & IDs)

2. Run application:

    ```
    Linux;
    # List all payments
    $> ./gradlew run -PappArgs="['/payment']"

    # List all payments which were created after and before specific dates
    $> ./gradlew run -PappArgs="['/payment?consolidation_start=2016-06-29T10:00:00&consolidation_end=2016-06-29T16:00:00']"

    # List details of payment eb056e53fe4db9438a1dad0c717e26 and it's associated charges
    $> ./gradlew run -PappArgs="['/payment/eb056e53fe4db9438a1dad0c717e26/charge']"
    ```

    ```
    Windows:
    $> ./gradlew.bat ...
    ```

