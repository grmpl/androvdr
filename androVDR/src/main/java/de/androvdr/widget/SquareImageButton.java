package de.androvdr.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TableLayout;

public class SquareImageButton extends ImageButton {
	private int maxHeight;

	public SquareImageButton(Context context) {
		super(context);
	}

	public SquareImageButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SquareImageButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * @see android.view.View#measure(int, int)
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);
		int rowCount = -1;

		if (width == 0)
			width = getSuggestedMinimumWidth();
		if (height > maxHeight)
			maxHeight = height;

		if (height == 0)
			height = getSuggestedMinimumHeight();

		ViewGroup parent = (ViewGroup) getParent().getParent();
		if (parent != null && parent instanceof TableLayout) {
			TableLayout tl = (TableLayout) parent;
			rowCount = tl.getChildCount();
		}

		if (rowCount > 0 && height * rowCount > maxHeight) {
			width = maxHeight / rowCount;
		} else
			width = height;

		super.onMeasure(
				MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
	}
}
