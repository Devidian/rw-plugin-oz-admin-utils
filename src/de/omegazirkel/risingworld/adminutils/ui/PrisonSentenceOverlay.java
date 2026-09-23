package de.omegazirkel.risingworld.adminutils.ui;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.adminutils.db.entities.Prisoner;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.Position;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.ui.style.Unit;

/** Prisoner-only HUD placed immediately below Land Claim's zone overlay. */
public final class PrisonSentenceOverlay extends OZUIElement {
    public static final String PLAYER_ATTRIBUTE = "oz.adminutils.prison-sentence-overlay";

    private final UILabel label;

    private PrisonSentenceOverlay() {
        setPivot(Pivot.UpperCenter);
        style.position.set(Position.Absolute);
        style.left.set(50, Unit.Percent);
        // Land Claim's zone overlay starts at 1.5% and is 34px high.
        style.top.set(6.5f, Unit.Percent);
        style.width.set(28, Unit.Percent);
        style.minWidth.set(20, Unit.Percent);
        style.maxWidth.set(36, Unit.Percent);
        style.height.set(30, Unit.Pixel);
        setBackgroundColor(0f, 0f, 0f, 0.78f);
        setBorder(1);
        setBorderColor(0.95f, 0.75f, 0.25f, 0.62f);
        setBorderEdgeRadius(6, false);

        label = new UILabel();
        label.setFont(Font.DefaultBold);
        label.setFontSize(17);
        label.setFontColor(0xFFFFFFFF);
        label.setPivot(Pivot.MiddleCenter);
        label.setPosition(50, 50, true);
        label.style.width.set(96, Unit.Percent);
        label.style.height.set(24, Unit.Pixel);
        label.setTextAlign(TextAnchor.MiddleCenter);
        label.setTextWrap(false);
        addChild(label);
    }

    public static void refresh(Player player, Prisoner prisoner) {
        if (player == null || prisoner == null || !"INCARCERATED".equalsIgnoreCase(prisoner.status)) {
            hide(player);
            return;
        }
        PrisonSentenceOverlay overlay = (PrisonSentenceOverlay) player.getAttribute(PLAYER_ATTRIBUTE);
        if (overlay == null) {
            overlay = new PrisonSentenceOverlay();
            player.setAttribute(PLAYER_ATTRIBUTE, overlay);
            player.addUIElement(overlay, UITarget.HUD);
        }
        overlay.label.setText(message(player, prisoner));
    }

    public static void hide(Player player) {
        if (player == null) return;
        Object value = player.getAttribute(PLAYER_ATTRIBUTE);
        if (value instanceof PrisonSentenceOverlay overlay) {
            player.removeUIElement(overlay);
        }
        player.deleteAttribute(PLAYER_ATTRIBUTE);
    }

    private static String message(Player player, Prisoner prisoner) {
        long remainingMs = prisoner.realtime
                ? Math.max(0, prisoner.sentenceTotalMs - (System.currentTimeMillis() - prisoner.sentenceStartTs))
                : prisoner.getRemainingMs();
        return I18n.getInstance(AdminUtils.name)
                .get("tc.prison.sentence.remaining", player)
                .replace("PH_TIME", formatDuration(remainingMs));
    }

    private static String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0, durationMs) / 1000;
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;
        return days > 0
                ? String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds)
                : String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
