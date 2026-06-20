package de.omegazirkel.risingworld.adminutils.ui;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BaseButton;
import de.omegazirkel.risingworld.tools.ui.ButtonFactory;
import de.omegazirkel.risingworld.tools.ui.CursorManager;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UIScrollView;
import net.risingworld.api.ui.UIScrollView.ScrollViewMode;
import net.risingworld.api.ui.style.Align;
import net.risingworld.api.ui.style.DisplayStyle;
import net.risingworld.api.ui.style.FlexDirection;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Justify;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.ui.style.Unit;

public class NewPlayerInfoOverlay extends OZUIElement {
    public static final String PLAYER_ATTRIBUTE = "oz.adminutils.new-player-info";

    private static I18n t() {
        return I18n.getInstance(AdminUtils.name);
    }

    public NewPlayerInfoOverlay(Player player, String messageText, int widthPercent, int heightPercent) {
        setPivot(Pivot.UpperLeft);
        setPosition(0, 0, true);
        setSize(100, 100, true);
        setBackgroundColor(0, 0, 0, 0.45f);
        setClickable(true);

        UIElement panel = new UIElement();
        panel.setPivot(Pivot.MiddleCenter);
        panel.setPosition(50, 50, true);
        panel.setSize(widthPercent, heightPercent, true);
        panel.setBackgroundColor(0, 0, 0, 0.9f);
        panel.setBorder(2);
        panel.setBorderColor(0.95f, 0.75f, 0.25f, 0.62f);
        panel.setBorderEdgeRadius(6, false);
        addChild(panel);

        UILabel title = new UILabel(t().get("TC_NEW_PLAYER_INFO_TITLE", player));
        title.setPivot(Pivot.UpperLeft);
        title.setPosition(5, 6, true);
        title.setSize(90, 14, true);
        title.setFont(Font.DefaultBold);
        title.setFontSize(18);
        title.setTextAlign(TextAnchor.MiddleCenter);
        panel.addChild(title);

        UIScrollView messageScroll = new UIScrollView(ScrollViewMode.Vertical);
        messageScroll.setPivot(Pivot.UpperLeft);
        messageScroll.setPosition(7, 23, true);
        messageScroll.setSize(86, 46, true);
        messageScroll.setMouseWheelScrollSize(28);
        panel.addChild(messageScroll);

        UILabel message = new UILabel(messageText);
        message.setPivot(Pivot.UpperLeft);
        message.setPosition(0, 0, false);
        message.style.width.set(96, Unit.Percent);
        message.style.height.set(estimateMessageHeight(messageText, widthPercent), Unit.Pixel);
        message.setFontSize(14);
        message.setTextAlign(TextAnchor.UpperLeft);
        message.setTextWrap(true);
        messageScroll.addChild(message);

        UIElement footer = new UIElement();
        footer.setPivot(Pivot.LowerCenter);
        footer.setPosition(50, 94, true);
        footer.setSize(92, 20, true);
        footer.style.display.set(DisplayStyle.Flex);
        footer.style.flexDirection.set(FlexDirection.Row);
        footer.style.justifyContent.set(Justify.Center);
        footer.style.alignItems.set(Align.Center);
        panel.addChild(footer);

        footer.addChild(button(ButtonFactory.ok(t().get("TC_NEW_PLAYER_INFO_OK", player), event -> close(event.getPlayer())),
                34));
        footer.addChild(button(ButtonFactory.cancel(t().get("TC_NEW_PLAYER_INFO_DONT_SHOW", player), event -> {
            AdminUtilsPlayerPluginSettings.setNewPlayerInfoVisible(event.getPlayer(), false);
            close(event.getPlayer());
        }), 58));
    }

    static int estimateMessageHeight(String messageText, int widthPercent) {
        int charactersPerLine = Math.max(16, widthPercent);
        int lineCount = 0;
        for (String line : messageText.split("\\R", -1)) {
            lineCount += Math.max(1, (line.length() + charactersPerLine - 1) / charactersPerLine);
        }
        return Math.max(120, lineCount * 22);
    }

    private UIElement button(BaseButton button, int widthPercent) {
        button.setPivot(Pivot.UpperLeft);
        button.style.display.set(DisplayStyle.Flex);
        button.style.justifyContent.set(Justify.Center);
        button.style.alignItems.set(Align.Center);
        button.style.width.set(widthPercent, Unit.Percent);
        button.style.height.set(34, Unit.Pixel);
        button.style.marginLeft.set(3, Unit.Pixel);
        button.style.marginRight.set(3, Unit.Pixel);
        button.setBorderEdgeRadius(4, false);
        return button;
    }

    public void close(Player player) {
        player.removeUIElement(this);
        player.deleteAttribute(PLAYER_ATTRIBUTE);
        CursorManager.hide(player);
    }
}
