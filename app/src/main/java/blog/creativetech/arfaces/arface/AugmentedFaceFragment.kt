package blog.creativetech.arfaces.arface

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import blog.creativetech.arfaces.R
import blog.creativetech.arfaces.arface.helpers.DisplayRotationHelper
import blog.creativetech.arfaces.arface.helpers.SnackbarHelper
import blog.creativetech.arfaces.arface.helpers.TrackingStateHelper
import blog.creativetech.arfaces.arface.rendering.BackgroundRenderer
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.Session.Feature
import com.google.ar.core.exceptions.*
import java.io.IOException
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


public class AugmentedFaceFragment : Fragment(), GLSurfaceView.Renderer {

    private var session: Session? = null

    private var frameLayout: FrameLayout? = null
    private var surfaceView: GLSurfaceView? = null
    private lateinit var displayRotationHelper: DisplayRotationHelper
    private lateinit var trackingStateHelper: TrackingStateHelper

    var faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()

    private val backgroundRenderer: BackgroundRenderer = BackgroundRenderer()

    private var installRequested = false
    private var canRequestDangerousPermissions = true
    private val messageSnackbarHelper: SnackbarHelper = SnackbarHelper()
    private val RC_PERMISSIONS = 1010

    private var augmentedFaceListener: AugmentedFaceListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayRotationHelper = DisplayRotationHelper(context)
        trackingStateHelper = TrackingStateHelper(requireActivity())
        installRequested = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        frameLayout =
            inflater.inflate(R.layout.fragment_augmented_face, container, false) as FrameLayout
        surfaceView = frameLayout?.findViewById<View>(R.id.surface_view) as GLSurfaceView
        surfaceView?.let {
            it.preserveEGLContextOnPause = true
            it.setEGLContextClientVersion(2)
            it.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.

            it.setRenderer(this)
            it.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            it.setWillNotDraw(false)
        }

