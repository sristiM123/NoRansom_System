package com.ransomguard.agent.client;

import com.ransomguard.agent.model.AgentEvent;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class ControllerClient {

    private static final String CONTROLLER_URL = "http://127.0.0.1:9004/ingest";

    public static void send(AgentEvent e) {
        // ✅ This line MUST print every time send() is called
        System.out.println("ControllerClient.send() CALLED -> " + CONTROLLER_URL + " eventType=" + e.eventType);

        try {
            String json = toJson(e);
            System.out.println("Payload JSON -> " + json);

            HttpURLConnection con = (HttpURLConnection) URI.create(CONTROLLER_URL).toURL().openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = con.getResponseCode();

            InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
            String body = "";
            if (is != null) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                is.close();
            }

            // ✅ This is the line you wanted
            System.out.println("Sent to controller: HTTP " + code + " body=" + body);

        } catch (Exception ex) {
            // ✅ If controller is down you will see this
            System.out.println("Sent to controller: FAILED -> " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private static String toJson(AgentEvent e) {
        return "{"
                + "\"deviceId\":\"" + esc(e.deviceId) + "\","
                + "\"eventType\":\"" + esc(e.eventType) + "\","
                + "\"timestampMs\":" + e.timestampMs + ","
                + "\"severity\":" + e.severity + ","
                + "\"details\":\"" + esc(e.details) + "\""
                + "}";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
