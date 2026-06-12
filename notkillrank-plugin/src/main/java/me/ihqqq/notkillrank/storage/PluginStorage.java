package me.ihqqq.notkillrank.storage;

import java.util.List;

public interface PluginStorage {

    PlayerData load(String uuid);

    PlayerData loadByName(String name);

    void save(PlayerData data);

    List<PlayerData> loadAll();

    void close();
}
