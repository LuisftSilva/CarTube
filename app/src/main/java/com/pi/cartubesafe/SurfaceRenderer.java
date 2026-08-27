package com.pi.cartubesafe;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.car.app.SurfaceContainer;

/** Draws a static diagnostic frame into the Android Auto navigation surface. */
final class SurfaceRenderer {
    private SurfaceContainer container;
    private Rect visibleArea;
    private Rect stableArea;
    private String interaction = "none";
    private float interactionX;
    private float interactionY;
    private long interactionCount;

    synchronized void attach(@NonNull SurfaceContainer surfaceContainer) {
        container = surfaceContainer;
        redraw("surface-available");
    }

    synchronized void detach(@NonNull SurfaceContainer surfaceContainer) {
        if (container == surfaceContainer) {
            container = null;
        }
    }

    synchronized void setVisibleArea(@NonNull Rect area) {
        visibleArea = new Rect(area);
        redraw("visible-area");
    }

    synchronized void setStableArea(@NonNull Rect area) {
        stableArea = new Rect(area);
        redraw("stable-area");
    }

    synchronized void setLastInteraction(String type, float x, float y) {
        interaction = type;
        interactionX = x;
        interactionY = y;
        interactionCount++;
        redraw(type);
    }

    synchronized void redraw(String reason) {
        SurfaceContainer current = container;
        if (current == null) return;

        Surface surface = current.getSurface();
        if (surface == null || !surface.isValid()) {
            LogStore.w("SurfaceRenderer", "Surface unavailable during redraw: " + reason);
            return;
        }

        Canvas canvas = null;
        try {
            canvas = surface.lockCanvas(null);
            int width = canvas.getWidth();
            int height = canvas.getHeight();

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            canvas.drawColor(Color.rgb(18, 18, 18));

            float margin = Math.max(28f, Math.min(width, height) * 0.055f);
            float titleSize = Math.max(34f, Math.min(width, height) * 0.075f);
            float bodySize = Math.max(22f, Math.min(width, height) * 0.038f);
            float lineGap = bodySize * 1.5f;

            paint.setColor(Color.WHITE);
            paint.setTextSize(titleSize);
            paint.setFakeBoldText(true);
            canvas.drawText("CarTube Surface OK", margin, margin + titleSize, paint);

            paint.setFakeBoldText(false);
            paint.setTextSize(bodySize);
            paint.setColor(Color.LTGRAY);

            float y = margin + titleSize + lineGap;
            canvas.drawText("Android Auto Car App Library", margin, y, paint);
            y += lineGap;
            canvas.drawText(
                    "Surface: " + current.getWidth() + " x " + current.getHeight()
                            + " @ " + current.getDpi() + " dpi",
                    margin,
                    y,
                    paint
            );
            y += lineGap;
            canvas.drawText("Visible area: " + rectText(visibleArea), margin, y, paint);
            y += lineGap;
            canvas.drawText("Stable area: " + rectText(stableArea), margin, y, paint);
            y += lineGap;
            canvas.drawText(
                    "Interaction #" + interactionCount + ": " + interaction
                            + " (" + Math.round(interactionX) + ", " + Math.round(interactionY) + ")",
                    margin,
                    y,
                    paint
            );
            y += lineGap * 1.4f;

            paint.setColor(Color.WHITE);
            paint.setFakeBoldText(true);
            canvas.drawText("SurfaceCallback ativo", margin, y, paint);
            y += lineGap;
            paint.setFakeBoldText(false);
            paint.setColor(Color.LTGRAY);
            canvas.drawText("POC seguro: sem vídeo durante a condução", margin, y, paint);

            LogStore.i(
                    "SurfaceRenderer",
                    "Frame drawn reason=" + reason + " canvas=" + width + "x" + height
            );
        } catch (Throwable error) {
            LogStore.e("SurfaceRenderer", "Could not draw frame: " + reason, error);
        } finally {
            if (canvas != null) {
                try {
                    surface.unlockCanvasAndPost(canvas);
                } catch (Throwable error) {
                    LogStore.e("SurfaceRenderer", "unlockCanvasAndPost failed", error);
                }
            }
        }
    }

    private static String rectText(Rect rect) {
        if (rect == null) return "pending";
        return rect.left + "," + rect.top + " - " + rect.right + "," + rect.bottom;
    }
}
