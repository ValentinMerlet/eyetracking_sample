package eyetracking_game.vme.com.eyetrackinggame.views;

/**
 * Created by vmerlet on 29/11/2017.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by vmerlet on 28/11/2017.
 */
public class CircleView extends View {

    int mColor = Color.BLUE;

    /**
     * @param context
     */
    public CircleView(Context context) {
        super(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public CircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Modification de la couleur du cercle
     *
     * @param color
     */
    public void setColor(int color) {
        mColor = color;
        invalidate();
    }

    /**
     * Au rendu de la vue
     *
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // custom drawing code here
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        // draw blue circle with anti aliasing turned on
        paint.setAntiAlias(true);
        paint.setColor(mColor);
        canvas.drawCircle(100, 100, 100, paint);
    }
}

