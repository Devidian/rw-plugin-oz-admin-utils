package de.omegazirkel.risingworld.adminutils.ui;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.adminutils.db.PrisonerService;
import de.omegazirkel.risingworld.adminutils.db.entities.Prison;
import de.omegazirkel.risingworld.adminutils.db.entities.Prisoner;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.BasePluginOverlayWithTabs;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.Server;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;

public class PrisonDetailOverlay extends BasePluginOverlayWithTabs {

    public static final String ATTRIBUTE_KEY = "oz.adminutils.prison-detail-overlay";

    private static final float TABLE_SCROLL_BODY_HEIGHT = 330f;

    private enum DetailTab { CURRENT, OTHER }

    private final Player player;
    private Prison prison;
    private DetailTab activeDetailTab = DetailTab.CURRENT;

    public PrisonDetailOverlay(Player player, Prison prison, Callback<Player> onClose) {
        super(player, onClose);
        this.player = player;
        this.prison = prison;
        rebuild();
    }

    @Override
    protected I18n t() {
        return I18n.getInstance(AdminUtils.name);
    }

    @Override
    protected String titleText() {
        return t().get("tc.ui.prison.detail.title", player).replace("PH_PRISON_NAME", prison.name);
    }

    @Override
    protected String descriptionText() {
        return t().get("tc.ui.prison.detail.subtitle", player).replace("PH_AREA_ID", String.valueOf(prison.areaId));
    }

    @Override
    protected String legendText() {
        return t().get("tc.ui.prison.detail.footer", player);
    }

    @Override
    protected void setupTabs() {
        setupTabContainer();
        addTab(t().get("tc.ui.prison.detail.tab.current", player), 180, activeDetailTab == DetailTab.CURRENT, true, () -> {
            activeDetailTab = DetailTab.CURRENT;
            rebuild();
        });
        addTab(t().get("tc.ui.prison.detail.tab.other", player), 180, activeDetailTab == DetailTab.OTHER, true, () -> {
            activeDetailTab = DetailTab.OTHER;
            rebuild();
        });
        if (activeDetailTab == DetailTab.CURRENT) {
            setupPrisonerTable();
        } else {
            setupOtherPrisonsTable();
        }
    }

    private void setupPrisonerTable() {
        TableScrollView table = new TableScrollView(
                Arrays.asList(
                        t().get("tc.ui.prison.detail.th.player", player),
                        t().get("tc.ui.prison.detail.th.status", player),
                        t().get("tc.ui.prison.detail.th.type", player),
                        t().get("tc.ui.prison.detail.th.remaining", player),
                        t().get("tc.ui.prison.detail.th.actions", player)),
                Arrays.asList(30f, 18f, 16f, 18f, 18f));
        table.setScrollBodyHeight(TABLE_SCROLL_BODY_HEIGHT);

        PrisonerService prisonerService = AdminUtils.prisonerService();
        List<Prisoner> prisoners = prisonerService == null ? List.of() : prisonerService.getByPrison(prison.areaId);
        if (prisoners.isEmpty()) {
            table.addRow(textOnlyRow(t().get("tc.ui.prison.detail.empty", player), 100f));
        } else {
            for (Prisoner prisoner : prisoners) {
                table.addRow(prisonerRow(prisoner));
            }
        }
        body.addChild(table);
    }

    private void setupOtherPrisonsTable() {
        TableScrollView table = new TableScrollView(
                Arrays.asList(
                        t().get("tc.ui.prison.detail.th.zone", player),
                        t().get("tc.ui.prison.detail.th.prisoners", player),
                        t().get("tc.ui.prison.detail.th.actions", player)),
                Arrays.asList(50f, 20f, 30f));
        table.setScrollBodyHeight(TABLE_SCROLL_BODY_HEIGHT);
        List<Prison> prisons = AdminUtils.prisonService() == null ? List.of() : AdminUtils.prisonService().getAll();
        prisons.stream().sorted(Comparator.comparingLong(value -> value.areaId)).forEach(candidate -> table.addRow(prisonRow(candidate)));
        if (prisons.isEmpty()) {
            table.addRow(textOnlyRow(t().get("tc.ui.prison.detail.empty", player), 100f));
        }
        body.addChild(table);
    }

    private TableRow prisonRow(Prison candidate) {
        PrisonerService service = AdminUtils.prisonerService();
        int prisoners = service == null ? 0 : service.getByPrison(candidate.areaId).size();
        OZUIElement actions = new OZUIElement();
        actions.setSize(100, 100, true);
        AdvancedButton select = AdvancedButtonFactory.defaultButton(t().get("tc.ui.prison.detail.open", player), event -> {
            prison = candidate;
            activeDetailTab = DetailTab.CURRENT;
            rebuild();
        });
        select.setPivot(Pivot.UpperLeft);
        select.setPosition(0, 4, false);
        select.setSize(78, 26, false);
        actions.addChild(select);
        if (prisoners == 0) {
            AdvancedButton dissolve = AdvancedButtonFactory.danger(t().get("tc.menu.prison.zone.dissolve", player), event -> showDissolveConfirmation(candidate));
            dissolve.setPivot(Pivot.UpperLeft);
            dissolve.setPosition(86, 4, false);
            dissolve.setSize(150, 26, false);
            actions.addChild(dissolve);
        }
        return new TableRow(Arrays.asList(
                labelCell(candidate.name + " (#" + candidate.areaId + ")", 50f),
                labelCell(String.valueOf(prisoners), 20f),
                new TableCell(actions, 30f)));
    }

