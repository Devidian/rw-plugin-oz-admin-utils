package de.omegazirkel.risingworld.adminutils.ui;

import java.util.Arrays;
import java.util.List;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.adminutils.db.PrisonerService;
import de.omegazirkel.risingworld.adminutils.db.entities.Prison;
import de.omegazirkel.risingworld.adminutils.db.entities.Prisoner;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.ButtonFactory;
import de.omegazirkel.risingworld.tools.ui.CursorManager;
import de.omegazirkel.risingworld.tools.ui.DangerButton;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.Server;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.Position;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.ui.style.Unit;

public class PrisonDetailOverlay extends OZUIElement {

    public static final String ATTRIBUTE_KEY = "oz.adminutils.prison-detail-overlay";

    private static final float PANEL_WIDTH_PERCENT = 82f;
    private static final float PANEL_HEIGHT_PIXELS = 520f;
    private static final float TABLE_SCROLL_BODY_HEIGHT = 330f;

    private final Player player;
    private final Prison prison;
    private final Callback<Player> onClose;
    private OZUIElement panel;
    private OZUIElement body;

    public PrisonDetailOverlay(Player player, Prison prison, Callback<Player> onClose) {
        this.player = player;
        this.prison = prison;
        this.onClose = onClose;
        setClickable(false);
        setPivot(Pivot.UpperLeft);
        setSize(100, 100, true);
        setBackgroundColor(0, 0, 0, 0.42f);
        rebuild();
    }

    private static I18n t() {
        return I18n.getInstance(AdminUtils.name);
    }

    private void rebuild() {
        removeAllChilds();
        panel = new OZUIElement();
        panel.setPivot(Pivot.MiddleCenter);
        panel.setPosition(50f, 50f, true);
        panel.style.width.set(PANEL_WIDTH_PERCENT, Unit.Percent);
        panel.style.height.set(PANEL_HEIGHT_PIXELS, Unit.Pixel);
        panel.setBackgroundColor(0, 0, 0, 0.86f);
        panel.setBorderColor(0.95f, 0.75f, 0.25f, 0.6f);
        panel.setBorder(1);
        panel.setBorderEdgeRadius(6, false);
        addChild(panel);

        setupHeader();
        setupBody();
        setupFooter();
    }

    private void setupHeader() {
        UILabel title = new UILabel(t().get("TC_UI_PRISON_DETAIL_TITLE", player)
                .replace("PH_PRISON_NAME", prison.name));
        title.setPivot(Pivot.UpperLeft);
        title.setPosition(24, 18, false);
        title.setFont(Font.DefaultBold);
        title.setFontSize(24);
        panel.addChild(title);

        UILabel subtitle = new UILabel(t().get("TC_UI_PRISON_DETAIL_SUBTITLE", player)
                .replace("PH_AREA_ID", String.valueOf(prison.areaId)));
        subtitle.setPivot(Pivot.UpperLeft);
        subtitle.setPosition(24, 52, false);
        subtitle.setFont(Font.Default);
        subtitle.setFontSize(12);
        panel.addChild(subtitle);

        OZUIElement closeButton = new OZUIElement();
        closeButton.setPivot(Pivot.UpperRight);
        closeButton.style.position.set(Position.Absolute);
        closeButton.style.right.set(0, Unit.Pixel);
        closeButton.style.top.set(20, Unit.Pixel);
        closeButton.setSize(34, 34, false);
        closeButton.setBorder(1);
        closeButton.setBorderColor(0.95f, 0.75f, 0.25f, 0.54f);
        closeButton.setBorderEdgeRadius(4, false);
        closeButton.setBackgroundColor(0.12f, 0.10f, 0.08f, 0.9f);
        closeButton.setHoverBackgroundColor(0x611F1AF2);
        closeButton.setClickable(true);
        closeButton.setClickAction(event -> close());
        UILabel closeLabel = centeredLabel("X", 18, true);
        closeButton.addChild(closeLabel);
        panel.addChild(closeButton);
    }

    private void setupBody() {
        body = new OZUIElement();
        body.setPivot(Pivot.UpperLeft);
        body.setPosition(24, 92, false);
        body.style.width.set(96, Unit.Percent);
        body.style.height.set(384, Unit.Pixel);
        body.setBackgroundColor(0.08f, 0.08f, 0.08f, 0.55f);
        body.setBorder(1);
        body.setBorderColor(0.95f, 0.75f, 0.25f, 0.48f);
        body.setBorderEdgeRadius(4, false);
        panel.addChild(body);

        TableScrollView table = new TableScrollView(
                Arrays.asList(
                        t().get("TC_UI_PRISON_DETAIL_TH_PLAYER", player),
                        t().get("TC_UI_PRISON_DETAIL_TH_STATUS", player),
                        t().get("TC_UI_PRISON_DETAIL_TH_TYPE", player),
                        t().get("TC_UI_PRISON_DETAIL_TH_REMAINING", player),
                        t().get("TC_UI_PRISON_DETAIL_TH_ACTIONS", player)),
                Arrays.asList(30f, 18f, 16f, 18f, 18f));
        table.setScrollBodyHeight(TABLE_SCROLL_BODY_HEIGHT);

        PrisonerService prisonerService = AdminUtils.prisonerService();
        List<Prisoner> prisoners = prisonerService == null ? List.of() : prisonerService.getByPrison(prison.areaId);
        if (prisoners.isEmpty()) {
            table.addRow(textOnlyRow(t().get("TC_UI_PRISON_DETAIL_EMPTY", player), 100f));
        } else {
            for (Prisoner prisoner : prisoners) {
                table.addRow(prisonerRow(prisoner));
            }
        }
        body.addChild(table);
    }

