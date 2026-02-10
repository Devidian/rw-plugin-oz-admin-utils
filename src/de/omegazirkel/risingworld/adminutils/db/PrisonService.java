package de.omegazirkel.risingworld.adminutils.db;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import de.omegazirkel.risingworld.adminutils.db.entities.Prison;

public class PrisonService {

    private final PrisonStore store;
    private final ConcurrentHashMap<Long, Prison> byAreaId = new ConcurrentHashMap<>();

    public PrisonService(PrisonStore store) {
        this.store = store;
        rebuildIndexes();
    }

    public Prison get(long areaId) {
        return byAreaId.get(areaId);
    }

    public List<Prison> getAll() {
        return new ArrayList<>(byAreaId.values());
    }

    public Prison createIfAbsent(long areaId, Prison prison) {
        Prison existing = store.get(new PrisonKey(areaId));
        if (existing != null) {
            return existing;
        }

        store.put(new PrisonKey(areaId), prison);
        index(prison);
        return prison;
    }

    public void markDirty(Prison prison) {
        store.markDirty(prison);
    }

    public void rebuildIndexes() {
        byAreaId.clear();
        store.clear();
        store.loadAll();

        for (Prison p : store.values()) {
            index(p);
        }
    }

    private void index(Prison prison) {
        byAreaId.put(prison.areaId, prison);
    }
}
