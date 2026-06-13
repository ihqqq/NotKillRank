package me.ihqqq.notkillrank.webhook;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public class SkinsRestorerHook {

    public static String getTextureUrl(UUID uuid, String name) {
        try {
            Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
            Object api = providerClass.getMethod("get").invoke(null);
            if (api == null) return null;

            Object playerStorage = api.getClass().getMethod("getPlayerStorage").invoke(api);
            if (playerStorage == null) return null;

            Optional<?> optSkin = invokeOptional(playerStorage, "getSkinOfPlayer",
                    new Class[]{UUID.class}, new Object[]{uuid});

            if (optSkin == null || optSkin.isEmpty()) {
                optSkin = invokeOptional(playerStorage, "getSkinForPlayer",
                        new Class[]{UUID.class, String.class}, new Object[]{uuid, name});
            }

            if (optSkin == null || optSkin.isEmpty()) return null;

            Object skinProperty = optSkin.get();
            Method getValueMethod = findMethod(skinProperty.getClass(), "getValue");
            if (getValueMethod == null) return null;
            String base64Value = (String) getValueMethod.invoke(skinProperty);

            return decodeTextureUrl(base64Value);

        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<?> invokeOptional(Object target, String methodName,
                                              Class<?>[] paramTypes, Object[] args) {
        try {
            Method m = findMethod(target.getClass(), methodName, paramTypes);
            if (m == null) return null;
            return (Optional<?>) m.invoke(target, args);
        } catch (Exception e) {
            return null;
        }
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        // Tìm trong class hierarchy
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method m = current.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
            // Tìm trong interfaces
            for (Class<?> iface : current.getInterfaces()) {
                Method m = findMethod(iface, name, paramTypes);
                if (m != null) return m;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static String decodeTextureUrl(String base64Value) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Value);
            String json = new String(decoded, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject textures = root.getAsJsonObject("textures");
            if (textures == null) return null;
            JsonObject skin = textures.getAsJsonObject("SKIN");
            if (skin == null) return null;
            return skin.get("url").getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}
