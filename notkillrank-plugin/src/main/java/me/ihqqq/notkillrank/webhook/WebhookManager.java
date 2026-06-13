package me.ihqqq.notkillrank.webhook;

import com.google.gson.Gson;
import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.file.module.WebhookFile;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WebhookManager {

    private static final Gson GSON = new Gson();
    private static WebhookManager instance;

    public static WebhookManager getInstance() {
        return instance;
    }

    public WebhookManager() {
        instance = this;
    }

    public void sendStats(Map<String, String> replacements) {
        var cfg = WebhookFile.get();
        if (cfg == null) return;
        if (!cfg.getBoolean("stats.enabled", false)) return;

        String url = cfg.getString("stats.url", "");
        if (url == null || url.isBlank() || url.equals("https://discord.com/api/webhooks/YOUR_WEBHOOK_URL")) return;

        ConfigurationSection payloadSection = cfg.getConfigurationSection("stats.payload");
        if (payloadSection == null) return;

        Map<String, Object> payload = sectionToMap(payloadSection);

        NotKillRank.plugin.getServer().getScheduler().runTaskAsynchronously(NotKillRank.plugin, () -> {
            try {
                Map<String, Object> processed = applyReplacements(payload, replacements);
                stripEmptyUrlObjects(processed);
                String json = GSON.toJson(processed);
                post(url, json);
            } catch (Exception e) {
                MessageUtil.warn("[Webhook] Gửi stats webhook thất bại: " + e.getMessage());
            }
        });
    }

    private Map<String, Object> sectionToMap(ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object val = section.get(key);
            if (val instanceof ConfigurationSection nested) {
                map.put(key, sectionToMap(nested));
            } else if (val instanceof List<?> list) {
                map.put(key, convertList(list));
            } else {
                map.put(key, val);
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private List<Object> convertList(List<?> list) {
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof ConfigurationSection section) {
                result.add(sectionToMap(section));
            } else if (item instanceof Map<?, ?> mapItem) {
                result.add(convertMapRecursive((Map<String, Object>) mapItem));
            } else if (item instanceof List<?> nested) {
                result.add(convertList(nested));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertMapRecursive(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Map<?, ?> nested) {
                result.put(entry.getKey(), convertMapRecursive((Map<String, Object>) nested));
            } else if (val instanceof List<?> list) {
                result.put(entry.getKey(), convertList(list));
            } else {
                result.put(entry.getKey(), val);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> applyReplacements(Map<String, Object> map, Map<String, String> replacements) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = applyReplacementsToString(entry.getKey(), replacements);
            Object val = entry.getValue();
            if (val instanceof String str) {
                result.put(key, applyReplacementsToString(str, replacements));
            } else if (val instanceof Map<?, ?> nested) {
                result.put(key, applyReplacements((Map<String, Object>) nested, replacements));
            } else if (val instanceof List<?> list) {
                result.put(key, applyReplacementsToList(list, replacements));
            } else {
                result.put(key, val);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Object> applyReplacementsToList(List<?> list, Map<String, String> replacements) {
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String str) {
                result.add(applyReplacementsToString(str, replacements));
            } else if (item instanceof Map<?, ?> map) {
                result.add(applyReplacements((Map<String, Object>) map, replacements));
            } else if (item instanceof List<?> nested) {
                result.add(applyReplacementsToList(nested, replacements));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void stripEmptyUrlObjects(Map<String, Object> map) {
        map.entrySet().removeIf(entry -> {
            Object val = entry.getValue();
            if (val instanceof Map<?, ?> nested) {
                Map<String, Object> nestedMap = (Map<String, Object>) nested;
                Object urlVal = nestedMap.get("url");
                if (urlVal instanceof String s && s.isBlank()) return true;
                stripEmptyUrlObjects(nestedMap);
            } else if (val instanceof List<?> list) {
                stripEmptyUrlObjectsInList((List<Object>) list);
            }
            return false;
        });
    }

    @SuppressWarnings("unchecked")
    private void stripEmptyUrlObjectsInList(List<Object> list) {
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                stripEmptyUrlObjects((Map<String, Object>) map);
            }
        }
    }

    private String applyReplacementsToString(String text, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }

    private void post(String urlStr, String json) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "NotKillRank-Webhook/1.0");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            MessageUtil.warn("[Webhook] Server trả về HTTP " + status + " khi gửi webhook.");
        }
        conn.disconnect();
    }
}
