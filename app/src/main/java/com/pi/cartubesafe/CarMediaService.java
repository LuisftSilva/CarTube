package com.pi.cartubesafe;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.media.MediaBrowserServiceCompat;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.Collections;
import java.util.List;

public final class CarMediaService extends MediaBrowserServiceCompat {
    private static final String ROOT_ID = "cartube_root";
    private static final String SAFE_ITEM_ID = "safe_driving_mode";

    private MediaSessionCompat session;
    private Bitmap artwork;

    @Override
    public void onCreate() {
        super.onCreate();
        LogStore.init(this);
        LogStore.i("CarMediaService", "MediaBrowserServiceCompat created");

        artwork = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

        session = new MediaSessionCompat(this, "CarTubeSession");
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                LogStore.i("CarMediaService", "onPlay from host");
                showDrivingSafeState();
            }

            @Override
            public void onPlayFromMediaId(String mediaId, Bundle extras) {
                LogStore.i("CarMediaService", "onPlayFromMediaId: " + mediaId);
                showDrivingSafeState();
            }

            @Override
            public void onPause() {
                LogStore.i("CarMediaService", "onPause from host");
                showDrivingSafeState();
            }

            @Override
            public void onStop() {
                LogStore.i("CarMediaService", "onStop from host");
                setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                LogStore.syncDriveBestEffort();
            }
        });

        session.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                        | PlaybackStateCompat.ACTION_STOP)
                .build());
        session.setActive(true);
        setSessionToken(session.getSessionToken());
        showDrivingSafeState();
    }

    private void setPlaybackState(int stateValue) {
        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setState(stateValue, 0L, 0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                        | PlaybackStateCompat.ACTION_STOP)
                .build();
        session.setPlaybackState(state);
    }

    private void showDrivingSafeState() {
        MediaMetadataCompat.Builder metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, SAFE_ITEM_ID)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "CarTube")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Modo de condução")
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "Vídeo disponível quando estacionado");
        if (artwork != null) {
            metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, artwork);
            metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork);
        }
        session.setMetadata(metadata.build());

        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        LogStore.i("CarMediaService", "Safe driving media state ready without playback error");
        LogStore.syncDriveBestEffort();
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        LogStore.i("CarMediaService", "onGetRoot client=" + clientPackageName + " uid=" + clientUid + " hints=" + String.valueOf(rootHints));
        LogStore.syncDriveBestEffort();
        return new BrowserRoot(ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaBrowserCompat.MediaItem>> result) {
        LogStore.i("CarMediaService", "onLoadChildren parent=" + parentId);
        if (!ROOT_ID.equals(parentId)) {
            result.sendResult(Collections.emptyList());
            LogStore.syncDriveBestEffort();
            return;
        }

        MediaDescriptionCompat.Builder description = new MediaDescriptionCompat.Builder()
                .setMediaId(SAFE_ITEM_ID)
                .setTitle("CarTube")
                .setSubtitle("Modo de condução seguro")
                .setDescription("O vídeo fica disponível quando o veículo estiver estacionado.");
        if (artwork != null) {
            description.setIconBitmap(artwork);
        }

        MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(
                description.build(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        );
        result.sendResult(Collections.singletonList(item));
        LogStore.i("CarMediaService", "Returned 1 playable root item");
        LogStore.syncDriveBestEffort();
    }

    @Override
    public void onDestroy() {
        LogStore.i("CarMediaService", "Media service destroyed");
        LogStore.syncDriveBestEffort();
        if (session != null) {
            session.release();
            session = null;
        }
        artwork = null;
        super.onDestroy();
    }
}
