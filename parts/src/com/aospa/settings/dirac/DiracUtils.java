/*
 * Copyright (C) 2022 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aospa.settings.dirac;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.UserHandle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import java.lang.IllegalArgumentException;
import java.util.List;

public final class DiracUtils {

    protected DiracSound mDiracSound;
    private static DiracUtils mInstance;
    private MediaSessionManager mMediaSessionManager;
    private Handler mHandler = new Handler();
    private Context mContext;

    public static DiracUtils getInstance() {
        if (mInstance == null) {
            throw new IllegalArgumentException("Trying to get instance without initializing!");
        }
        return mInstance;
    }

    public DiracUtils(final Context context) {
        mContext = context;
        mMediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        mDiracSound = new DiracSound(0, 0);
    }

    public void onBootCompleted() {
        mInstance = this;
    }

    protected void refreshPlaybackIfNecessary(){
        if (mMediaSessionManager == null) {
            mMediaSessionManager = (MediaSessionManager) mContext.getSystemService(Context.MEDIA_SESSION_SERVICE);
        }
        final List<MediaController> sessions
                = mMediaSessionManager.getActiveSessionsForUser(
                null, UserHandle.ALL);
        for (MediaController aController : sessions) {
            if (PlaybackState.STATE_PLAYING ==
                    getMediaControllerPlaybackState(aController)) {
                triggerPlayPause(aController);
                break;
            }
        }
    }

    private void triggerPlayPause(MediaController controller) {
        long when = SystemClock.uptimeMillis();
        final KeyEvent evDownPause = new KeyEvent(when, when, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0);
        final KeyEvent evUpPause = KeyEvent.changeAction(evDownPause, KeyEvent.ACTION_UP);
        final KeyEvent evDownPlay = new KeyEvent(when, when, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
        final KeyEvent evUpPlay = KeyEvent.changeAction(evDownPlay, KeyEvent.ACTION_UP);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                controller.dispatchMediaButtonEvent(evDownPause);
            }
        });
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                controller.dispatchMediaButtonEvent(evUpPause);
            }
        }, 20);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                controller.dispatchMediaButtonEvent(evDownPlay);
            }
        }, 1000);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                controller.dispatchMediaButtonEvent(evUpPlay);
            }
        }, 1020);
    }

    private int getMediaControllerPlaybackState(MediaController controller) {
        if (controller != null) {
            final PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState != null) {
                return playbackState.getState();
            }
        }
        return PlaybackState.STATE_NONE;
    }
    public void setEnabled(boolean enable) {
        mDiracSound.setEnabled(enable);
        mDiracSound.setMusic(enable ? 1 : 0);
        if (enable) {
            refreshPlaybackIfNecessary();
        }
    }

    public boolean isDiracEnabled() {
        return mDiracSound.getMusic() == 1;
    }

    public void setLevel(String preset) {
        String[] level = preset.split("\\s*,\\s*");

        for (int band = 0; band <= level.length - 1; band++) {
            mDiracSound.setLevel(band, Float.valueOf(level[band]));
        }
    }

    public void setHeadsetType(int paramInt) {
         mDiracSound.setHeadsetType(paramInt);
    }
}
