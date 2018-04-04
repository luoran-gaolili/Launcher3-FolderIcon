/**
 * Copyright (C) 2015 The Android Open Source Project
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

package com.rgks.launcher3.folder;

import android.content.Context;
import android.content.res.Resources;

public class StackFolderIconLayoutRule implements FolderIcon.PreviewLayoutRule {

    //modify by luoran for folder display 20170109(start)
    static int MAX_NUM_ITEMS_IN_PREVIEW = 4;
    private static final int MIN_NUM_ITEMS_IN_PREVIEW = 3;
    //modify by luoran for folder display 20170109(end)

    // The degree to which the item in the back of the stack is scaled [0...1]
    // (0 means it's not scaled at all, 1 means it's scaled to nothing)
    private static final float PERSPECTIVE_SCALE_FACTOR = 0.35f;

    // The amount of vertical spread between items in the stack [0...1]
    private static final float PERSPECTIVE_SHIFT_FACTOR = 0.18f;

    private float mBaselineIconScale;
    private int mBaselineIconSize;
    private int mAvailableSpaceInPreview;
    private float mMaxPerspectiveShift;
    //add by luoran for folder display 20170109(start)
    private Context context;
    public static int sAvailableIconSpace = -1;
    public boolean isRectFolderStyle;
    private FolderIcon folderIcon;

    public StackFolderIconLayoutRule(Context context,FolderIcon folderIcon) {
        this.context = context;
        this.folderIcon = folderIcon;
    }
    //modify by luoran for folder display 20170109(end)
    @Override
    public void init(int availableSpace, float intrinsicIconSize, boolean rtl) {
        mAvailableSpaceInPreview = availableSpace;
        //add by luoran for folder display 20170109(start)
        sAvailableIconSpace = context.getResources().getDimensionPixelSize(com.rgks.launcher3.R.dimen.folder_available_icon_space);
        isRectFolderStyle = context.getResources().getBoolean(com.rgks.launcher3.R.bool.is_rect_folder);
        if(isRectFolderStyle){
            MAX_NUM_ITEMS_IN_PREVIEW = 9;
        }else{
            MAX_NUM_ITEMS_IN_PREVIEW = 4;
        }
        //add by luoran for folder display 20170109(end)
        // cos(45) = 0.707  + ~= 0.1) = 0.8f
        int adjustedAvailableSpace = (int) ((mAvailableSpaceInPreview / 2) * (1 + 0.8f));

        int unscaledHeight = (int) (intrinsicIconSize * (1 + PERSPECTIVE_SHIFT_FACTOR));

        mBaselineIconScale = (1.0f * adjustedAvailableSpace / unscaledHeight);

        mBaselineIconSize = (int) (intrinsicIconSize * mBaselineIconScale);
        mMaxPerspectiveShift = mBaselineIconSize * PERSPECTIVE_SHIFT_FACTOR;
    }

    @Override
    public PreviewItemDrawingParams computePreviewItemDrawingParams(int index, int curNumItems,
            PreviewItemDrawingParams params) {
        //modify by luoran for folder display 20170109(start)
        if(!isRectFolderStyle) {
            float scale = scaleForItem(index, curNumItems);

            index = MAX_NUM_ITEMS_IN_PREVIEW - index - 1;
            float r = (index * 1.0f) / (MAX_NUM_ITEMS_IN_PREVIEW - 1);

            float offset = (1 - r) * mMaxPerspectiveShift;
            float scaledSize = scale * mBaselineIconSize;
            float scaleOffsetCorrection = (1 - scale) * mBaselineIconSize;

            // We want to imagine our coordinates from the bottom left, growing up and to the
            // right. This is natural for the x-axis, but for the y-axis, we have to invert things.
            float transY = mAvailableSpaceInPreview - (offset + scaledSize + scaleOffsetCorrection);
            float transX = (mAvailableSpaceInPreview - scaledSize) / 2;
            float totalScale = mBaselineIconScale * scale;
            final float overlayAlpha = (80 * (1 - r)) / 255f;

            if (params == null) {
                params = new PreviewItemDrawingParams(transX, transY, totalScale, overlayAlpha);
            } else {
                params.update(transX, transY, totalScale);
                params.overlayAlpha = overlayAlpha;
            }
            return params;
        }else{
            float transY;
            float transX;
            float totalScale;
            final int overlayAlpha;
            float mAvailableSpaceInFolderIcon = sAvailableIconSpace;
            float itemPadding = 2;
            float scaledSize = (mAvailableSpaceInFolderIcon - itemPadding * MIN_NUM_ITEMS_IN_PREVIEW) / MIN_NUM_ITEMS_IN_PREVIEW;
            float scale = scaledSize /folderIcon.mPreviewItemManager.getIntrinsicIconSize();
            float leftMargin = (mAvailableSpaceInPreview - mAvailableSpaceInFolderIcon) / 2;
            Resources res = context.getResources();
            int iconTopToFolderHeight = res.getDimensionPixelSize(com.rgks.launcher3.R.dimen.folder_available_icon_height);
            float topMarginY = (mAvailableSpaceInPreview - mAvailableSpaceInFolderIcon) / 2 + iconTopToFolderHeight;
            int column = index % MIN_NUM_ITEMS_IN_PREVIEW;
            int row = index / MIN_NUM_ITEMS_IN_PREVIEW;
            transX = leftMargin + scaledSize * column + itemPadding * column + 2;
            transY = topMarginY + scaledSize * row + itemPadding * row;
            totalScale = scale;
            overlayAlpha = 0;
            if (params == null) {
                params = new PreviewItemDrawingParams(transX, transY, totalScale, overlayAlpha);
            } else {
                params.transX = transX;
                params.transY = transY;
                params.scale = totalScale;
                params.overlayAlpha = overlayAlpha;
            }
            return params;
            //modify by luoran for folder display 20170109(end)
        }
    }

    @Override
    public int maxNumItems() {
        return MAX_NUM_ITEMS_IN_PREVIEW;
    }

    @Override
    public float getIconSize() {
        return mBaselineIconSize;
    }

    @Override
    public float scaleForItem(int index, int numItems) {
        // Scale is determined by the position of the icon in the preview.
        index = MAX_NUM_ITEMS_IN_PREVIEW - index - 1;
        float r = (index * 1.0f) / (MAX_NUM_ITEMS_IN_PREVIEW - 1);
        return (1 - PERSPECTIVE_SCALE_FACTOR * (1 - r));
    }

    @Override
    public boolean clipToBackground() {
        return false;
    }
    //modify by luoran for folder display 20170109(start)
    @Override
    public boolean hasEnterExitIndices() {
        return false;
    }
    //modify by luoran for folder display 20170109(end)
    @Override
    public int getExitIndex() {
        throw new RuntimeException("hasEnterExitIndices not supported");
    }

    @Override
    public int getEnterIndex() {
        throw new RuntimeException("hasEnterExitIndices not supported");
    }
}