    private TableRow prisonerRow(Prisoner prisoner) {
        return new TableRow(Arrays.asList(
                labelCell(playerName(prisoner), 30f),
                labelCell(prisoner.status, 18f),
                labelCell(sentenceType(prisoner), 16f),
                labelCell(remaining(prisoner), 18f),
                new TableCell(actions(prisoner), 18f)));
    }

    private TableRow textOnlyRow(String text, float width) {
        return new TableRow(Arrays.asList(labelCell(text, width)));
    }

    private TableCell labelCell(String text, float width) {
        UILabel label = new UILabel(text == null ? "" : text);
        label.setFontSize(13);
        label.setTextAlign(TextAnchor.MiddleLeft);
        label.setPivot(Pivot.MiddleLeft);
        label.setPosition(2, 50, true);
        return new TableCell(label, width);
    }

    private OZUIElement actions(Prisoner prisoner) {
        OZUIElement actions = new OZUIElement();
        actions.setSize(100, 100, true);
        if (!"RELEASED".equalsIgnoreCase(prisoner.status)) {
            DangerButton pardon = ButtonFactory.danger(t().get("TC_UI_PRISON_DETAIL_PARDON", player), event -> {
                pardon(prisoner);
            });
            pardon.setPivot(Pivot.UpperLeft);
            pardon.setPosition(0, 8, true);
            pardon.setSize(70, 24, false);
            pardon.setBorderEdgeRadius(3, false);
            actions.addChild(pardon);
        } else {
            UILabel released = new UILabel("-");
            released.setFontSize(13);
            released.setTextAlign(TextAnchor.MiddleLeft);
            actions.addChild(released);
        }
        return actions;
    }

    private void pardon(Prisoner prisoner) {
        PrisonerService prisonerService = AdminUtils.prisonerService();
        if (prisonerService == null) {
            player.sendTextMessage(t().get("TC_PRISON_DETAIL_SERVICE_UNAVAILABLE", player));
            return;
        }

        Player prisonerPlayer = Server.getPlayerByDbID(prisoner.playerDbId);
        if (AdminUtils.prisonReleaseService() != null && prisonerPlayer != null) {
            if (AdminUtils.prisonReleaseService().release(prisonerPlayer, prisoner, "PARDONED").success()) {
                player.sendTextMessage(t().get("TC_PRISON_DETAIL_PARDONED_RESTORED", player)
                        .replace("PH_PLAYER_NAME", playerName(prisoner)));
                rebuild();
                return;
            }
        } else {
            prisonerService.pardon(prisoner, "PARDONED", System.currentTimeMillis());
        }

        player.sendTextMessage(t().get("TC_PRISON_DETAIL_PARDONED", player)
                .replace("PH_PLAYER_NAME", playerName(prisoner)));
        rebuild();
    }

    private String playerName(Prisoner prisoner) {
        if (prisoner.playerName != null && !prisoner.playerName.isBlank()) {
            return prisoner.playerName;
        }
        return "DB #" + prisoner.playerDbId;
    }

    private String sentenceType(Prisoner prisoner) {
        return t().get(prisoner.realtime ? "TC_UI_PRISON_DETAIL_TYPE_REALTIME" : "TC_UI_PRISON_DETAIL_TYPE_GAMETIME",
                player);
    }

    private String remaining(Prisoner prisoner) {
        long remainingMs = prisoner.realtime
                ? Math.max(0, prisoner.sentenceTotalMs - (System.currentTimeMillis() - prisoner.sentenceStartTs))
                : prisoner.getRemainingMs();
        long minutes = Math.max(0, (remainingMs + 59999) / 60000);
        return t().get("TC_UI_PRISON_DETAIL_MINUTES", player).replace("PH_MINUTES", String.valueOf(minutes));
    }

    private UILabel centeredLabel(String text, float fontSize, boolean bold) {
        UILabel label = new UILabel(text);
        label.setPivot(Pivot.MiddleCenter);
        label.setPosition(50, 50, true);
        label.setSize(100, 100, true);
        label.setFont(bold ? Font.DefaultBold : Font.Default);
        label.setFontSize(fontSize);
        label.setTextAlign(TextAnchor.MiddleCenter);
        return label;
    }

    private void setupFooter() {
        UILabel legend = new UILabel(t().get("TC_UI_PRISON_DETAIL_FOOTER", player));
        legend.setPivot(Pivot.LowerLeft);
        legend.setPosition(24, PANEL_HEIGHT_PIXELS - 18, false);
        legend.setFontSize(12);
        panel.addChild(legend);
    }

    private void close() {
        player.removeUIElement(this);
        CursorManager.hide(player);
        player.deleteAttribute(ATTRIBUTE_KEY);
        onClose.onCall(player);
    }
}