    public void showDissolveConfirmation(Prison candidate) {
        OZUIElement dialog = new OZUIElement();
        dialog.setPivot(Pivot.MiddleCenter);
        dialog.setPosition(50, 50, true);
        dialog.setSize(480, 190, false);
        dialog.setBackgroundColor(0x10100EF8);
        dialog.setBorder(1);
        dialog.setBorderColor(0xD7AE55FF);
        panel.addChild(dialog);

        UILabel title = new UILabel(t().get("tc.ui.prison.dissolve.confirm.title", player));
        title.setPivot(Pivot.UpperLeft);
        title.setPosition(20, 18, false);
        title.setSize(440, 28, false);
        title.setFontSize(18);
        dialog.addChild(title);

        UILabel message = new UILabel(t().get("tc.ui.prison.dissolve.confirm.message", player)
                .replace("PH_AREA_NAME", candidate.name));
        message.setPivot(Pivot.UpperLeft);
        message.setPosition(20, 54, false);
        message.setSize(440, 64, false);
        message.setFontSize(13);
        message.setTextWrap(true);
        dialog.addChild(message);

        AdvancedButton cancel = AdvancedButtonFactory.cancel(t().get("tc.ui.cancel", player), event -> panel.removeChild(dialog));
        cancel.setPivot(Pivot.UpperLeft);
        cancel.setPosition(24, 142, false);
        cancel.setSize(150, 32, false);
        dialog.addChild(cancel);
        AdvancedButton confirm = AdvancedButtonFactory.danger(t().get("tc.menu.prison.zone.dissolve", player), event -> {
            panel.removeChild(dialog);
            dissolve(candidate);
        });
        confirm.setPivot(Pivot.UpperRight);
        confirm.setPosition(456, 142, false);
        confirm.setSize(180, 32, false);
        dialog.addChild(confirm);
    }

    private void dissolve(Prison candidate) {
        PrisonerService prisonerService = AdminUtils.prisonerService();
        if (prisonerService != null && !prisonerService.getByPrison(candidate.areaId).isEmpty()) {
            player.sendTextMessage(t().get("tc.prison.zone.dissolve.blocked", player).replace("PH_AREA_NAME", candidate.name));
            rebuild();
            return;
        }
        if (AdminUtils.prisonService() != null && AdminUtils.prisonService().remove(candidate.areaId)) {
            player.sendTextMessage(t().get("tc.prison.zone.dissolved", player).replace("PH_AREA_NAME", candidate.name));
        }
        rebuild();
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
            AdvancedButton pardon = AdvancedButtonFactory.danger(t().get("tc.ui.prison.detail.pardon", player), event -> {
                pardon(prisoner);
            });
            pardon.setPivot(Pivot.UpperLeft);
            pardon.setPosition(0, 4, false);
            pardon.setSize(132, 26, false);
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
            player.sendTextMessage(t().get("tc.prison.detail.service.unavailable", player));
            return;
        }

        Player prisonerPlayer = Server.getPlayerByDbID(prisoner.playerDbId);
        if (AdminUtils.prisonReleaseService() != null && prisonerPlayer != null) {
            if (AdminUtils.prisonReleaseService().release(prisonerPlayer, prisoner, "PARDONED").success()) {
                player.sendTextMessage(t().get("tc.prison.detail.pardoned.restored", player)
                        .replace("PH_PLAYER_NAME", playerName(prisoner)));
                rebuild();
                return;
            }
        } else {
            prisonerService.pardon(prisoner, "PARDONED", System.currentTimeMillis());
        }

        player.sendTextMessage(t().get("tc.prison.detail.pardoned", player)
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
        return t().get(prisoner.realtime ? "tc.ui.prison.detail.type.realtime" : "tc.ui.prison.detail.type.gametime",
                player);
    }

    private String remaining(Prisoner prisoner) {
        long remainingMs = prisoner.realtime
                ? Math.max(0, prisoner.sentenceTotalMs - (System.currentTimeMillis() - prisoner.sentenceStartTs))
                : prisoner.getRemainingMs();
        long minutes = Math.max(0, (remainingMs + 59999) / 60000);
        return t().get("tc.ui.prison.detail.minutes", player).replace("PH_MINUTES", String.valueOf(minutes));
    }

    @Override
    protected void close() {
        player.deleteAttribute(ATTRIBUTE_KEY);
        super.close();
    }
}
