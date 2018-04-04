package com.rgks.launcher3.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.os.Process;
import android.provider.BaseColumns;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.rgks.launcher3.IconCache;
import com.rgks.launcher3.InvariantDeviceProfile;
import com.rgks.launcher3.ItemInfo;
import com.rgks.launcher3.LauncherAppState;
import com.rgks.launcher3.ShortcutInfo;
import com.rgks.launcher3.Utilities;
import com.rgks.launcher3.compat.LauncherAppsCompat;
import com.rgks.launcher3.LauncherSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LoaderCursor}
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class LoaderCursorTest {

    private LauncherAppState mMockApp;
    private IconCache mMockIconCache;

    private MatrixCursor mCursor;
    private InvariantDeviceProfile mIDP;
    private Context mContext;
    private LauncherAppsCompat mLauncherApps;

    private LoaderCursor mLoaderCursor;

    @Before
    public void setup() {
        mIDP = new InvariantDeviceProfile();
        mCursor = new MatrixCursor(new String[] {
                LauncherSettings.BaseLauncherColumns.ICON, LauncherSettings.BaseLauncherColumns.ICON_PACKAGE, LauncherSettings.BaseLauncherColumns.ICON_RESOURCE, LauncherSettings.BaseLauncherColumns.TITLE,
                BaseColumns._ID, LauncherSettings.Favorites.CONTAINER, LauncherSettings.BaseLauncherColumns.ITEM_TYPE, LauncherSettings.Favorites.PROFILE_ID,
                LauncherSettings.Favorites.SCREEN, LauncherSettings.Favorites.CELLX, LauncherSettings.Favorites.CELLY, LauncherSettings.Favorites.RESTORED, LauncherSettings.BaseLauncherColumns.INTENT
        });
        mContext = InstrumentationRegistry.getTargetContext();

        mMockApp = mock(LauncherAppState.class);
        mMockIconCache = mock(IconCache.class);
        when(mMockApp.getIconCache()).thenReturn(mMockIconCache);
        when(mMockApp.getInvariantDeviceProfile()).thenReturn(mIDP);
        when(mMockApp.getContext()).thenReturn(mContext);
        mLauncherApps = LauncherAppsCompat.getInstance(mContext);

        mLoaderCursor = new LoaderCursor(mCursor, mMockApp);
        mLoaderCursor.allUsers.put(0, Process.myUserHandle());
    }

    private void initCursor(int itemType, String title) {
        mCursor.newRow()
                .add(BaseColumns._ID, 1)
                .add(LauncherSettings.Favorites.PROFILE_ID, 0)
                .add(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, itemType)
                .add(LauncherSettings.BaseLauncherColumns.TITLE, title)
                .add(LauncherSettings.Favorites.CONTAINER, LauncherSettings.Favorites.CONTAINER_DESKTOP);
    }

    @Test
    public void getAppShortcutInfo_dontAllowMissing_invalidComponent() {
        initCursor(LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION, "");
        assertTrue(mLoaderCursor.moveToNext());
        ComponentName cn = new ComponentName(mContext.getPackageName(), "dummy-do");
        assertNull(mLoaderCursor.getAppShortcutInfo(
                new Intent().setComponent(cn), false /* allowMissingTarget */, true));
    }

    @Test
    public void getAppShortcutInfo_dontAllowMissing_validComponent() {
        initCursor(LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION, "");
        assertTrue(mLoaderCursor.moveToNext());

        ComponentName cn = mLauncherApps.getActivityList(null, mLoaderCursor.user)
                .get(0).getComponentName();
        ShortcutInfo info = mLoaderCursor.getAppShortcutInfo(
                new Intent().setComponent(cn), false /* allowMissingTarget */, true);
        assertNotNull(info);
        assertTrue(Utilities.isLauncherAppTarget(info.intent));
    }

    @Test
    public void getAppShortcutInfo_allowMissing_invalidComponent() {
        initCursor(LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION, "");
        assertTrue(mLoaderCursor.moveToNext());

        ComponentName cn = new ComponentName(mContext.getPackageName(), "dummy-do");
        ShortcutInfo info = mLoaderCursor.getAppShortcutInfo(
                new Intent().setComponent(cn), true  /* allowMissingTarget */, true);
        assertNotNull(info);
        assertTrue(Utilities.isLauncherAppTarget(info.intent));
    }

    @Test
    public void loadSimpleShortcut() {
        initCursor(LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT, "my-shortcut");
        assertTrue(mLoaderCursor.moveToNext());

        Bitmap icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
        when(mMockIconCache.getDefaultIcon(eq(mLoaderCursor.user))).thenReturn(icon);
        ShortcutInfo info = mLoaderCursor.loadSimpleShortcut();
        assertEquals(icon, info.iconBitmap);
        assertEquals("my-shortcut", info.title);
        assertEquals(LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT, info.itemType);
    }

    @Test
    public void checkItemPlacement_wrongWorkspaceScreen() {
        ArrayList<Long> workspaceScreens = new ArrayList<>(Arrays.asList(1L, 3L));
        mIDP.numRows = 4;
        mIDP.numColumns = 4;
        mIDP.numHotseatIcons = 3;

        // Item on unknown screen are not placed
        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, LauncherSettings.Favorites.CONTAINER_DESKTOP, 4L), workspaceScreens));
        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, LauncherSettings.Favorites.CONTAINER_DESKTOP, 5L), workspaceScreens));
        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, LauncherSettings.Favorites.CONTAINER_DESKTOP, 2L), workspaceScreens));

        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, LauncherSettings.Favorites.CONTAINER_DESKTOP, 1L), workspaceScreens));
        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, LauncherSettings.Favorites.CONTAINER_DESKTOP, 3L), workspaceScreens));

    }
    @Test
    public void checkItemPlacement_outsideBounds() {
        ArrayList<Long> workspaceScreens = new ArrayList<>(Arrays.asList(1L, 2L));
        mIDP.numRows = 4;
        mIDP.numColumns = 4;
        mIDP.numHotseatIcons = 3;

        // Item outside screen bounds are not placed
        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(4, 4, 1, 1, LauncherSettings.Favorites.CONTAINER_DESKTOP, 1L), workspaceScreens));
    }

    @Test
    public void checkItemPlacement_overlappingItems() {
        ArrayList<Long> workspaceScreens = new ArrayList<>(Arrays.asList(1L, 2L));
        mIDP.numRows = 4;
        mIDP.numColumns = 4;
        mIDP.numHotseatIcons = 3;

        // Overlapping items are not placed
        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, LauncherSettings.Favorites.CONTAINER_DESKTOP, 1L), workspaceScreens));
        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, LauncherSettings.Favorites.CONTAINER_DESKTOP, 1L), workspaceScreens));

        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, LauncherSettings.Favorites.CONTAINER_DESKTOP, 2L), workspaceScreens));
        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, LauncherSettings.Favorites.CONTAINER_DESKTOP, 2L), workspaceScreens));

        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(1, 1, 1, 1, LauncherSettings.Favorites.CONTAINER_DESKTOP, 1L), workspaceScreens));
        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(2, 2, 2, 2, LauncherSettings.Favorites.CONTAINER_DESKTOP, 1L), workspaceScreens));

        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(3, 2, 1, 2, LauncherSettings.Favorites.CONTAINER_DESKTOP, 1L), workspaceScreens));
    }

    @Test
    public void checkItemPlacement_hotseat() {
        ArrayList<Long> workspaceScreens = new ArrayList<>();
        mIDP.numRows = 4;
        mIDP.numColumns = 4;
        mIDP.numHotseatIcons = 3;

        // Hotseat items are only placed based on screenId
        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(3, 3, 1, 1, LauncherSettings.Favorites.CONTAINER_HOTSEAT, 1L), workspaceScreens));
        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(3, 3, 1, 1, LauncherSettings.Favorites.CONTAINER_HOTSEAT, 2L), workspaceScreens));

        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(3, 3, 1, 1, LauncherSettings.Favorites.CONTAINER_HOTSEAT, 3L), workspaceScreens));
    }

    private ItemInfo newItemInfo(int cellX, int cellY, int spanX, int spanY,
            long container, long screenId) {
        ItemInfo info = new ItemInfo();
        info.cellX = cellX;
        info.cellY = cellY;
        info.spanX = spanX;
        info.spanY = spanY;
        info.container = container;
        info.screenId = screenId;
        return info;
    }
}
