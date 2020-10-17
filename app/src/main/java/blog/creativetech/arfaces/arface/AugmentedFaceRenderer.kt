package blog.creativetech.arfaces.arface

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import blog.creativetech.arfaces.arface.rendering.ShaderUtil.loadGLShader
import com.google.ar.core.AugmentedFace
import java.io.IOException
import java.nio.FloatBuffer
import java.nio.ShortBuffer

public class AugmentedFaceRenderer {
    private val TAG = AugmentedFaceRenderer::class.java.simpleName

    private var modelViewUniform = 0
    private var modelViewProjectionUniform = 0

    private var textureUniform = 0

    private var lightingParametersUniform = 0

    private var materialParametersUniform = 0

    private var colorCorrectionParameterUniform = 0

    private var tintColorUniform = 0

    private var attriVertices = 0
    private var attriUvs = 0
    private var attriNormals = 0

    // Set some default material properties to use for lighting.
    private var ambient = 0.3f
    private var diffuse = 1.0f
    private var specular = 1.0f
    private var specularPower = 6.0f

    private val textureId = IntArray(1)

    private val lightDirection = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f)
    private var program = 0
    private val modelViewProjectionMat = FloatArray(16)
    private val modelViewMat = FloatArray(16)
    private val viewLightDirection = FloatArray(4)

    fun AugmentedFaceRenderer() {}

    @Throws(IOException::class)
    fun createOnGlThread(
        context: Context,
        diffuseTextureAssetName: String
    ) {
        val vertexShader: Int =
            loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, Companion.VERTEX_SHADER_NAME)
        val fragmentShader: Int =
            loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, Companion.FRAGMENT_SHADER_NAME)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        modelViewProjectionUniform = GLES20.glGetUniformLocation(program, "u_ModelViewProjection")
        modelViewUniform = GLES20.glGetUniformLocation(program, "u_ModelView")
        textureUniform = GLES20.glGetUniformLocation(program, "u_Texture")
        lightingParametersUniform = GLES20.glGetUniformLocation(program, "u_LightningParameters")
        materialParametersUniform = GLES20.glGetUniformLocation(program, "u_MaterialParameters")
        colorCorrectionParameterUniform =
            GLES20.glGetUniformLocation(program, "u_ColorCorrectionParameters")
        tintColorUniform = GLES20.glGetUniformLocation(program, "u_TintColor")
        attriVertices = GLES20.glGetAttribLocation(program, "a_Position")
        attriUvs = GLES20.glGetAttribLocation(program, "a_TexCoord")
        attriNormals = GLES20.glGetAttribLocation(program, "a_Normal")
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glGenTextures(1, textureId, 0)
        loadTexture(context, textureId, diffuseTextureAssetName)
    }

    @Throws(IOException::class)
    private fun loadTexture(
        context: Context,
        textureId: IntArray,
        filename: String
    ) {
        val textureBitmap = BitmapFactory.decodeStream(context.getAssets().open(filename))
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        textureBitmap.recycle()
    }

    fun draw(
        projmtx: FloatArray?,
        viewmtx: FloatArray?,
        modelmtx: FloatArray?,
        colorCorrectionRgba: FloatArray?,
        face: AugmentedFace
    ) {
        val vertices: FloatBuffer = face.meshVertices
        val normals: FloatBuffer = face.meshNormals
        val textureCoords: FloatBuffer = face.meshTextureCoordinates
        val triangleIndices: ShortBuffer = face.meshTriangleIndices
        GLES20.glUseProgram(program)
        GLES20.glDepthMask(false)
        val modelViewProjectionMatTemp = FloatArray(16)
        Matrix.multiplyMM(modelViewProjectionMatTemp, 0, projmtx, 0, viewmtx, 0)
        Matrix.multiplyMM(modelViewProjectionMat, 0, modelViewProjectionMatTemp, 0, modelmtx, 0)
        Matrix.multiplyMM(modelViewMat, 0, viewmtx, 0, modelmtx, 0)

        // Set the lighting environment properties.
        Matrix.multiplyMV(viewLightDirection, 0, modelViewMat, 0, lightDirection, 0)
        normalizeVec3(viewLightDirection)
        GLES20.glUniform4f(
            lightingParametersUniform,
            viewLightDirection[0],
            viewLightDirection[1],
            viewLightDirection[2],
            1f
        )
        GLES20.glUniform4fv(colorCorrectionParameterUniform, 1, colorCorrectionRgba, 0)

        // Set the object material properties.
        GLES20.glUniform4f(materialParametersUniform, ambient, diffuse, specular, specularPower)

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(modelViewUniform, 1, false, modelViewMat, 0)
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMat, 0)
        GLES20.glEnableVertexAttribArray(attriVertices)
        GLES20.glVertexAttribPointer(attriVertices, 3, GLES20.GL_FLOAT, false, 0, vertices)
        GLES20.glEnableVertexAttribArray(attriNormals)
        GLES20.glVertexAttribPointer(attriNormals, 3, GLES20.GL_FLOAT, false, 0, normals)
        GLES20.glEnableVertexAttribArray(attriUvs)
        GLES20.glVertexAttribPointer(attriUvs, 2, GLES20.GL_FLOAT, false, 0, textureCoords)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(textureUniform, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        GLES20.glUniform4f(tintColorUniform, 0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_BLEND)

        // Textures are loaded with premultiplied alpha
        // (https://developer.android.com/reference/android/graphics/BitmapFactory.Options#inPremultiplied),
        // so we use the premultiplied alpha blend factors.
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, triangleIndices.limit(), GLES20.GL_UNSIGNED_SHORT, triangleIndices
        )
        GLES20.glUseProgram(0)
        GLES20.glDepthMask(true)
    }

    fun setMaterialProperties(
        ambient: Float,
        diffuse: Float,
        specular: Float,
        specularPower: Float
    ) {
        this.ambient = ambient
        this.diffuse = diffuse
        this.specular = specular
        this.specularPower = specularPower
    }

    private fun normalizeVec3(v: FloatArray) {
        val reciprocalLength = 1.0f / Math.sqrt(
            v[0] * v[0] + v[1] * v[1] + (v[2] * v[2]).toDouble()
        ).toFloat()
        v[0] *= reciprocalLength
        v[1] *= reciprocalLength
        v[2] *= reciprocalLength
    }

    companion object {
        private const val VERTEX_SHADER_NAME = "shaders/object.vert"
        private const val FRAGMENT_SHADER_NAME = "shaders/object.frag"
    }
}