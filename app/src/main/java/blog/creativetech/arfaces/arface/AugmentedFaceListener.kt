package blog.creativetech.arfaces.arface

public interface AugmentedFaceListener {
    fun onFaceAdded(face: AugmentedFaceNode)
    fun onFaceUpdate(face: AugmentedFaceNode)
}