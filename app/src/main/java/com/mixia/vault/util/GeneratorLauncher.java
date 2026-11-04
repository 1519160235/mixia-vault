package com.mixia.vault.util;

import android.app.Activity;

/** Thin wrapper so activities do not need to import the dialog directly. */
public final class GeneratorLauncher {
    private GeneratorLauncher() {
    }

    public static void show(Activity activity, String initial, GeneratorDialog.Listener listener) {
        GeneratorDialog.show(activity, initial, listener);
    }
}
