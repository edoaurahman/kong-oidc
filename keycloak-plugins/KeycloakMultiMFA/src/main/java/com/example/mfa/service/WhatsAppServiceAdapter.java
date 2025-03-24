package com.example.mfa.service;

import org.jboss.logging.Logger;
import com.example.mfa.config.MFAConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Adapter Pattern: Implementation untuk layanan WhatsApp
 * Singleton Pattern: Hanya satu instance per konfigurasi
 */
public class WhatsAppServiceAdapter implements ExternalServiceAdapter {
    private static final Logger logger = Logger.getLogger(WhatsAppServiceAdapter.class);

    // Konstanta kunci - harus sama persis dengan yang ada di konfigurasi
    private static final String KEY_API_KEY = "whatsAppApiKey";
    private static final String KEY_API_TOKEN = "whatsAppApiToken";
    private static final String KEY_API_URL = "whatsAppApiUrl";
    private static final String KEY_MESSAGE_TEMPLATE = "whatsAppMessageTemplate";

    private final String apiKey;
    private final String apiToken;
    private final String apiUrl;
    private final String messageTemplate;
    private final HttpClient httpClient;
    private static WhatsAppServiceAdapter instance;

    private WhatsAppServiceAdapter(MFAConfig config) {
        // Get the raw configuration map for direct access
        Map<String, String> rawConfig = config.getAllConfig();
        
        // Store configuration values
        this.apiKey = rawConfig.get(KEY_API_KEY);
        this.apiToken = rawConfig.get(KEY_API_TOKEN);
        this.apiUrl = rawConfig.get(KEY_API_URL);
        this.messageTemplate = rawConfig.getOrDefault(KEY_MESSAGE_TEMPLATE, 
                                "Kode verifikasi Anda adalah: {code}. Kode ini berlaku selama {expiry} menit.");
        
        // Initialize HTTP client
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        // Log configuration status
        logger.info("WhatsApp API Key: " + (apiKey != null ? "[SET]" : "null"));
        logger.info("WhatsApp API Token: " + (apiToken != null ? "[SET]" : "null"));
        logger.info("WhatsApp API URL: " + (apiUrl != null ? apiUrl : "null"));
        logger.info("WhatsApp isConfigured(): " + isConfigured());
    }


    /**
     * Mendapatkan instance singleton - reset instance untuk memastikan konfigurasi baru
     */
    public static synchronized WhatsAppServiceAdapter getInstance(MFAConfig config) {
        // Selalu membuat instance baru untuk memastikan konfigurasi terbaru
        instance = new WhatsAppServiceAdapter(config);
        return instance;
    }

    @Override
    public boolean isConfigured() {
        boolean configured = apiKey != null && !apiKey.isEmpty() && 
                            apiToken != null && !apiToken.isEmpty() && 
                            apiUrl != null && !apiUrl.isEmpty();
        
        if (!configured) {
            if (apiKey == null || apiKey.isEmpty())
                logger.info("Reason: WhatsApp API Key is missing");
            if (apiToken == null || apiToken.isEmpty())
                logger.info("Reason: WhatsApp API Token is missing");
            if (apiUrl == null || apiUrl.isEmpty())
                logger.info("Reason: WhatsApp API URL is missing");
        }
        
        return configured;
    }


    @Override
    public void sendVerificationCode(String phoneNumber, String code) throws Exception {
        if (!isConfigured()) {
            logger.error("WhatsApp API tidak dikonfigurasi dengan benar");
            throw new IllegalStateException("WhatsApp API tidak dikonfigurasi");
        }

        String message = createFormattedMessage(code);

        // Membuat body request
        String requestBody = String.format(
                "{\"phoneNumber\":\"%s\",\"message\":\"%s\"}",
                phoneNumber, message
        );

        // Membuat HTTP request dengan header yang diperbarui
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("X-APP-KEY", apiKey)
                .header("X-APP-TOKEN", apiToken)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("Kode verifikasi WhatsApp berhasil dikirim ke " + phoneNumber);
            } else {
                logger.error("Gagal mengirim kode verifikasi WhatsApp. Kode status: " + response.statusCode());
                logger.error("Response body: " + response.body());
                throw new Exception("Gagal mengirim kode verifikasi WhatsApp: " + response.body());
            }
        } catch (Exception e) {
            logger.error("Error saat mengirim kode verifikasi WhatsApp", e);
            throw new Exception("Gagal mengirim kode verifikasi WhatsApp", e);
        }
    }

    /**
     * Membuat pesan terformat menggunakan template yang disediakan
     */
    private String createFormattedMessage(String code) {
        String message;

        if (messageTemplate == null || messageTemplate.isEmpty()) {
            // Template default jika tidak disediakan
            message = String.format(
                    "Kode verifikasi anda adalah %s\n" +
                    "Berlaku selama 5 menit\n" +
                    "Demi keamanan, mohon untuk tidak membagikan kode ini dengan siapapun.",
                    code
            );
        } else {
            // Gunakan template yang disediakan
            LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss 'WIB' dd MMMM yyyy")
                    .withZone(ZoneId.of("Asia/Jakarta"));
            
            message = messageTemplate
                    .replace("{{code}}", code)
                    .replace("{{expiry_time}}", formatter.format(expiryTime))
                    .replace("\\n", "\n"); // Konversi \n dalam konfigurasi menjadi baris baru
        }

        // Escape karakter khusus JSON
        return message.replace("\"", "\\\"")
                     .replace("\n", "\\n");
    }

    @Override
    public boolean verifyCode(String phoneNumber, String code) {
        // WhatsApp tidak memiliki API verifikasi bawaan, jadi kita bergantung pada perbandingan kode manual
        // Ini ditangani oleh metode verifyCode AbstractMFAProvider
        return true;
    }
}