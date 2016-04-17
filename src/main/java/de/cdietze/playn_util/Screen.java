package de.cdietze.playn_util;

import playn.core.Game;
import playn.core.Platform;
import tripleplay.anim.Animator;
import tripleplay.game.ScreenStack;
import tripleplay.ui.Element;
import tripleplay.ui.Layout;
import tripleplay.ui.SimpleStyles;
import tripleplay.ui.Stylesheet;
import tripleplay.ui.layout.AxisLayout;

public abstract class Screen extends ScreenStack.UIScreen {
    /**
     * An animator that's updated on every update(), thus can be used for game logic. This supplements {@link
     * tripleplay.ui.Interface#anim} which is updated on paint().
     */
    public final Animator updateAnim = new Animator();
    public final Platform plat;

    private final Game game;
    private final DialogKeeper dialogKeeper;

    public Screen(Game game) {
        super(game.plat);
        this.game = game;
        this.plat = game.plat;
        this.dialogKeeper = new DialogKeeper(this);
        update.connect(updateAnim.onPaint);
    }

    @Override
    public Game game() {
        return game;
    }

    /**
     * Returns a dialog which contains the supplied contents.
     */
    public DialogKeeper.Dialog createDialog(Element<?> dialog) {
        DialogKeeper.Dialog d = createDialog();
        d.root.add(dialog);
        return d;
    }

    /**
     * Returns a dialog with the supplied root layout.
     */
    public DialogKeeper.Dialog createDialog(Layout layout, Stylesheet stylesheet) {
        return dialogKeeper.new Dialog(this, iface.createRoot(layout, stylesheet, layer));
    }

    /**
     * Returns a dialog with a vertical axis layout.
     */
    public DialogKeeper.Dialog createDialog() {
        return createDialog(AxisLayout.vertical(), SimpleStyles.newSheet(plat.graphics()));
    }
}
