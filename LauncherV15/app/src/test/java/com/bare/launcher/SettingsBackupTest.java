package com.bare.launcher;

import static org.junit.Assert.*;
import org.junit.Test;

public class SettingsBackupTest {
    @Test public void appOptionsRoundTripIndependently() {
        for (boolean alphabetical : new boolean[] { false, true }) {
            for (boolean homeRow : new boolean[] { false, true }) {
                SettingsBackup.Parsed p = SettingsBackup.parse(SettingsBackup.serialize(
                        "z,a", 4, "", "hidden", 0, alphabetical, homeRow));
                assertNotNull(p);
                assertEquals(alphabetical ? 1 : 0, p.intVal(SettingsBackup.K_ALPHABETICAL, -1));
                assertEquals(homeRow ? 1 : 0, p.intVal(SettingsBackup.K_HOME_ROW, -1));
                assertEquals("z,a", p.str(SettingsBackup.K_APP_ORDER));
                assertEquals(4, p.intVal(SettingsBackup.K_HOME_COUNT, -1));
            }
        }
    }

    @Test public void oldBackupLeavesNewOptionsUnspecified() {
        SettingsBackup.Parsed p = SettingsBackup.parse("BLBK\t1\napp_order\tz,a\nhome_count\t4\n");
        assertNotNull(p);
        assertFalse(p.has(SettingsBackup.K_ALPHABETICAL));
        assertFalse(p.has(SettingsBackup.K_HOME_ROW));
        assertEquals("z,a", p.str(SettingsBackup.K_APP_ORDER));
    }
}
