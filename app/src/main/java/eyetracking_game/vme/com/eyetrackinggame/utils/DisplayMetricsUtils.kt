package eyetracking_game.vme.com.eyetrackinggame.utils

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue

/**
 * Created by vmerlet on 26/10/2017.
 *
 * Conversion px->dp et inversement
 */
object DisplayMetricsUtils {

    /**
     * Conversion pixels to dp
     *
     * @param pixels
     *
     * @return int : la valeur en dip des pixels passés en paramètre
     */
    fun convertPxToDp(pixels: Int): Int {
        val displaymetrics = DisplayMetrics()

        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixels.toFloat(), displaymetrics).toInt()
    }

    /**
     * Conversion dip to pixels
     *
     * @param context
     * @param dipValue
     *
     * @return float
     */
    fun dipToPixels(context: Context, dipValue: Float): Float {
        val metrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics)
    }

    /**
     * Récupère la largeur de l'écran
     *
     * @param activity
     *
     * @return int
     */
    fun getWidth(activity: Activity): Int {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    /**
     * Récupère la hauteur de l'écran
     *
     * @param activity
     *
     * @return int
     */
    fun getHeight(activity: Activity): Int {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }
}
