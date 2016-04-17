package de.cdietze.playn_util;

import playn.core.Surface;
import playn.scene.Layer;
import pythagoras.f.IDimension;
import pythagoras.f.Point;
import react.*;
import tripleplay.anim.Animation;
import tripleplay.game.ScreenStack;
import tripleplay.ui.Element;
import tripleplay.ui.Root;
import tripleplay.ui.Style;
import tripleplay.util.Colors;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * Mostly taken from https://groups.google.com/d/msg/ooo-libs/pdCQmqSNaLk/oVEBCJyLt_8J
 */
public class DialogKeeper {

    private float dialogDepth = 100f;
    private final List<Dialog> dialogs = new ArrayList<>();

    private final ScreenStack.Screen screen;

    public DialogKeeper(ScreenStack.Screen screen) {
        this.screen = screen;
    }

    public class Dialog implements Closeable {
        /**
         * The root that contains this dialog UI.
         */
        public final Root root;
        /**
         * A connection group that will be cleared when this dialog is dismissed.
         */
        public final Closeable.Set conns = new Closeable.Set();
        /**
         * A signal emitted (500ms) after the dialog reveal animation completes ({@code true}) and (immediately) after the
         * dialog hide animation completes ({@code false}). You probably want to use {@link #onShown} and {@link #onHidden}
         * unless your needs are special.
         */
        public final Signal<Boolean> shown = Signal.create();
        protected final Value<Boolean> _showing = Value.create(false);
        private final Screen screen;
        protected Runnable _onDisplay = new Runnable() {
            @Override
            public void run() {
            }
        };
        protected DialogAnim _anim = DialogAnim.GROW_SHRINK;
        protected ShaderLayer _shade;
        protected float _y = -1;
        protected Style.VAlign _valign;
        protected float _duration = 500f;

        Dialog(Screen screen, Root root) {
            this.screen = screen;
            this.root = root;
        }

        /** Adds an element to this Dialog. Supports chaining. */
        public Dialog add(Element<?> element) {
            root.add(element);
            return this;
        }

        /**
         * A value that exposes this dialog's showing state. Read-only.
         */
        public ValueView<Boolean> showing() {
            return _showing;
        }

        /**
         * Registers {@code slot} to be called a beat (500ms) after this dialog's show animation has completed.
         */
        public Dialog onShown(UnitSlot slot) {
            shown.connect(slot.filtered(Functions.<Boolean>identity()));
            return this;
        }

        /**
         * Registers {@code slot} to be called immediately after this dialog's hide animation has completed.
         */
        public Dialog onHidden(UnitSlot slot) {
            shown.connect(slot.filtered(Functions.NOT));
            return this;
        }

        public Dialog duration(float duration) {
            _duration = duration;
            return this;
        }

        /**
         * Configures this dialog to simply appear and disappear, no animation.
         */
        public Dialog noAnimate() {
            _anim = DialogAnim.NONE;
            return this;
        }

        /**
         * Configures this dialog to alpha fade in and out.
         */
        public Dialog fadeInOut() {
            _anim = DialogAnim.FADE_INOUT;
            return this;
        }

        /**
         * Configures this dialog to grow into view and shrink out of view.
         */
        public Dialog growShrink() {
            _anim = DialogAnim.GROW_SHRINK;
            return this;
        }

        public Dialog slideLeftRight() {
            _anim = DialogAnim.SLIDE_LEFT_RIGHT;
            return this;
        }

        public Dialog slideTopDown() {
            _anim = DialogAnim.SLIDE_TOP_DOWN;
            return this;
        }

        /**
         * Configures this dialog to display a click-intercepting shade over the main screen, while it is visible.
         */
        public Dialog useShade() {
            checkState(_shade == null, "Already called useShade on this dialog.");
            _shade = new ShaderLayer();
            _shade.setHitTester(new Layer.HitTester() {
                public Layer hitTest(Layer layer, Point p) {
                    return _shade;
                }
            });
            _shade.setInteractive(true);
            _shade.setDepth(dialogDepth - 1);
            return this;
        }

        /**
         * Specifies the y position of the dialog, when shown.
         */
        public Dialog atY(float y) {
            _y = y;
            _valign = Style.VAlign.TOP;
            return this;
        }

        /**
         * Specifies the y position of the (vertical) center of the dialog, when shown.
         */
        public Dialog centerY(float y) {
            _y = y;
            _valign = Style.VAlign.CENTER;
            return this;
        }

        /**
         * Packs and displays this dialog.
         */
        public Dialog display() {
            return display(screen.size().width() - 20);
        }

        /**
         * Packs this dialog to the specified width and displays it.
         */
        public Dialog display(float packWidth) {
            checkState(!_showing.get(), "Dialog is already displayed.");
            root.layer.setDepth(dialogDepth);
            root.pack(packWidth, 0);
            // make sure we go away if the screen is for some reason hidden before we're dismissed;
            // usually this is because the screen is hidden while we're doing our close animation
            // and the close animation never completes
            dialogs.add(this);
            // run the show animation, pause a beat, then emit our "shown" signal
            _anim.show(screen, root, _duration, computeY()).then().
                    delay(_duration).then().emit(shown, true);
            // if we have a shade, animate that into view as well
            if (_shade != null) screen.iface.anim.add(screen.layer, _shade).then().
                    tween(_shade.alphaValue()).from(0).to(0.375f).in(_duration);
            dialogDepth += 2; // ensure that subsequent dialogs will render above this one
            _showing.update(true); // report that we're showing
            return this;
        }

        /**
         * Repacks the dialog root to the default width.
         */
        public void repack() {
            root.pack(screen.size().width() - 20, 0);
            root.layer.setTranslation((screen.size().width() - root.size().width()) / 2, computeY());
        }

