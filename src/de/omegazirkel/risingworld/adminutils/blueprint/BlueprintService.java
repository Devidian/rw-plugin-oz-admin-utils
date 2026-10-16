package de.omegazirkel.risingworld.adminutils.blueprint;

import java.sql.SQLException;

import de.omegazirkel.risingworld.adminutils.DiscordConnect;
import de.omegazirkel.risingworld.adminutils.PluginSettings;
import de.omegazirkel.risingworld.adminutils.blueprint.BlueprintProvenanceStore.Signature;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.events.player.PlayerCreateBlueprintEvent;
import net.risingworld.api.events.player.world.PlayerPlaceBlueprintEvent;
import net.risingworld.api.objects.Player;

/** Creation audit and placement rules for world-local blueprints. */
public final class BlueprintService {
    private final BlueprintProvenanceStore store;
    private final PluginSettings settings;
    private final I18n i18n;
    private final OZLogger logger;

    public BlueprintService(BlueprintProvenanceStore store, PluginSettings settings, I18n i18n, OZLogger logger) {
        this.store = store;
        this.settings = settings;
        this.i18n = i18n;
        this.logger = logger;
    }

    public void onCreate(PlayerCreateBlueprintEvent event) {
        Player player = event.getPlayer();
        if (player == null || event.isCancelled()) return;
        Signature signature = new Signature(event.getConstructionCount(), event.getObjectCount(),
                event.getPlantCount(), event.getTerrainCount(), event.getConstructionsHash(),
                event.getObjectsHash(), event.getPlantsHash(), event.getTerrainHash());
        try {
            if (store == null) throw new SQLException("Blueprint provenance store unavailable");
            store.save(player.getDbID(), signature);
        } catch (SQLException ex) {
            logger.error("Could not save blueprint provenance for player " + player.getDbID() + ": " + ex.getMessage());
            if (settings.maxConstructionCountInExternalBlueprints >= 0) {
                event.setCancelled(true);
                showError(player, "tc.blueprint.provenance.database");
            }
            return;
        }
        if (settings.discordBlueprintCreateChannelId != 0) {
            String message = i18n.get("tc.event.blueprint.created", DiscordConnect.botLang())
                    .replace("PH_PLAYER", player.getName())
                    .replace("PH_DBID", Integer.toString(player.getDbID()))
                    .replace("PH_DETAILS", details(signature, event.getSize(), event.getPosition()));
            DiscordConnect.sendDiscordMessage(message, settings.discordBlueprintCreateChannelId, null,
                    settings.discordBlueprintCreateThreadTopic);
        }
    }

    public void onPlace(PlayerPlaceBlueprintEvent event) {
        Player player = event.getPlayer();
        if (player == null || event.isCancelled()) return;
        Signature signature = new Signature(event.getConstructionCount(), event.getObjectCount(),
                event.getPlantCount(), event.getTerrainCount(), event.getConstructionsHash(),
                event.getObjectsHash(), event.getPlantsHash(), event.getTerrainHash());
        boolean admin = player.isAdmin();
        if (decide(settings.maxConstructionCountInExternalBlueprints, settings.blueprintMinPlayHours,
                settings.blueprintAdminExempt, admin, player.getTotalPlayTime(), signature.constructionCount(), true)
                == Decision.PLAYTIME) {
            reject(event, player, Decision.PLAYTIME);
            reportPlace(event, player, signature, Decision.PLAYTIME);
            return;
        }
        boolean known = true;
        Decision decision;
        if (needsLookup(settings.maxConstructionCountInExternalBlueprints, settings.blueprintAdminExempt,
                admin, signature.constructionCount())) {
            try {
                if (store == null) throw new SQLException("Blueprint provenance store unavailable");
                known = store.contains(signature);
            } catch (SQLException ex) {
                logger.error("Could not check blueprint provenance for player " + player.getDbID() + ": " + ex.getMessage());
                decision = Decision.DATABASE;
                reject(event, player, decision);
                reportPlace(event, player, signature, decision);
                return;
            }
        }
        decision = decide(settings.maxConstructionCountInExternalBlueprints, settings.blueprintMinPlayHours,
                settings.blueprintAdminExempt, admin, player.getTotalPlayTime(), signature.constructionCount(), known);
        if (decision != Decision.ALLOWED) reject(event, player, decision);
        reportPlace(event, player, signature, decision);
    }

    private void reject(PlayerPlaceBlueprintEvent event, Player player, Decision decision) {
        event.setCancelled(true);
        String key = switch (decision) {
            case PLAYTIME -> "tc.blueprint.playtime.required";
            case EXTERNAL -> "tc.blueprint.provenance.external";
            case DATABASE -> "tc.blueprint.provenance.database";
            case ALLOWED -> throw new IllegalArgumentException("Cannot reject an allowed placement");
        };
        showError(player, key);
    }

    private void showError(Player player, String key) {
        player.showErrorMessageBox(i18n.get("tc.blueprint.error.title", player),
                i18n.get(key, player).replace("PH_HOURS", Integer.toString(settings.blueprintMinPlayHours)));
    }

    private void reportPlace(PlayerPlaceBlueprintEvent event, Player player, Signature signature, Decision decision) {
        if (settings.discordBlueprintPlaceChannelId == 0) return;
        String language = DiscordConnect.botLang();
        String result = i18n.get("tc.event.blueprint.result." + decision.name().toLowerCase(), language);
        String message = i18n.get("tc.event.blueprint.placed", language)
                .replace("PH_PLAYER", player.getName())
                .replace("PH_DBID", Integer.toString(player.getDbID()))
                .replace("PH_RESULT", result)
                .replace("PH_BLUEPRINT_ID", Long.toString(event.getBlueprintID()))
                .replace("PH_CREATOR", String.valueOf(event.getCreatorName()))
                .replace("PH_DETAILS", details(signature, event.getSize(), event.getPosition()));
        DiscordConnect.sendDiscordMessage(message, settings.discordBlueprintPlaceChannelId, null,
                settings.discordBlueprintPlaceThreadTopic);
    }

    private static String details(Signature signature, Object size, Object position) {
        return "C/O/P/T=" + signature.constructionCount() + "/" + signature.objectCount() + "/"
                + signature.plantCount() + "/" + signature.terrainCount()
                + ", hashes=" + signature.constructionsHash() + "/" + signature.objectsHash() + "/"
                + signature.plantsHash() + "/" + signature.terrainHash()
                + ", size=" + size + ", position=" + position;
    }

    static boolean needsLookup(int maximum, boolean adminExempt, boolean admin, int constructions) {
        return !(adminExempt && admin) && maximum >= 0 && (maximum == 0 || constructions > maximum);
    }

    static Decision decide(int maximum, int minimumHours, boolean adminExempt, boolean admin,
                           long playSeconds, int constructions, boolean known) {
        if (adminExempt && admin) return Decision.ALLOWED;
        if (minimumHours > 0 && playSeconds < minimumHours * 3600L) return Decision.PLAYTIME;
        if (needsLookup(maximum, adminExempt, admin, constructions) && !known) return Decision.EXTERNAL;
        return Decision.ALLOWED;
    }

    enum Decision { ALLOWED, PLAYTIME, EXTERNAL, DATABASE }
}
