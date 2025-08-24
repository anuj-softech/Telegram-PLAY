package com.rock.tgplay.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Custom Button class for TV UI with focus change handling to update background and text colors.
 */
@SuppressLint("AppCompatCustomView")
public class TVButton extends Button {
    private ColorStateList xmlback;
    private ColorStateList xmltxtcolor;

    public TVButton(Context context) {
        super(context);
        init();
    }

    public TVButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TVButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public TVButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * Initializes the button by storing the initial background and text color states.
     */
    private void init() {
        xmlback = getBackgroundTintList();
        xmltxtcolor = getTextColors();
    }

    /**
     * Handles focus change events to update the button's background and text colors.
     *
     * @param focused  whether the button has focus or not
     * @param direction the direction focus has moved
     * @param previouslyFocusedRect the rectangle of the previously focused view
     */
    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) {
            setBackgroundTintList(ColorStateList.valueOf(-1)); // White background
            setTextColor(Color.BLACK); // Black text
        } else {
            setBackgroundTintList(xmlback); // Original background
            setTextColor(xmltxtcolor); // Original text color
        }
    }
}
