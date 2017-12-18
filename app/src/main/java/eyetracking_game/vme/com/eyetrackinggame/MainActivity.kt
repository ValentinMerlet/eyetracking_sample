package eyetracking_game.vme.com.eyetrackinggame

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.WindowManager
import eyetracking_game.vme.com.eyetrackinggame.utils.DisplayMetricsUtils
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import org.opencv.objdetect.Objdetect
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(), CvCameraViewListener2 {

    private val TAG: String = "EyeActivity"
    private val LBP_FRONTALFACE_FILENAME: String = "lbpcascade_frontalface_improved.xml"
    private val HAAR_LEFT_EYE_FILENAME: String = "haarcascade_lefteye_2splits.xml"
    private val HAAR_RIGHT_EYE_FILENAME: String = "haarcascade_righteye_2splits.xml"
    private lateinit var mJavaDetector: CascadeClassifier
    private lateinit var mJavaDetectorLeftEye: CascadeClassifier
    private lateinit var mJavaDetectorRightEye: CascadeClassifier
    private lateinit var mOpenCvCameraView: JavaCameraView
    private lateinit var mRgba: Mat
    private lateinit var mGray: Mat
    private var mAbsoluteFaceSize = 0
    private var mRelativeFaceSize = 0.2f
    private val THICKNESS_PUPIL_CIRCLE = 5
    private val mPointsLeftEye = LinkedList<Point>()
    private val mPointsRightEye = LinkedList<Point>()
    private var mControl: Boolean = true
    private var listAverageXEyePosition: MutableList<Int> = ArrayList()
    // Objet permettant l'animation du cercle
    private var mAnimator: ViewPropertyAnimator? = null
    // Durée de l'animation du cercle, en millisecondes
    private val ANIMATION_CIRCLE_DURATION = 100
    private var mStopRecording: Boolean = false
    private var mCanStartAnimation: Boolean = true
    private var mMoveLeft: Boolean = false
    private var mMoveRight: Boolean = false

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")

                    setUpFile(R.raw.lbpcascade_frontalface, LBP_FRONTALFACE_FILENAME)
                    setUpFile(R.raw.haarcascade_lefteye_2splits, HAAR_LEFT_EYE_FILENAME)
                    setUpFile(R.raw.haarcascade_righteye_2splits, HAAR_RIGHT_EYE_FILENAME)

                    mOpenCvCameraView.enableFpsMeter()
                    mOpenCvCameraView.setCameraIndex(1)
                    mOpenCvCameraView.enableView()
                    mOpenCvCameraView.SetCaptureFormat(CameraBridgeViewBase.GRAY)
                }
                else -> super.onManagerConnected(status)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mOpenCvCameraView = findViewById<View>(R.id.java_camera_view_activity_eye) as JavaCameraView
        mOpenCvCameraView.setCvCameraViewListener(this)
        mOpenCvCameraView.visibility = SurfaceView.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView.disableView()
    }

    fun setUpFile(resourceId: Int, resourceName: String) {

        try {
            val buffer = ByteArray(4096)

            // load cascade file from application resources
            val ise = resources.openRawResource(resourceId)
            var bytesRead: Int = ise.read(buffer)
            val cascadeDirEye = getDir("cascade", Context.MODE_PRIVATE)
            val cascadeFile = File(cascadeDirEye, resourceName)
            val ose = FileOutputStream(cascadeFile)

            while (bytesRead != -1) {
                ose.write(buffer, 0, bytesRead)
                bytesRead = ise.read(buffer)
            }
            ise.close()
            ose.close()

            if (resourceName == LBP_FRONTALFACE_FILENAME) {
                mJavaDetector = CascadeClassifier(cascadeFile.getAbsolutePath())
                if (mJavaDetector.empty()) {
                    Log.e(TAG, "Failed to load cascade classifier")
                } else {
                    Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath())
                }
            } else if (resourceName == HAAR_LEFT_EYE_FILENAME) {
                mJavaDetectorLeftEye = CascadeClassifier(cascadeFile.getAbsolutePath())
                if (mJavaDetectorLeftEye.empty()) {
                    Log.e(TAG, "Failed to load cascade classifier for eye")
                } else {
                    Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath())
                }
            } else if (resourceName == HAAR_RIGHT_EYE_FILENAME) {
                mJavaDetectorRightEye = CascadeClassifier(cascadeFile.getAbsolutePath())
                if (mJavaDetectorRightEye.empty()) {
                    Log.e(TAG, "Failed to load cascade classifier for eye")
                } else {
                    Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath())
                }
            }

            cascadeFile.delete()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e)
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mGray = Mat()
        mRgba = Mat()
    }

    override fun onCameraViewStopped() {
        mGray.release()
        mRgba.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        mRgba = inputFrame.rgba()
        mGray = inputFrame.gray()

        if (mAbsoluteFaceSize == 0) {
            val height = mGray.rows()
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize)
            }
        }

        val faces = MatOfRect()

        mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2,
                Size(mAbsoluteFaceSize.toDouble(), mAbsoluteFaceSize.toDouble()), Size())

        val facesArray = faces.toArray()

        for (i in facesArray.indices) {

            val r = facesArray[i]

            val leftEyeArea = Rect(r.x + r.width / 16
                    + (r.width - 2 * r.width / 16) / 2,
                    (r.y + r.height / 4.5).toInt(),
                    (r.width - 2 * r.width / 16) / 2, (r.height / 3.0).toInt())

            getTemplate(mJavaDetectorLeftEye, leftEyeArea, "left")

            val rightEyeArea = Rect(r.x + r.width / 16,
                    (r.y + r.height / 4.5).toInt(),
                    (r.width - 2 * r.width / 16) / 2, (r.height / 3.0).toInt())
            getTemplate(mJavaDetectorRightEye, rightEyeArea, "right")
        }

        return mRgba
    }

    /**
     * @param clasificator : permet de reconnaître des formes sur une image
     * @param area         : la zone de l'oeil sur l'écran
     * @param eye          : left or right pour oeil gauche ou droit
     */
    private fun getTemplate(clasificator: CascadeClassifier, area: Rect, eye: String) {
        var roi = mGray.submat(area)
        val eyes = MatOfRect()

        val iris = Point()
        clasificator.detectMultiScale(roi, eyes, 1.15, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT or Objdetect.CASCADE_SCALE_IMAGE, Size(
                30.0, 30.0),
                Size())

        val eyesArray = eyes.toArray()

        if (eyesArray.isNotEmpty()) {
            val e = eyesArray[0]

            e.x = area.x + e.x
            e.y = area.y + e.y
            val eyeRectangle = Rect(e.tl().x.toInt(),
                    (e.tl().y + e.height * 0.4).toInt(), e.width,
                    (e.height * 0.6).toInt())
            roi = mGray.submat(eyeRectangle)
            val vyrez: Mat = mRgba.submat(eyeRectangle)

            val mmG = Core.minMaxLoc(roi)

            Imgproc.circle(vyrez, mmG.minLoc, 2, Scalar(255.0, 255.0, 255.0, 255.0), THICKNESS_PUPIL_CIRCLE)

            iris.x = mmG.minLoc.x + eyeRectangle.x
            iris.y = mmG.minLoc.y + eyeRectangle.y

            if (eye.equals("left") && mStopRecording == false) {
                mPointsLeftEye.add(Point(iris.x, iris.y));
            } else {
                if (mStopRecording == false) {
                    mPointsRightEye.add(Point(iris.x, iris.y));
                }
            }

            if (mControl) {
                var averageXLeftEye = 0
                var averageXRightEye = 0
                var averageYLeftEye = 0
                var averageYRightEye = 0

                mStopRecording = true

                if (mPointsLeftEye.size > 5 && mPointsRightEye.size > 5) {
                    for (point in mPointsLeftEye) {
                        averageXLeftEye += point.x.toInt()
                        averageYLeftEye += point.y.toInt()
                    }

                    for (point in mPointsRightEye) {
                        averageXRightEye += point.x.toInt()
                        averageYRightEye += point.y.toInt()
                    }

                    averageXLeftEye /= mPointsLeftEye.size
                    averageXRightEye /= mPointsRightEye.size
                    averageYLeftEye /= mPointsLeftEye.size
                    averageYRightEye /= mPointsRightEye.size

                    mPointsLeftEye.clear()
                    mPointsRightEye.clear()

                    listAverageXEyePosition.add(averageXLeftEye + averageXRightEye)
                }

                mStopRecording = false

                if (listAverageXEyePosition.size >= 2) {
                    var currentAverageX = listAverageXEyePosition.get(listAverageXEyePosition.size -1)
                    var lastAverageX = listAverageXEyePosition.get(listAverageXEyePosition.size -2)

                    // Largeur de l'écran
                    val screenWidth = DisplayMetricsUtils.getWidth(this)
                    // Diamètre du cercle bleu
                    val diameterBlueCircle = DisplayMetricsUtils.dipToPixels(
                            applicationContext, 100f
                    ).toInt()

                    if (lastAverageX > currentAverageX + 10) {
                        mMoveLeft = false
                        mMoveRight = true
                    } else if (lastAverageX + 10 < currentAverageX) {
                        mMoveLeft = true
                        mMoveRight = false
                    }

                    if (mCanStartAnimation && (mMoveRight || mMoveLeft)) {

                        if (mMoveLeft) {
                            runOnUiThread {
                                mAnimator = circle_view
                                        .animate()
                                        .translationX(0.toFloat())
                                        .translationY(0.toFloat())
                                        .setDuration(ANIMATION_CIRCLE_DURATION.toLong())
                                        .withEndAction(Runnable {
                                            mCanStartAnimation = true
                                        })
                            }
                        } else if (mMoveRight) {
                            runOnUiThread {
                                mAnimator = circle_view
                                        .animate()
                                        .translationX((screenWidth - diameterBlueCircle).toFloat())
                                        .translationY(0.toFloat())
                                        .setDuration(ANIMATION_CIRCLE_DURATION.toLong())
                                        .withEndAction(Runnable {
                                            mCanStartAnimation = true
                                        })
                            }
                        }
                    }

                    mCanStartAnimation = false

                }
            }
        }
    }
}