        /**
         * Dismisses the dialog.
         */
        public void dismiss() {
            if (!_showing.get()) return; // ignore multiple dismiss() calls
            this.conns.close(); // clear any connections bound to this dialog's lifecycle
            // if we have a shade, start fading that out
            if (_shade != null) screen.iface.anim.tween(_shade.alphaValue()).to(0).in(_duration);
            // run the hide animation, destroy the interface, then run the onDismissed action
            _anim.hide(screen, root, _duration).then().action(new Runnable() {
                public void run() {
                    // if we get this far, the screen need no longer clean up after us
                    dialogs.remove(Dialog.this);
                    close();
                }
            }).then().emit(shown, false);
            _showing.update(false); // report that we're no longer showing
        }

        /**
         * Returns a slot that dismisses this dialog when triggered.
         */
        public UnitSlot dismissSlot() {
            return new UnitSlot() {
                public void onEmit() {
                    dismiss();
                }
            };
        }

        /**
         * Destroys this dialog without any nice animation or pleasantry. You probably want {@link #dismiss} instead.
         */
        public void close() {
            if (screen.iface.disposeRoot(root)) {
                dialogDepth -= 2; // reduce dialog depth now that we're gone
                if (_shade != null) _shade.close();
            }
        }

        protected float computeY() {
            float height = root.size().height(), y;
            if (_y == -1) y = (screen.size().height() - height) / 2;
            else switch (_valign) {
                default:
                case TOP:
                    y = _y;
                    break;
                case CENTER:
                    y = _y - height / 2;
                    break;
                case BOTTOM:
                    y = _y - height;
                    break;
            }
            return y;
        }
    }

    enum DialogAnim {
        NONE {
            @Override
            public Animation show(ScreenStack.UIScreen screen, Root root, float duration, float y) {
                root.layer.setTranslation((screen.size().width() - root.size().width()) / 2, y);
                return screen.iface.anim.delay(0);
            }

            @Override
            public Animation hide(ScreenStack.UIScreen screen, Root root, float duration) {
                return screen.iface.anim.delay(0);
            }
        },
        SLIDE_LEFT_RIGHT {
            @Override
            public Animation show(ScreenStack.UIScreen screen, Root root, float duration, float y) {
                float dwidth = root.size().width(), tx = (screen.size().width() - dwidth) / 2;
                root.layer.setTranslation(-dwidth, y);
                return screen.iface.anim.tweenX(root.layer).easeIn().to(tx).in(duration);
            }

            @Override
            public Animation hide(ScreenStack.UIScreen screen, Root root, float duration) {
                return screen.iface.anim.tweenX(root.layer).easeOut().to(screen.size().width()).in(duration);
            }
        },
        SLIDE_TOP_DOWN {
            @Override
            public Animation show(ScreenStack.UIScreen screen, Root root, float duration, float y) {
                float dheight = root.size().height();
                root.layer.setTranslation((screen.size().width() - root.size().width()) / 2, -dheight);
                return screen.iface.anim.tweenY(root.layer).easeOut().to(y).in(duration);
            }

            @Override
            public Animation hide(ScreenStack.UIScreen screen, Root root, float duration) {
                return screen.iface.anim.tweenY(root.layer).easeOut().to(screen.size().height()).in(duration);
            }
        },
        FADE_INOUT {
            @Override
            public Animation show(ScreenStack.UIScreen screen, Root root, float duration, float y) {
                root.layer.setTranslation((screen.size().width() - root.size().width()) / 2, y);
                root.layer.setAlpha(0);
                return screen.iface.anim.tweenAlpha(root.layer).easeIn().to(1).in(duration);
            }

            @Override
            public Animation hide(ScreenStack.UIScreen screen, Root root, float duration) {
                return screen.iface.anim.tweenAlpha(root.layer).easeOut().to(0).in(duration);
            }
        },
        GROW_SHRINK {
            @Override
            public Animation show(final ScreenStack.UIScreen screen, final Root root, float duration,
                                  final float y) {
                final IDimension dsize = root.size();
                root.layer.setOrigin(dsize.width() / 2, dsize.height() / 2);
                root.layer.setTranslation(screen.size().width() / 2, y + dsize.height() / 2);
                root.layer.setScale(0.01f);
                return screen.iface.anim.tweenScale(root.layer).easeInOut().to(1).in(duration).
                        then().action(new Runnable() {
                    public void run() {
                        root.layer.setOrigin(0, 0);
                        root.layer.setTranslation((screen.size().width() - dsize.width()) / 2, y);
                    }
                });
            }

            @Override
            public Animation hide(ScreenStack.UIScreen screen, Root root, float duration) {
                IDimension dsize = root.size();
                root.layer.setOrigin(dsize.width() / 2, dsize.height() / 2);
                root.layer.setTranslation(root.x() + dsize.width() / 2, root.y() + dsize.height() / 2);
                return screen.iface.anim.tweenScale(root.layer).easeInOut().to(0.01f).in(duration);
            }
        };

        public abstract Animation show(ScreenStack.UIScreen screen, Root root, float duration, float y);

        public abstract Animation hide(ScreenStack.UIScreen screen, Root root, float duration);
    }


    class ShaderLayer extends Layer {

        float alpha = 0f;

        @Override
        protected void paintImpl(Surface surf) {
            surf.setAlpha(alpha);
            surf.setFillColor(Colors.BLACK).fillRect(0, 0, screen.size().width(), screen.size().height());
        }

        public Animation.Value alphaValue() {
            return new Animation.Value() {
                @Override
                public float initial() {
                    return alpha;
                }

                @Override
                public void set(float value) {
                    alpha = value;
                }
            };
        }
    }
}
