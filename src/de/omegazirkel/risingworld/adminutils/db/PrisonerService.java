package de.omegazirkel.risingworld.adminutils.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.omegazirkel.risingworld.adminutils.db.entities.Prisoner;

public class PrisonerService {

    private final PrisonerStore store;

    private final ConcurrentHashMap<Integer, Prisoner> byPlayer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<Prisoner>> byPrison = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Prisoner>> byStatus = new ConcurrentHashMap<>();

    public PrisonerService(PrisonerStore store) {
        this.store = store;
        rebuildIndexes();
    }

    public Prisoner get(int playerDbId) {
        return byPlayer.get(playerDbId);
    }

    public List<Prisoner> getByPrison(long areaId) {
        Set<Prisoner> set = byPrison.get(areaId);
        return set == null ? List.of() : new ArrayList<>(set);
    }

    public List<Prisoner> getByStatus(String status) {
        Set<Prisoner> set = byStatus.get(status);
        return set == null ? List.of() : new ArrayList<>(set);
    }

    public Prisoner incarcerateIfAbsent(Prisoner prisoner) {

        Prisoner existing = store.get(new PrisonerKey(prisoner.playerDbId));
        if (existing != null) {
            return existing;
        }

        store.put(new PrisonerKey(prisoner.playerDbId), prisoner);
        index(prisoner);
        return prisoner;
    }

    public void markDirty(Prisoner prisoner) {
        store.markDirty(prisoner);
    }

    public void release(Prisoner prisoner) {
        store.remove(new PrisonerKey(prisoner.playerDbId));
        deindex(prisoner);
    }

    public void rebuildIndexes() {
        byPlayer.clear();
        byPrison.clear();
        byStatus.clear();

        store.clear();
        store.loadAll();

        for (Prisoner p : store.values()) {
            index(p);
        }
    }

    private void index(Prisoner p) {

        byPlayer.put(p.playerDbId, p);

        byPrison
                .computeIfAbsent(p.prisonAreaId, k -> ConcurrentHashMap.newKeySet())
                .add(p);

        byStatus
                .computeIfAbsent(p.status, k -> ConcurrentHashMap.newKeySet())
                .add(p);
    }

    private void deindex(Prisoner p) {

        byPlayer.remove(p.playerDbId);

        Set<Prisoner> prisonSet = byPrison.get(p.prisonAreaId);
        if (prisonSet != null) {
            prisonSet.remove(p);
        }

        Set<Prisoner> statusSet = byStatus.get(p.status);
        if (statusSet != null) {
            statusSet.remove(p);
        }
    }
}
