package me.ihqqq.notkillrank.api;

public final class NKRProvider {

    private static volatile NKRApi instance;

    private NKRProvider() {}

    public static void register(NKRApi api) {
        instance = api;
    }

    public static void unregister() {
        instance = null;
    }

    public static NKRApi get() {
        NKRApi api = instance;
        if (api == null) {
            throw new IllegalStateException(
                    "NotKillRank API chưa sẵn sàng. " +
                            "Đảm bảo NotKillRank đã load");
        }
        return api;
    }

    public static boolean isAvailable() {
        return instance != null;
    }
}
