package me.ihqqq.notbooster.storage;

import me.ihqqq.notbooster.booster.Booster;

import java.util.Collection;

public interface BoosterStorage {
    Collection<Booster> load();
    void save(Collection<Booster> boosters);
}
