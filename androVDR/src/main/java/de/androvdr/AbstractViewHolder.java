package de.androvdr;

import java.lang.reflect.Field;

import android.util.TypedValue;
import android.widget.TextView;

public abstract class AbstractViewHolder {

	public void setTextSize(int textSizeOffset, float scaledDensity) {
		if (textSizeOffset == 0)
			return;

		for (Field field : this.getClass().getDeclaredFields()) {
			if (field.getType() == TextView.class) {
				try {
					TextView tv = (TextView) field.get(this);
					if (tv != null)
						tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
								(tv.getTextSize() / scaledDensity) + textSizeOffset);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}

	}
}
