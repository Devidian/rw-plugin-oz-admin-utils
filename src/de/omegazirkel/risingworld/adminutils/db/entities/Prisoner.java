package de.omegazirkel.risingworld.adminutils.db.entities;

public class Prisoner {

    /* --- identity --- */
    public final int playerDbId;
    public final long prisonAreaId;

    /* --- sentence --- */
    public long sentenceTotalMs;
    public long sentenceServedMs;
    public long sentenceStartTs;
    public boolean realtime;

    /* --- labor --- */
    public int totalWorkDone;
    public long lastWorkTs;

    /* --- state --- */
    public String status;           // INCARCERATED, RELEASED, ESCAPED, ...
    public long lastSeenTs;

    public Prisoner(
            int playerDbId,
            long prisonAreaId,
            long sentenceTotalMs,
            long sentenceServedMs,
            long sentenceStartTs,
            boolean realtime,
            int totalWorkDone,
            long lastWorkTs,
            String status,
            long lastSeenTs
    ) {
        this.playerDbId = playerDbId;
        this.prisonAreaId = prisonAreaId;
        this.sentenceTotalMs = sentenceTotalMs;
        this.sentenceServedMs = sentenceServedMs;
        this.sentenceStartTs = sentenceStartTs;
        this.realtime = realtime;
        this.totalWorkDone = totalWorkDone;
        this.lastWorkTs = lastWorkTs;
        this.status = status;
        this.lastSeenTs = lastSeenTs;
    }

    /* --- helpers --- */

    public long getRemainingMs() {
        return Math.max(0, sentenceTotalMs - sentenceServedMs);
    }

    public boolean isCompleted() {
        return sentenceServedMs >= sentenceTotalMs;
    }
}
