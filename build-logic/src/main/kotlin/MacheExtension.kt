import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property

open class MacheExtension(objects: ObjectFactory) {
    val minecraftVersion: Property<String> = objects.property()

    val decompilerArgs: ListProperty<String> = objects.listProperty()

    init {
        decompilerArgs.convention(
            listOf(
                "-nns=true",
                "-tcs=true",
                "-ovr=false",
                "-vvm=true",
                "-iec=true",
                "-jrt=current",
                "-ind=    ",
                "-jvn=true",
                "-dcc=true",
            ),
        )
    }
}
