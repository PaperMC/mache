package io.papermc.mache.tasks

import com.github.difflib.patch.Patch
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option

@UntrackedTask(because = "Always apply patches")
abstract class ApplyPatchesFuzzy : ApplyPatches() {

    @get:Input
    @get:Option(option = "max-fuzz", description = "Maximum")
    abstract val maxFuzz: Property<String>

    override fun applyPatch(patch: Patch<String>, lines: List<String>): List<String> {
        val fuzz = maxFuzz.get().toIntOrNull() ?: throw IllegalArgumentException("max-fuzz argument must be a non-negative integer")
        if (fuzz < 0) {
            throw IllegalArgumentException("max-fuzz argument must be a non-negative integer")
        }
        return patch.applyFuzzy(lines, fuzz)
    }
}
