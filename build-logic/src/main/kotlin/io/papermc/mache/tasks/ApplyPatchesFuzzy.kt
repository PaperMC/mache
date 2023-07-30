package io.papermc.mache.tasks

import io.papermc.mache.util.patches.JavaPatcherFuzzy
import io.papermc.mache.util.patches.NativePatcherFuzzy
import io.papermc.mache.util.patches.Patcher
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option

@UntrackedTask(because = "Always apply patches")
abstract class ApplyPatchesFuzzy : ApplyPatches() {

    @get:Input
    @get:Option(
        option = "max-fuzz",
        description = "Max fuzz. Cannot be set higher than context (3). Setting this " +
            "value higher increases the chances of a faulty patch.",
    )
    abstract val maxFuzz: Property<String>

    override fun createPatcher(): Patcher {
        return if (useNativeDiff.get()) {
            NativePatcherFuzzy(exec, patchExecutable.get(), maxFuzz.get().toInt())
        } else {
            JavaPatcherFuzzy(maxFuzz.get().toInt())
        }
    }
}
