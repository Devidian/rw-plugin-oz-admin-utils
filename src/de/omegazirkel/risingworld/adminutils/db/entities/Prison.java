package de.omegazirkel.risingworld.adminutils.db.entities;

import net.risingworld.api.utils.Vector3f;

public class Prison {

    public final long areaId;
    public String name;

    public Vector3f spawnPosition;

    public Long directorPlayerId; // nullable
    public boolean enabled;

    /* --- statistics --- */
    public long totalInmatesLifetime;
    public long totalWorkDone;
    public long totalSentenceMs;
    public long totalServedMs;

    public int currentInmates;

    public Prison(
            long areaId,
            String name,
            Vector3f spawnPosition,
            Long directorPlayerId,
            boolean enabled,
            long totalInmatesLifetime,
            long totalWorkDone,
            long totalSentenceMs,
            long totalServedMs,
            int currentInmates) {
        this.areaId = areaId;
        this.name = name;
        this.spawnPosition = spawnPosition;
        this.directorPlayerId = directorPlayerId;
        this.enabled = enabled;
        this.totalInmatesLifetime = totalInmatesLifetime;
        this.totalWorkDone = totalWorkDone;
        this.totalSentenceMs = totalSentenceMs;
        this.totalServedMs = totalServedMs;
        this.currentInmates = currentInmates;
    }
}
