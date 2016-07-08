package com.zpaslab.reporting;


import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.protobuf.TextFormat;
import com.googlecode.protobuf.format.HtmlFormat;
import com.googlecode.protobuf.format.JsonFormat;
import com.zpaslab.lockerbox.LockerboxProtos;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Properties;
import java.util.regex.Pattern;


// TODO(pawelb): props file (oauth server, root ca, client_id, client_secret)

public class PaymentsMain {

    static final Pattern getPayments = Pattern.compile("/payment.*");
    static final Pattern getPaymentWithCharges = Pattern.compile("/payment/[a-zA-Z0-9]+/charge");
    static final Pattern getParcel = Pattern.compile("/parcel/.*");

    public static void main(String[] args) {
        try {
            Config config = readConfig();
            addRootCA(config.ROOT_CA);
            String accessToken = requestAccessToken(config.OAUTH_SERVER, config.CLIENT_ID, config.CLIENT_SECRET);
            executeApiRequest(args, config.API_SERVER, accessToken);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Config {
        // OAuth server location; will be used to obtain access token.
        String OAUTH_SERVER;
        // OAuth client and secret. Both will be used to obtain access token from OAuth server via
        // client credentials grant: https://tools.ietf.org/html/rfc6749#section-4.4
        String CLIENT_ID;
        String CLIENT_SECRET;

        // Alternative Root CA (Certification authority) to be used in TLS connections.
        // Might be needed due to problems with Let's Encrypt certificates in Java.
        String ROOT_CA;

        // Api Server address; will be used to query for system data.
        String API_SERVER;
    }

    public static Config readConfig() throws Exception {
        Properties prop = new Properties();
        String propFileName = "config.properties";

        InputStream inputStream = new FileInputStream(propFileName);
        if (inputStream != null) {
            prop.load(inputStream);
        } else {
            throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
        }
        Config config = new Config();
        config.OAUTH_SERVER = prop.getProperty("oauth-server");
        config.CLIENT_ID = prop.getProperty("oauth-client");
        config.CLIENT_SECRET = prop.getProperty("oauth-secret");
        config.ROOT_CA = prop.getProperty("root-ca");

        config.API_SERVER = prop.getProperty("api-server");
        return config;
    }

    // Adding Root CA is required for some versions of Java:
    // https://community.letsencrypt.org/t/will-the-cross-root-cover-trust-by-the-default-list-in-the-jdk-jre/134
    public static void addRootCA(String rootCA) throws Exception {
        // if rootCA is empty, let's use system default certificates.
        if (rootCA.equals("")) {
            return;
        }
        InputStream fis = new BufferedInputStream(new FileInputStream(rootCA));
        Certificate ca = CertificateFactory.getInstance("X.509").generateCertificate(fis);

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry(Integer.toString(1), ca);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);

        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());

    }

    static String requestAccessToken(String oauthServer, String oauthClientId, String oauthClientSecret) throws IOException {
        try {
            TokenResponse response =
                    new ClientCredentialsTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                            new GenericUrl(oauthServer))
                            .setClientAuthentication(
                                    new BasicAuthentication(oauthClientId, oauthClientSecret)).execute();
            System.out.println("Access token: " + response.getAccessToken());
            return response.getAccessToken();
        } catch (TokenResponseException e) {
            if (e.getDetails() != null) {
                System.err.println("Error: " + e.getDetails().getError());
                if (e.getDetails().getErrorDescription() != null) {
                    System.err.println(e.getDetails().getErrorDescription());
                }
                if (e.getDetails().getErrorUri() != null) {
                    System.err.println(e.getDetails().getErrorUri());
                }
            } else {
                System.err.println(e.getMessage());
            }
            throw e;
        }
    }

    static void executeApiRequest(String[] args, String apiServer, String accessToken) throws IOException {
        if (args.length != 1) {
            System.out.println("\nInvalid number of args (expected 1)\n\nUsage:\n\t./gradlew <api-url>\n");
            System.exit(1);
        }
        if (getPaymentWithCharges.matcher(args[0]).find()) {
            System.out.printf("\n---------------------------\nget payments with charges [%s]\n\n", args[0]);

            String url = apiServer.concat(args[0]);
            HttpResponse response = executeGet(new NetHttpTransport(), new JacksonFactory(), accessToken,
                    new GenericUrl(url));
            InputStream responseContent = response.getContent();

            LockerboxProtos.Charges.Builder chargesBuilder = LockerboxProtos.Charges.newBuilder();
            JsonFormat jsonFormat = new JsonFormat();
            jsonFormat.merge(responseContent, chargesBuilder);

            System.out.println(TextFormat.printToUnicodeString(chargesBuilder));
            return;
        }
        if (getPayments.matcher(args[0]).find()) {
            System.out.printf("\n---------------------------\nget payments [%s]\n\n", args[0]);

            String url = apiServer.concat(args[0]);
            HttpResponse response = executeGet(new NetHttpTransport(), new JacksonFactory(), accessToken,
                    new GenericUrl(url));
            InputStream responseContent = response.getContent();

            LockerboxProtos.PaymentList.Builder paymentsList = LockerboxProtos.PaymentList.newBuilder();
            JsonFormat jsonFormat = new JsonFormat();
            jsonFormat.merge(responseContent, paymentsList);

            System.out.println(TextFormat.printToUnicodeString(paymentsList));

            return;
        }
        if (getParcel.matcher(args[0]).find()) {
            System.out.printf("\n---------------------------\nget parcel [%s]\n\n", args[0]);

            String url = apiServer.concat(args[0]);
            HttpResponse response = executeGet(new NetHttpTransport(), new JacksonFactory(), accessToken,
                    new GenericUrl(url));
            InputStream responseContent = response.getContent();

            LockerboxProtos.Parcel.Builder parcel = LockerboxProtos.Parcel.newBuilder();
            JsonFormat jsonFormat = new JsonFormat();
            jsonFormat.merge(responseContent, parcel);

            System.out.println(TextFormat.printToUnicodeString(parcel));
            return;
        }

        System.out.printf("\nInvalid API operation: %s\n", args[0]);
        System.exit(1);
    }

    static HttpResponse executeGet(
            HttpTransport transport, JsonFactory jsonFactory, String accessToken, GenericUrl url)
            throws IOException {
        Credential credential =
                new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken);
        HttpRequestFactory requestFactory = transport.createRequestFactory(credential);
        return requestFactory.buildGetRequest(url).execute();
    }

}
