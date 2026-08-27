package com.pi.cartubesafe;

import android.content.Intent;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.car.app.AppManager;
import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.SessionInfo;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.validation.HostValidator;
import androidx.core.graphics.drawable.IconCompat;

/**
 * Android Auto surface proof-of-concept.
 *
 * This service intentionally uses only public AndroidX Car App Library APIs. It does not
 * hook Android Auto, change driving restrictions, or render video while driving. Its goal
 * is to verify that the host discovers CarTube as a templated app and grants a map Surface.
 */
public final class CarTubeCarAppService extends CarAppService {

    @Override
    public void onCreate() {
        super.onCreate();
        LogStore.init(this);
        LogStore.i("CarTubeCarAppService", "CarAppService created");
        LogStore.syncDriveBestEffort();
    }

    @NonNull
    @Override
    public Session onCreateSession(@NonNull SessionInfo sessionInfo) {
        LogStore.i("CarTubeCarAppService", "onCreateSession: " + sessionInfo);
        LogStore.syncDriveBestEffort();

        return new Session() {
            @NonNull
            @Override
            public Screen onCreateScreen(@NonNull Intent intent) {
                LogStore.i("CarTubeCarAppService", "onCreateScreen: " + intent);
                return new SurfaceScreen(getCarContext());
            }

            @Override
            public void onNewIntent(@NonNull Intent intent) {
                super.onNewIntent(intent);
                LogStore.i("CarTubeCarAppService", "onNewIntent: " + intent);
                LogStore.syncDriveBestEffort();
            }
        };
    }

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        // This branch is specifically for sideloaded development testing. The host still
        // enforces Android Auto UX/driving restrictions; this only avoids rejecting a debug
        // host certificate at the app-side validator.
        LogStore.i("CarTubeCarAppService", "createHostValidator");
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @Override
    public void onDestroy() {
        LogStore.i("CarTubeCarAppService", "CarAppService destroyed");
        LogStore.syncDriveBestEffort();
        super.onDestroy();
    }

    private static final class SurfaceScreen extends Screen implements SurfaceCallback {
        private final SurfaceRenderer renderer = new SurfaceRenderer();

        SurfaceScreen(@NonNull CarContext carContext) {
            super(carContext);
            LogStore.i("CarTubeSurface", "Screen created; Car API=" + carContext.getCarAppApiLevel());
            carContext.getCarService(AppManager.class).setSurfaceCallback(this);
            LogStore.syncDriveBestEffort();
        }

        @NonNull
        @Override
        public Template onGetTemplate() {
            CarIcon icon = new CarIcon.Builder(
                    IconCompat.createWithResource(getCarContext(), R.drawable.ic_launcher)
            ).build();

            Action redraw = new Action.Builder()
                    .setIcon(icon)
                    .setOnClickListener(() -> renderer.redraw("action"))
                    .build();

            ActionStrip actions = new ActionStrip.Builder()
                    .addAction(redraw)
                    .build();

            ActionStrip mapActions = new ActionStrip.Builder()
                    .addAction(Action.PAN)
                    .build();

            return new NavigationTemplate.Builder()
                    .setActionStrip(actions)
                    .setMapActionStrip(mapActions)
                    .build();
        }

        @Override
        public void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer) {
            LogStore.i(
                    "CarTubeSurface",
                    "onSurfaceAvailable " + surfaceContainer.getWidth() + "x"
                            + surfaceContainer.getHeight() + " dpi=" + surfaceContainer.getDpi()
            );
            renderer.attach(surfaceContainer);
            LogStore.syncDriveBestEffort();
        }

        @Override
        public void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer) {
            LogStore.i("CarTubeSurface", "onSurfaceDestroyed");
            renderer.detach(surfaceContainer);
            LogStore.syncDriveBestEffort();
        }

        @Override
        public void onVisibleAreaChanged(@NonNull Rect visibleArea) {
            LogStore.i("CarTubeSurface", "visibleArea=" + visibleArea);
            renderer.setVisibleArea(visibleArea);
        }

        @Override
        public void onStableAreaChanged(@NonNull Rect stableArea) {
            LogStore.i("CarTubeSurface", "stableArea=" + stableArea);
            renderer.setStableArea(stableArea);
        }

        @Override
        public void onClick(float x, float y) {
            LogStore.i("CarTubeSurface", "click x=" + x + " y=" + y);
            renderer.setLastInteraction("tap", x, y);
            LogStore.syncDriveBestEffort();
        }

        @Override
        public void onScroll(float distanceX, float distanceY) {
            LogStore.i("CarTubeSurface", "scroll dx=" + distanceX + " dy=" + distanceY);
            renderer.setLastInteraction("scroll", distanceX, distanceY);
        }

        @Override
        public void onFling(float velocityX, float velocityY) {
            LogStore.i("CarTubeSurface", "fling vx=" + velocityX + " vy=" + velocityY);
            renderer.setLastInteraction("fling", velocityX, velocityY);
        }

        @Override
        public void onScale(float focusX, float focusY, float scaleFactor) {
            LogStore.i(
                    "CarTubeSurface",
                    "scale x=" + focusX + " y=" + focusY + " factor=" + scaleFactor
            );
            renderer.setLastInteraction("scale", focusX, focusY);
        }
    }
}
