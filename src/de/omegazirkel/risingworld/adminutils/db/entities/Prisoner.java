package de.omegazirkel.risingworld.adminutils.db.entities;

public class Prisoner {

    /* --- identity --- */
    public final int playerDbId;
    public String playerUid;
    public String playerName;
    public final long prisonAreaId;

    /* --- sentence --- */
    public long sentenceTotalMs;
    public long sentenceServedMs;
    public long sentenceStartTs;
    public boolean realtime;

    /* --- release / restore --- */
    public Float releaseX;
    public Float releaseY;
    public Float releaseZ;
    public byte[] inventoryBlob;
    public String inventoryFormat;
    public boolean restorePending;

    /* --- labor --- */
    public int totalWorkDone;
    public long lastWorkTs;

    /* --- state --- */
    public String status;           // INCARCERATED, RELEASED, ESCAPED, ...
    public long lastSeenTs;
    public long createdAt;
    public long updatedAt;
    public Long releasedAt;
    public String releaseReason;

    public Prisoner(
            int playerDbId,
            String playerUid,
            String playerName,
            long prisonAreaId,
            long sentenceTotalMs,
            long sentenceServedMs,
            long sentenceStartTs,
            boolean realtime,
            Float releaseX,
            Float releaseY,
            Float releaseZ,
            byte[] inventoryBlob,
            String inventoryFormat,
            boolean restorePending,
            int totalWorkDone,
            long lastWorkTs,
            String status,
            long lastSeenTs,
            long createdAt,
            long updatedAt,
            Long releasedAt,
            String releaseReason
    ) {
        this.playerDbId = playerDbId;
        this.playerUid = playerUid;
        this.playerName = playerName;
        this.prisonAreaId = prisonAreaId;
        this.sentenceTotalMs = sentenceTotalMs;
        this.sentenceServedMs = sentenceServedMs;
        this.sentenceStartTs = sentenceStartTs;
        this.realtime = realtime;
        this.releaseX = releaseX;
        this.releaseY = releaseY;
        this.releaseZ = releaseZ;
        this.inventoryBlob = inventoryBlob;
        this.inventoryFormat = inventoryFormat;
        this.restorePending = restorePending;
        this.totalWorkDone = totalWorkDone;
        this.lastWorkTs = lastWorkTs;
        this.status = status;
        this.lastSeenTs = lastSeenTs;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.releasedAt = releasedAt;
        this.releaseReason = releaseReason;
    }

    /* --- helpers --- */

    public long getRemainingMs() {
        return Math.max(0, sentenceTotalMs - sentenceServedMs);
    }

    public boolean isCompleted() {
        return sentenceServedMs >= sentenceTotalMs;
    }
}
