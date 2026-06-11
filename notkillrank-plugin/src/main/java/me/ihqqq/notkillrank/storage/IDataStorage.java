package me.ihqqq.notkillrank.storage;

import java.util.List;

public interface IDataStorage {

    PlayerData load(String uuid);

    void save(PlayerData data);

    List<PlayerData> loadAll();

    void close();
}
