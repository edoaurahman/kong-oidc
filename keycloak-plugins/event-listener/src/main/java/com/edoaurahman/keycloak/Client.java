package com.edoaurahman.keycloak;

import com.google.gson.JsonObject;
import org.jboss.logging.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Client {
    private static final Logger log = Logger.getLogger(Client.class);
    private static final String WEBHOOK_URL = "WEBHOOK_URL";
    private static final String LOG_ACTIVITY_KEY = "LOG_ACTIVITY_KEY";

    public static void postService(String data) throws IOException {
        try {
            final String urlString = System.getenv(WEBHOOK_URL);
            log.infof("WEBHOOK_URL: %s", urlString);
            final String logActivityKey = System.getenv(LOG_ACTIVITY_KEY);

            if (urlString == null || urlString.isEmpty()) {
                throw new IllegalArgumentException("WEBHOOK_URL environment variable not set.");
            }

            URL url = URI.create(urlString).toURL();
            HttpURLConnection conn = getHttpURLConnection(data, url, logActivityKey);

            final int responseCode = conn.getResponseCode();
            InputStream stream = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                log.infof("HTTP %d: %s", responseCode, response.toString());
            }

            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
                throw new RuntimeException("HTTP request failed: " + responseCode);
            }

            conn.disconnect();
        } catch (IOException e) {
            throw new IOException("Post failed: " + e.getMessage(), e);
        }
    }

    private static HttpURLConnection getHttpURLConnection(String data, URL url, String logActivityKey) throws IOException {
        String query = "?" + data;
        URL urlWithQuery = new URL(url.toString() + query);

        HttpURLConnection conn = (HttpURLConnection) urlWithQuery.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        if (logActivityKey != null) {
            conn.setRequestProperty("Log-Activity-Key", logActivityKey);
        }
        return conn;
    }
}
