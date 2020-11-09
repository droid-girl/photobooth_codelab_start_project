# ARCore Augmented Faces wrapper without Sceneform



## Simple usage:

```
<fragment android:name="blog.creativetech.arfaces.arface.AugmentedFaceFragment"
       android:id="@+id/face_view"
       android:layout_width="match_parent"
       android:layout_height="match_parent"
       android:layout_gravity="top" />
```
Use `AugmentedFaceFragment` in your `main_activity` layout

Implement interface in MainActivity

```
class MainActivity : AppCompatActivity(), AugmentedFaceListener {
   override fun onFaceAdded(face: AugmentedFaceNode) {}

   override fun onFaceUpdate(face: AugmentedFaceNode) {}
}
```
Check out the codelab for more information and learn how to use the wrapper -> https://arcore.how/posts/ar_faces_intro/lab/#0 