        return frameLayout
    }

    override fun onResume() {
        super.onResume()
        if (session == null) {
            var exception: Exception? = null
            var message: String? = null
            try {
                val installStatus = ArCoreApk.getInstance()
                                                .requestInstall(requireActivity(), !installRequested)
                when (installStatus) {
                    InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        return
                    }
                    InstallStatus.INSTALLED -> {
                    }
                    else -> {
                        println("Undefined installed status")
                    }
                }
                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (ContextCompat.checkSelfPermission(requireActivity(), "android.permission.CAMERA")
                    == PackageManager.PERMISSION_GRANTED) {
                    // Configure session to use front facing camera.
                    val featureSet: EnumSet<Feature> =
                        EnumSet.of(Feature.FRONT_CAMERA)
                    // Create the session.
                    session = Session( /* context= */context, featureSet)
                    configureSession()
                } else {
                    requestDangerousPermissions()
                }

            } catch (e: UnavailableArcoreNotInstalledException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableUserDeclinedInstallationException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableApkTooOldException) {
                message = "Please update ARCore"
                exception = e
            } catch (e: UnavailableSdkTooOldException) {
                message = "Please update this app"
                exception = e
            } catch (e: UnavailableDeviceNotCompatibleException) {
                message = "This device does not support AR"
                exception = e
            } catch (e: Exception) {
                message = "Failed to create AR session"
                exception = e
            }
            if (message != null) {
                messageSnackbarHelper.showError(requireActivity(), message)
                println("Exception creating session:$exception")
                return
            }
        }
        // Note that order matters - see the note in onPause(), the reverse applies here.

        try {
            session?.resume()
        } catch (e: CameraNotAvailableException) {
            messageSnackbarHelper.showError(
                requireActivity(),
                "Camera not available. Try restarting the app."
            )
            session = null
            return
        }

        surfaceView?.onResume()
        displayRotationHelper.onResume()
    }

    fun requestDangerousPermissions() {
        if (!canRequestDangerousPermissions) {
            // If this is in progress, don't do it again.
            return
        }
        canRequestDangerousPermissions = false

        val permissions: ArrayList<String> = ArrayList()
        val additionalPermissions: ArrayList<String> = ArrayList()
        val permissionLength = additionalPermissions.size
        for (i in 0 until permissionLength) {
            if (checkSelfPermission(requireActivity(), additionalPermissions[i])
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(additionalPermissions[i])
            }
        }

        // Always check for camera permission
        if (checkSelfPermission(requireActivity(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (!permissions.isEmpty()) {
            // Request the permissions
            requestPermissions(
                permissions.toArray(arrayOfNulls<String>(permissions.size)),
                RC_PERMISSIONS
            )
        }
    }

    public fun setAugmentedFaceListener(listener: AugmentedFaceListener) {
        augmentedFaceListener = listener
    }
    /**
     * If true, [.requestDangerousPermissions] returns without doing anything, if false
     * permissions will be requested
     */
    private fun getCanRequestDangerousPermissions(): Boolean? {
        return canRequestDangerousPermissions
    }

    /**
     * If true, [.requestDangerousPermissions] returns without doing anything, if false
     * permissions will be requested
     */
    private fun setCanRequestDangerousPermissions(canRequestDangerousPermissions: Boolean?) {
        this.canRequestDangerousPermissions = canRequestDangerousPermissions!!
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (checkSelfPermission(
                requireActivity(),
                Manifest.permission.CAMERA
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val builder: AlertDialog.Builder = AlertDialog.Builder(
                                                requireActivity(),
                                                android.R.style.Theme_Material_Dialog_Alert
                                            )
        builder
            .setTitle("Camera permission required")
            .setMessage("Add camera permission via Settings?")
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                // If Ok was hit, bring up the Settings app.
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.fromParts(
                    "package",
                    requireActivity().packageName,
                    null
                )
                requireActivity().startActivity(intent)
                // When the user closes the Settings app, allow the app to resume.
                // Allow the app to ask for permissions again now.
                setCanRequestDangerousPermissions(true)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setOnDismissListener {
                // canRequestDangerousPermissions will be true if "OK" was selected from the dialog,
                // false otherwise.  If "OK" was selected do nothing on dismiss, the app will
                // continue and may ask for permission again if needed.
                // If anything else happened, finish the activity when this dialog is
                // dismissed.
                if (!getCanRequestDangerousPermissions()!!) {
                    requireActivity().finish()
                }
            }
            .show()
    }

    override fun onPause() {
        super.onPause()
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause()
            surfaceView?.onPause()
            session?.pause()
        }
    }

    override fun onDestroy() {
        if (session != null) {
            // Explicitly close ARCore Session to release native resources.
            // Review the API reference for important considerations before calling close() in apps with
            // more complicated lifecycle requirements:
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            session?.close()
            session = null
        }
        super.onDestroy()
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        session?.let {
            // Notify ARCore session that the view size changed so that the perspective matrix and
            // the video background can be properly adjusted.
            // Notify ARCore session that the view size changed so that the perspective matrix and
            // the video background can be properly adjusted.
            displayRotationHelper.updateSessionIfNeeded(it)

            try {
                it.setCameraTextureName(backgroundRenderer.textureId)

                // Obtain the current frame from ARSession. When the configuration is set to
                // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
                // camera framerate.
                val frame: Frame = it.update()
                val camera: Camera = frame.camera

                // Get projection matrix.
                val projectionMatrix = FloatArray(16)
                camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)

                // Get camera matrix and draw.
                val viewMatrix = FloatArray(16)
                camera.getViewMatrix(viewMatrix, 0)

                // Compute lighting from average intensity of the image.
                // The first three components are color scaling factors.
                // The last one is the average pixel intensity in gamma space.
                val colorCorrectionRgba = FloatArray(4)
                frame.lightEstimate.getColorCorrection(colorCorrectionRgba, 0)

                // If frame is ready, render camera preview image to the GL surface.
                backgroundRenderer.draw(frame)

                // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
                trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)
                val faces: Collection<AugmentedFace> =
                    it.getAllTrackables(AugmentedFace::class.java)
                for (face in faces) {
                    if (!faceNodeMap.containsKey(face)) {
                        val faceNode = AugmentedFaceNode(face, requireContext())
                        augmentedFaceListener?.onFaceAdded(faceNode)
                        faceNodeMap[face] = faceNode
                    } else {
                        faceNodeMap[face]?.let{ node ->
                            augmentedFaceListener?.onFaceUpdate(node)
                        }
                    }

                    val iter = faceNodeMap.entries.iterator()
                    while (iter.hasNext()) {
                        val entry = iter.next()
                        val faceNode = entry.key
                        if (faceNode.trackingState == TrackingState.STOPPED) {
                            iter.remove()
                        }
                    }
                    if (face.trackingState !== TrackingState.TRACKING) {
                        break
                    }
                    // Face objects use transparency so they must be rendered back to front without depth write.
                    GLES20.glDepthMask(false)


                    // Each face's region poses, mesh vertices, and mesh normals are updated every frame.

                    // 1. Render the face mesh first, behind any 3D objects attached to the face regions.
                    faceNodeMap[face]?.onDraw(projectionMatrix, viewMatrix, colorCorrectionRgba)
                }
            } catch (t: Throwable) {
                // Avoid crashing the application due to unhandled exceptions.
                println("Exception on the OpenGL thread $t")
            } finally {
                GLES20.glDepthMask(true)
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(context)
        } catch (e: IOException) {
            println("Failed to read an asset file $e")
        }
    }

    private fun configureSession() {
        val config = Config(session)
        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
        session?.configure(config)
    }
}