package com.rgks.launcher3.folder;


import android.content.Context;
import android.content.res.Resources;
import com.rgks.launcher3.R;
public class ClippedFolderIconLayoutRule implements FolderIcon.PreviewLayoutRule {

    //modify by luoran for folder display 20170109(start)
    static int MAX_NUM_ITEMS_IN_PREVIEW = 4;
    private static final int MIN_NUM_ITEMS_IN_PREVIEW = 3;
    //modify by luoran for folder display 20170109(end)

    private static final float MIN_SCALE = 0.48f;
    private static final float MAX_SCALE = 0.58f;
    private static final float MAX_RADIUS_DILATION = 0.15f;
    private static final float ITEM_RADIUS_SCALE_FACTOR = 1.33f;

    private static final int EXIT_INDEX = -2;
    private static final int ENTER_INDEX = -3;

    private float[] mTmpPoint = new float[2];

    private float mAvailableSpace;
    private float mRadius;
    private float mIconSize;
    private boolean mIsRtl;
    private float mBaselineIconScale;
    //add by luoran for folder display 20170109(start)
    private Context context;
    public static int sAvailableIconSpace = -1;
    public boolean isRectFolderStyle;
    private FolderIcon folderIcon;

    public ClippedFolderIconLayoutRule(Context context,FolderIcon folderIcon) {
        this.context = context;
        this.folderIcon = folderIcon;
    }
    //modify by luoran for folder display 20170109(end)
    @Override
    public void init(int availableSpace, float intrinsicIconSize, boolean rtl) {
        mAvailableSpace = availableSpace;
        //add by luoran for folder display 20170109(start)
        sAvailableIconSpace = context.getResources().getDimensionPixelSize(R.dimen.folder_available_icon_space);
        isRectFolderStyle = context.getResources().getBoolean(R.bool.is_rect_folder);
        if(isRectFolderStyle){
            MAX_NUM_ITEMS_IN_PREVIEW = 9;
        }else{
            MAX_NUM_ITEMS_IN_PREVIEW = 4;
        }
        //add by luoran for folder display 20170109(end)
        mRadius = ITEM_RADIUS_SCALE_FACTOR * availableSpace / 2f;
        mIconSize = intrinsicIconSize;
        mIsRtl = rtl;
        mBaselineIconScale = availableSpace / (intrinsicIconSize * 1f);
    }

    @Override
    public PreviewItemDrawingParams computePreviewItemDrawingParams(int index, int curNumItems,
            PreviewItemDrawingParams params) {
        //modify by luoran for folder display 20170109(start)
        if(!isRectFolderStyle) {
            float totalScale = scaleForItem(index, curNumItems);
            float transX;
            float transY;
            float overlayAlpha = 0;

        if (index == getExitIndex()) {
            // 0 1 * <-- Exit position (row 0, col 2)
            // 2 3
            getGridPosition(0, 2, mTmpPoint);
        } else if (index == getEnterIndex()) {
            // 0 1
            // 2 3 * <-- Enter position (row 1, col 2)
            getGridPosition(1, 2, mTmpPoint);
        } else if (index >= MAX_NUM_ITEMS_IN_PREVIEW) {
            // Items beyond those displayed in the preview are animated to the center
            mTmpPoint[0] = mTmpPoint[1] = mAvailableSpace / 2 - (mIconSize * totalScale) / 2;
        } else {
            getPosition(index, curNumItems, mTmpPoint);
        }

        transX = mTmpPoint[0];
        transY = mTmpPoint[1];

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
            float leftMargin = (mAvailableSpace - mAvailableSpaceInFolderIcon) / 2;
            Resources res = context.getResources();
            int iconTopToFolderHeight = res.getDimensionPixelSize(R.dimen.folder_available_icon_height);
            float topMarginY = (mAvailableSpace - mAvailableSpaceInFolderIcon) / 2 + iconTopToFolderHeight;
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

    /**
     * Builds a grid based on the positioning of the items when there are
     * {@link #MAX_NUM_ITEMS_IN_PREVIEW} in the preview.
     *
     * Positions in the grid: 0 1  // 0 is row 0, col 1
     *                        2 3  // 3 is row 1, col 1
     */
    private void getGridPosition(int row, int col, float[] result) {
        // We use position 0 and 3 to calculate the x and y distances between items.
        getPosition(0, 4, result);
        float left = result[0];
        float top = result[1];

        getPosition(3, 4, result);
        float dx = result[0] - left;
        float dy = result[1] - top;

        result[0] = left + (col * dx);
        result[1] = top + (row * dy);
    }

    private void getPosition(int index, int curNumItems, float[] result) {
        // The case of two items is homomorphic to the case of one.
        curNumItems = Math.max(curNumItems, 2);

        // We model the preview as a circle of items starting in the appropriate piece of the
        // upper left quadrant (to achieve horizontal and vertical symmetry).
        double theta0 = mIsRtl ? 0 : Math.PI;

        // In RTL we go counterclockwise
        int direction = mIsRtl ? 1 : -1;

        double thetaShift = 0;
        if (curNumItems == 3) {
            thetaShift = Math.PI / 6;
        } else if (curNumItems == 4) {
            thetaShift = Math.PI / 4;
        }
        theta0 += direction * thetaShift;

        // We want the items to appear in reading order. For the case of 1, 2 and 3 items, this
        // is natural for the circular model. With 4 items, however, we need to swap the 3rd and
        // 4th indices to achieve reading order.
        if (curNumItems == 4 && index == 3) {
            index = 2;
        } else if (curNumItems == 4 && index == 2) {
            index = 3;
        }

        // We bump the radius up between 0 and MAX_RADIUS_DILATION % as the number of items increase
        float radius = mRadius * (1 + MAX_RADIUS_DILATION * (curNumItems -
                MIN_NUM_ITEMS_IN_PREVIEW) / (MAX_NUM_ITEMS_IN_PREVIEW - MIN_NUM_ITEMS_IN_PREVIEW));
        double theta = theta0 + index * (2 * Math.PI / curNumItems) * direction;

        float halfIconSize = (mIconSize * scaleForItem(index, curNumItems)) / 2;

        // Map the location along the circle, and offset the coordinates to represent the center
        // of the icon, and to be based from the top / left of the preview area. The y component
        // is inverted to match the coordinate system.
        result[0] = mAvailableSpace / 2 + (float) (radius * Math.cos(theta) / 2) - halfIconSize;
        result[1] = mAvailableSpace / 2 + (float) (- radius * Math.sin(theta) / 2) - halfIconSize;

    }

    @Override
    public float scaleForItem(int index, int numItems) {
        // Scale is determined by the number of items in the preview.
        float scale = 1f;
        if (numItems <= 2) {
            scale = MAX_SCALE;
        } else if (numItems == 3) {
            scale = (MAX_SCALE + MIN_SCALE) / 2;
        } else {
            scale = MIN_SCALE;
        }

        return scale * mBaselineIconScale;
    }

    @Override
    public float getIconSize() {
        return mIconSize;
    }

    @Override
    public int maxNumItems() {
        return MAX_NUM_ITEMS_IN_PREVIEW;
    }

    @Override
    public boolean clipToBackground() {
        return true;
    }

    @Override
    public boolean hasEnterExitIndices() {
        return false;
    }

    @Override
    public int getExitIndex() {
        return EXIT_INDEX;
    }

    @Override
    public int getEnterIndex() {
        return ENTER_INDEX;
    }
}
