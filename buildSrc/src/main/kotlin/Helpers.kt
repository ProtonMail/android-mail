import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

// It's very easy to forget something when adding Hilt and finding the cause can be
// dreadful. Therefore, this little helper module ensure everything is setup correctly
fun Project.setAsHiltModule() {
    apply(plugin = "dagger.hilt.android.plugin")

    dependencies {
        implementation(listOf(Dagger.hiltAndroid))
        kapt(listOf(Dagger.hiltDaggerCompiler, AndroidX.Hilt.compiler))
    }
}
