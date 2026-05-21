package de.omegazirkel.risingworld.adminutils;

import de.omegazirkel.risingworld.adminutils.db.PrisonService;
import de.omegazirkel.risingworld.adminutils.db.PrisonerService;
import de.omegazirkel.risingworld.adminutils.db.entities.Prison;
import de.omegazirkel.risingworld.adminutils.db.entities.Prisoner;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Inventory;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Quaternion;
import net.risingworld.api.utils.SpawnPointType;
import net.risingworld.api.utils.Vector3f;

public class PrisonReleaseService {

    public static final String REASON_SENTENCE_COMPLETE = "SENTENCE_COMPLETE";

    private final PrisonService prisonService;
    private final PrisonerService prisonerService;

    public PrisonReleaseService(PrisonService prisonService, PrisonerService prisonerService) {
        this.prisonService = prisonService;
        this.prisonerService = prisonerService;
    }

    public ReleaseResult release(Player player, String reason) {
        if (player == null) {
            return ReleaseResult.failed(Status.INVALID_PLAYER, null, null);
        }
        Prisoner prisoner = prisonerService.get(player.getDbID());
        if (prisoner == null) {
            return ReleaseResult.failed(Status.NOT_PRISONER, null, null);
        }
        return release(player, prisoner, reason);
    }

    public ReleaseResult release(Player player, Prisoner prisoner, String reason) {
        if (player == null) {
            return ReleaseResult.failed(Status.INVALID_PLAYER, null, prisoner);
        }
        if (prisoner == null) {
            return ReleaseResult.failed(Status.NOT_PRISONER, null, null);
        }
        if (isReleased(prisoner) && !prisoner.restorePending) {
            return ReleaseResult.failed(Status.ALREADY_RELEASED, null, prisoner);
        }
        if (!isReleased(prisoner)) {
            prisonerService.markReleasePending(prisoner, reason, System.currentTimeMillis());
        }
        return restore(player, prisoner);
    }

    public ReleaseResult releaseIfDue(Player player) {
        if (player == null) {
            return ReleaseResult.failed(Status.INVALID_PLAYER, null, null);
        }
        Prisoner prisoner = prisonerService.get(player.getDbID());
        if (prisoner == null) {
            return ReleaseResult.failed(Status.NOT_PRISONER, null, null);
        }
        if (prisoner.restorePending) {
            return restore(player, prisoner);
        }
        if (isReleased(prisoner)) {
            return ReleaseResult.failed(Status.ALREADY_RELEASED, null, prisoner);
        }
        if (prisoner.realtime && realtimeSentenceComplete(prisoner, System.currentTimeMillis())) {
            return release(player, prisoner, REASON_SENTENCE_COMPLETE);
        }
        return ReleaseResult.failed(Status.NOT_DUE, null, prisoner);
    }

    public boolean realtimeSentenceComplete(Prisoner prisoner, long now) {
        return prisoner != null
                && prisoner.realtime
                && now - prisoner.sentenceStartTs >= prisoner.sentenceTotalMs;
    }

    private ReleaseResult restore(Player player, Prisoner prisoner) {
        Vector3f releasePosition = releasePosition(prisoner, player.getPosition());
        Status inventoryStatus = restoreInventory(player, prisoner);
        if (inventoryStatus != Status.SUCCESS) {
            return ReleaseResult.failed(inventoryStatus, null, prisoner);
        }

        Area area = Server.getArea(prisoner.prisonAreaId);
        if (area != null) {
            area.removePlayerPermission(player);
        }
        player.setSpawnPoint(SpawnPointType.Primary, releasePosition, Quaternion.IDENTITY, "Release");
        player.setPosition(releasePosition);

        markPrisonStats(prisoner);
        prisonerService.markRestoreComplete(prisoner, System.currentTimeMillis());
        return ReleaseResult.success(prisonService.get(prisoner.prisonAreaId), prisoner);
    }

    private Status restoreInventory(Player player, Prisoner prisoner) {
        if (prisoner.inventoryBlob == null || prisoner.inventoryBlob.length == 0) {
            return Status.SUCCESS;
        }
        if (!PrisonIncarcerationService.INVENTORY_FORMAT.equals(prisoner.inventoryFormat)) {
            return Status.UNSUPPORTED_INVENTORY_FORMAT;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return Status.INVENTORY_UNAVAILABLE;
        }
        inventory.clear();
        boolean restored = inventory.deserialize(prisoner.inventoryBlob);
        inventory.syncWithClient();
        return restored ? Status.SUCCESS : Status.INVENTORY_RESTORE_FAILED;
    }

    private void markPrisonStats(Prisoner prisoner) {
        Prison prison = prisonService.get(prisoner.prisonAreaId);
        if (prison == null) {
            return;
        }
        prison.currentInmates = Math.max(0, prison.currentInmates - 1);
        prison.totalServedMs += Math.min(prisoner.sentenceTotalMs, Math.max(0, prisoner.sentenceServedMs));
        prisonService.markDirty(prison);
    }

    private static Vector3f releasePosition(Prisoner prisoner, Vector3f fallback) {
        if (prisoner.releaseX == null || prisoner.releaseY == null || prisoner.releaseZ == null) {
            return copy(fallback);
        }
        return new Vector3f(prisoner.releaseX, prisoner.releaseY, prisoner.releaseZ);
    }

    private static Vector3f copy(Vector3f vector) {
        return vector == null ? new Vector3f(0f, 0f, 0f) : new Vector3f(vector.x, vector.y, vector.z);
    }

    private static boolean isReleased(Prisoner prisoner) {
        return "RELEASED".equalsIgnoreCase(prisoner.status);
    }

    public enum Status {
        SUCCESS,
        INVALID_PLAYER,
        NOT_PRISONER,
        NOT_DUE,
        ALREADY_RELEASED,
        INVENTORY_UNAVAILABLE,
        UNSUPPORTED_INVENTORY_FORMAT,
        INVENTORY_RESTORE_FAILED
    }

    public static final class ReleaseResult {
        public final Status status;
        public final Prison prison;
        public final Prisoner prisoner;

        private ReleaseResult(Status status, Prison prison, Prisoner prisoner) {
            this.status = status;
            this.prison = prison;
            this.prisoner = prisoner;
        }

        public boolean success() {
            return status == Status.SUCCESS;
        }

        public static ReleaseResult success(Prison prison, Prisoner prisoner) {
            return new ReleaseResult(Status.SUCCESS, prison, prisoner);
        }

        public static ReleaseResult failed(Status status, Prison prison, Prisoner prisoner) {
            return new ReleaseResult(status, prison, prisoner);
        }
    }
}
