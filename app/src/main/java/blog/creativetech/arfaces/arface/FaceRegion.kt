package blog.creativetech.arfaces.arface

import android.content.Context
import blog.creativetech.arfaces.arface.rendering.ObjectRenderer

class FaceRegion(val faceLandmark: AugmentedFaceNode.Companion.FaceLandmark) {
    private val objectRenderer: ObjectRenderer = ObjectRenderer()
    private val scaleFactor = 1.0f

    fun setRenderable (context: Context, modelName: String, modelTexture: String) {
        objectRenderer.createOnGlThread( /*context=*/context,
            modelName,
            modelTexture
        )
        objectRenderer.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f)
        objectRenderer.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending)
    }

    fun draw(objectMatrix: FloatArray, viewMatrix: FloatArray, projectionMatrix: FloatArray, colorCorrectionRgba: FloatArray) {
       objectRenderer.updateModelMatrix(objectMatrix, scaleFactor)
       objectRenderer.draw(
           viewMatrix,
           projectionMatrix,
           colorCorrectionRgba,
           DEFAULT_COLOR
       )
    }

    companion object {
        private val DEFAULT_COLOR = floatArrayOf(0f, 0f, 0f, 0f)
    }
}