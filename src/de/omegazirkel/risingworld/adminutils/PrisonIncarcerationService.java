package de.omegazirkel.risingworld.adminutils;

import de.omegazirkel.risingworld.AdminUtils;
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

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public class PrisonIncarcerationService {

    public static final String STATUS_INCARCERATED = "INCARCERATED";
    public static final String INVENTORY_FORMAT = "rw-inventory-v1";

    private final PrisonService prisonService;
    private final PrisonerService prisonerService;

    public PrisonIncarcerationService(PrisonService prisonService, PrisonerService prisonerService) {
        this.prisonService = prisonService;
        this.prisonerService = prisonerService;
    }

    public IncarcerationResult incarcerate(Player player, long sentenceTotalMs, boolean realtime, String reason) {
        if (player == null) {
            return IncarcerationResult.failed(Status.INVALID_PLAYER, null, null);
        }
        if (sentenceTotalMs <= 0) {
            return IncarcerationResult.failed(Status.INVALID_SENTENCE, null, null);
        }

        Prisoner existing = prisonerService.get(player.getDbID());
        if (existing != null && isBlockingExistingSentence(existing)) {
            return IncarcerationResult.failed(Status.ALREADY_INCARCERATED, null, existing);
        }

        Vector3f playerPosition = copy(player.getPosition());
        Optional<Prison> nearest = nearestEnabledPrison(playerPosition);
        if (nearest.isEmpty()) {
            return IncarcerationResult.failed(Status.NO_PRISON_AVAILABLE, null, null);
        }

        Prison prison = nearest.get();
        Area prisonArea = Server.getArea(prison.areaId);
        if (prisonArea == null) {
            return IncarcerationResult.failed(Status.PRISON_AREA_MISSING, prison, null);
        }

        long now = System.currentTimeMillis();
        Vector3f releasePosition = releasePosition(player, playerPosition);
        byte[] inventoryBlob = serializeInventory(player);
        Prisoner prisoner = new Prisoner(
                player.getDbID(),
                player.getUID(),
                player.getName(),
                prison.areaId,
                sentenceTotalMs,
                0L,
                now,
                realtime,
                releasePosition.x,
                releasePosition.y,
                releasePosition.z,
                inventoryBlob,
                INVENTORY_FORMAT,
                false,
                0,
                0L,
                STATUS_INCARCERATED,
                now,
                now,
                now,
                null,
                reason);

        prisonerService.incarcerate(prisoner);
        prisonArea.setPlayerPermission(player, AdminUtils.PRISONER_AREA_PERMISSION);
        Vector3f prisonSpawn = copy(prison.spawnPosition);
        stabilizePlayerForTransfer(player);
        player.setSpawnPoint(SpawnPointType.Primary, prisonSpawn, Quaternion.IDENTITY, prison.name);
        player.setPosition(prisonSpawn);
        stabilizePlayerForTransfer(player);
        clearInventory(player);

        prison.currentInmates = Math.max(0, prison.currentInmates) + 1;
        prison.totalInmatesLifetime++;
        prison.totalSentenceMs += sentenceTotalMs;
        prisonService.markDirty(prison);

        return IncarcerationResult.success(prison, prisoner);
    }

    public Optional<Prison> nearestEnabledPrison(Vector3f position) {
        if (position == null) {
            return Optional.empty();
        }
        return prisonService.getAll().stream()
                .filter(PrisonIncarcerationService::isUsablePrison)
                .min(Comparator.comparingDouble((Prison prison) -> position.distanceSquared(prison.spawnPosition))
                        .thenComparingLong(prison -> prison.areaId));
    }

    private static boolean isUsablePrison(Prison prison) {
        return prison != null && prison.enabled && prison.spawnPosition != null;
    }

    private static boolean isBlockingExistingSentence(Prisoner prisoner) {
        if (prisoner.restorePending) {
            return true;
        }
        return !Objects.equals("RELEASED", prisoner.status);
    }

    private static Vector3f releasePosition(Player player, Vector3f fallback) {
        Vector3f spawn = player.getSpawnPosition(SpawnPointType.Primary);
        return spawn == null ? copy(fallback) : copy(spawn);
    }

    private static byte[] serializeInventory(Player player) {
        Inventory inventory = player.getInventory();
        return inventory == null ? null : inventory.serialize();
    }

    private static void clearInventory(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }
        inventory.clear();
        inventory.syncWithClient();
    }

    private static void stabilizePlayerForTransfer(Player player) {
        player.setBleeding(false);
        player.setBrokenBones(false);
        player.setHealth(Math.max(1, player.getMaxHealth()));
    }

    private static Vector3f copy(Vector3f vector) {
        return vector == null ? null : new Vector3f(vector.x, vector.y, vector.z);
    }

    public enum Status {
        SUCCESS,
        INVALID_PLAYER,
        INVALID_SENTENCE,
        ALREADY_INCARCERATED,
        NO_PRISON_AVAILABLE,
        PRISON_AREA_MISSING
    }

    public static final class IncarcerationResult {
        public final Status status;
        public final Prison prison;
        public final Prisoner prisoner;

        private IncarcerationResult(Status status, Prison prison, Prisoner prisoner) {
            this.status = status;
            this.prison = prison;
            this.prisoner = prisoner;
        }

        public boolean success() {
            return status == Status.SUCCESS;
        }

        public static IncarcerationResult success(Prison prison, Prisoner prisoner) {
            return new IncarcerationResult(Status.SUCCESS, prison, prisoner);
        }

        public static IncarcerationResult failed(Status status, Prison prison, Prisoner prisoner) {
            return new IncarcerationResult(status, prison, prisoner);
        }
    }
}
