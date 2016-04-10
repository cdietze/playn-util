package de.cdietze.playn_util;

import playn.scene.Layer;
import tripleplay.ui.Element;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Takes a {@link Layer} and scales it as big as possible while keeping its aspect ratio and its specified <i>world size</i>.
 */
public class ScaledElement extends Element<ScaledElement> {

    private static float maxScale(float aspectRatio, float viewWidth, float viewHeight) {
        checkArgument(aspectRatio > 0f);
        checkArgument(viewWidth > 0f && viewHeight > 0f, "view width and height must be greater than 0, are you using a stretching layout?");
        float maxWidthIfHeightRestricted = viewHeight * aspectRatio;
        float maxWidth = Math.min(viewWidth, maxWidthIfHeightRestricted);
        return maxWidth / viewWidth;
    }

    private final Layer worldLayer;

    public ScaledElement(Layer layer) {
        worldLayer = layer;
        checkArgument(layer.width() > 0f && layer.height() > 0f, "The layer must have a size.");
        this.layer.add(layer);
    }

    @Override
    protected LayoutData createLayoutData(float hintX, float hintY) {
        return new LayoutData() {
            @Override
            public void layout(float left, float top, float width, float height) {
                float ratio = worldLayer.width() / worldLayer.height();
                float scale = maxScale(ratio, width, height);
                worldLayer.setTranslation(left + width * .5f, top + height * .5f);
                worldLayer.setScale(scale * width / worldLayer.width());
            }
        };
    }

    @Override
    protected Class<?> getStyleClass() {
        return ScaledElement.class;
    }
}
