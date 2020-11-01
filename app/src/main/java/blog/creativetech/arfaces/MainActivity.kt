package blog.creativetech.arfaces

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import blog.creativetech.arfaces.arface.AugmentedFaceFragment
import blog.creativetech.arfaces.arface.AugmentedFaceListener
import blog.creativetech.arfaces.arface.AugmentedFaceNode
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), AugmentedFaceListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (face_view as AugmentedFaceFragment).setAugmentedFaceListener(this)
    }
  
    override fun onFaceAdded(face: AugmentedFaceNode) {
        face.setFaceMeshTexture("models/freckles.png")
        face.setRegionModel(
                AugmentedFaceNode.Companion.FaceLandmark.NOSE_TIP,
                "models/NOSE.obj",
                "models/nose_fur.png")
        face.setRegionModel(
                AugmentedFaceNode.Companion.FaceLandmark.FOREHEAD_LEFT,
                "models/FOREHEAD_LEFT.obj",
                "models/ear_fur.png")
        face.setRegionModel(
                AugmentedFaceNode.Companion.FaceLandmark.FOREHEAD_RIGHT,
                "models/FOREHEAD_RIGHT.obj",
                "models/ear_fur.png")
    }

    override fun onFaceUpdate(face: AugmentedFaceNode) {}
}