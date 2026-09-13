package com.bare.launcher;

import static org.junit.Assert.*;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.content.SharedPreferences;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LauncherAppOptionsTest {
    private static Object field(Object target, String name) {
        try {
            Field f = LauncherActivity.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void call(LauncherActivity a, String name, Class<?>[] types, Object... args) {
        try {
            Method m = LauncherActivity.class.getDeclaredMethod(name, types);
            m.setAccessible(true);
            m.invoke(a, args);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test public void alphabeticalIncludesNewAppsAndManualOrderCanBeRestored() {
        try (ActivityScenario<LauncherActivity> scenario = ActivityScenario.launch(LauncherActivity.class)) {
            scenario.onActivity(a -> {
                SharedPreferences prefs = (SharedPreferences) field(a, "prefs");
                String originalOrder = prefs.getString("app_order", null);
                boolean originalAlpha = prefs.getBoolean("always_alphabetical", false);
                try {
                    prefs.edit().putString("app_order", "z,b").putBoolean("always_alphabetical", true).apply();
                    AppInfo z = new AppInfo("z", "Zulu", null, null);
                    AppInfo b = new AppInfo("b", "bravo", null, null);
                    AppInfo added = new AppInfo("new", "Alpha", null, null);
                    List<AppInfo> apps = new ArrayList<>(Arrays.asList(z, b, added));
                    call(a, "applyStoredOrder", new Class<?>[] { List.class }, apps);
                    assertEquals(Arrays.asList(added, b, z), apps);
                    call(a, "saveOrder", new Class<?>[0]);
                    assertEquals("z,b", prefs.getString("app_order", null));
                    prefs.edit().putBoolean("always_alphabetical", false).apply();
                    call(a, "applyStoredOrder", new Class<?>[] { List.class }, apps);
                    assertEquals(Arrays.asList(z, b, added), apps);
                } finally {
                    prefs.edit().putString("app_order", originalOrder)
                            .putBoolean("always_alphabetical", originalAlpha).apply();
                }
            });
        }
    }

    @Test public void noHomeRowKeepsAppsHiddenUntilDown() {
        try (ActivityScenario<LauncherActivity> scenario = ActivityScenario.launch(LauncherActivity.class)) {
            final boolean[] original = new boolean[1];
            scenario.onActivity(a -> {
                SharedPreferences prefs = (SharedPreferences) field(a, "prefs");
                original[0] = prefs.getBoolean("show_home_row", true);
                prefs.edit().putBoolean("show_home_row", true).apply();
                call(a, "showSettingsPanel", new Class<?>[0]);
                call(a, "activateSettingsRowId", new Class<?>[] { int.class }, field(a, "SR_HOME_ROW"));
            });
            getInstrumentation().waitForIdleSync();
            scenario.onActivity(a -> {
                try {
                    assertEquals(View.GONE, ((View) field(a, "drawer")).getVisibility());
                    assertEquals(View.INVISIBLE, ((View) field(a, "shelf")).getVisibility());
                    assertTrue(((View) field(a, "settingsOverlay")).hasFocus());
                    // Hide the modal synchronously so DOWN reaches home navigation.
                    ((View) field(a, "settingsOverlay")).setVisibility(View.GONE);
                    ((View) field(a, "mapperBtnView")).requestFocus();
                    a.dispatchKeyEvent(new android.view.KeyEvent(
                            android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_DOWN));
                    a.dispatchKeyEvent(new android.view.KeyEvent(
                            android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DPAD_DOWN));
                    LauncherActivity.AppDrawer drawer = (LauncherActivity.AppDrawer) field(a, "drawer");
                    assertEquals(View.VISIBLE, drawer.getVisibility());
                    call(a, "closeDrawer", new Class<?>[0]);
                    assertTrue("Back starts closing the app grid", drawer.closing);
                    assertTrue(((View) field(a, "netBtn")).hasFocus());
                    call(a, "activateSettingsRowId", new Class<?>[] { int.class }, field(a, "SR_HOME_ROW"));
                    assertEquals(View.GONE, ((View) field(a, "drawer")).getVisibility());
                    assertEquals(View.VISIBLE, ((View) field(a, "shelf")).getVisibility());
                } finally {
                    ((SharedPreferences) field(a, "prefs")).edit()
                            .putBoolean("show_home_row", original[0]).apply();
                }
            });
        }
    }
}
