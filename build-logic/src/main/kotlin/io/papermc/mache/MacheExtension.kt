package io.papermc.mache

import io.papermc.mache.constants.DefaultRepos
import io.papermc.mache.util.MacheRepo
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property

open class MacheExtension(objects: ObjectFactory) {
    /**
     * The version of Minecraft which will serve as the base.
     */
    val minecraftVersion: Property<String> = objects.property()

    /**
     * Base arguments passed to the decompiler.
     */
    val decompilerArgs: ListProperty<String> = objects.listProperty()

    /**
     * Maven repositories needed to resolve the configurations necessary to run mache. The configurations are
     * `codebook`, `paramMappings`, `constants`, `remapper`, and `decompiler`.
     *
     * These are defined in this way because we need this information for the metadata file we generate. Repositories
     * defined in the normal Gradle style will not be reported in the metadata file.
     */
    val repositories: NamedDomainObjectContainer<MacheRepo> = objects.domainObjectContainer(MacheRepo::class)

    init {
        decompilerArgs.convention(
            listOf(
                // Synthetic Not Set: Treat some known structures as synthetic even when not explicitly set
                "-nns=true",
                // Ternary Constant Simplification:
                // Fold branches of ternary expressions that have boolean true and false constants
                "-tcs=true",
                // Override Annotation: Display override annotations for methods known to the decompiler
                "-ovr=false",
                // [Experimental] Verify Variable Merges:
                // Double checks to make sure the validity of variable merges
                "-vvm=true",
                // Include Entire Classpath: Give the decompiler information about every jar on the classpath
                "-iec=true",
                // Include Java Runtime: Give the decompiler information about the Java runtime
                "-jrt=current",
                // Indent String
                "-ind=    ",
                // JAD-Style Variable Naming: Use JAD-style variable naming for local variables
                "-jvn=true",
                // Decompile complex constant-dynamic expressions:
                // Some constant-dynamic expressions can't be converted to a single Java expression with
                // identical run-time behaviour. This decompiles them to a similar non-lazy expression,
                // marked with a comment
                "-dcc=true",
                // Skip Extra Files: Skip copying non-class files from the input folder or file to the output
                "-sef=true",
                // New Line Seperator: Character that seperates lines in the decompiled output.
                "-nls=1",
            ),
        )

        for (repo in DefaultRepos.DEFAULTS) {
            repositories.register(repo.name) {
                url.set(repo.url)
                includeGroups.addAll(repo.includeGroups)
            }
        }
    }
}
