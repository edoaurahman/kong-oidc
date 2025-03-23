package com.example.mfa.client;

import com.example.mfa.config.MFAConfig;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class WhatsAppClient {
    private static final Logger logger = Logger.getLogger(WhatsAppClient.class);
    private final MFAConfig config;
    private final HttpClient httpClient;

    public WhatsAppClient(MFAConfig config) {
        this.config = config;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }


    public void sendMessage(String phoneNumber, String message) throws Exception {
        // Send message using WhatsApp API
        try {
            String url = String.format(config.getWhatsAppEndpoint());
            logger.info("Sending message to " + phoneNumber + " via WhatsApp API: " + url);

            String jsonBody = String.format(
                    "{\"schema\": \"NUMBER\", \"receiver\": \"%s\",\"message\": {\"text\": \"%s\"}}",
                    phoneNumber, message
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("X-WhatsApp-Token", config.getWhatsAppXAppToken())
                    .header("X-WhatsApp-Key", config.getWhatsAppXAppKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Failed to send Telegram message. Status: " + response.statusCode());
                throw new RuntimeException("Failed to send Telegram message");
            }
            logger.info("Json BODY:" + jsonBody);

            logger.info("WhatsApp message sent successfully to " + phoneNumber);
        } catch (Exception e) {
            logger.error("Failed to send message to " + phoneNumber, e);
            throw e;
        }
    }
}
