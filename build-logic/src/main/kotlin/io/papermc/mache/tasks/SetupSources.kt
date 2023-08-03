package io.papermc.mache.tasks

import io.papermc.mache.util.convertToPath
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.PersonIdent
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Always setup sources")
abstract class SetupSources : DefaultTask() {

    @get:InputFile
    abstract val decompJar: RegularFileProperty

    @get:InputFile
    abstract val patchedJar: RegularFileProperty

    @get:InputFile
    abstract val failedPatchJar: RegularFileProperty

    @get:OutputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val files: FileOperations

    @TaskAction
    fun run() {
        val sources = sourceDir.convertToPath()

        val gitDir = sources.resolve(".git")
        val git = if (gitDir.notExists()) {
            setupInitialRepo(sources)
        } else {
            Git.open(sources.toFile()).let { git ->
                val tags = git.tagList().call()
                val initialTag = tags.firstOrNull { it.name == "refs/tags/$INITIAL_TAG" }
                if (initialTag != null) {
                    git.tagDelete()
                        .setTags(*tags.filter { it !== initialTag }.map { it.name }.toTypedArray())
                        .call()

                    clearDirectory(sources)
                    git.reset()
                        .setMode(ResetCommand.ResetType.HARD)
                        .setRef(initialTag.objectId.name)
                        .call()

                    return@let git
                } else {
                    // the repo isn't setup how we expect, so just start from scratch
                    return@let setupInitialRepo(sources)
                }
            }
        }

        files.sync {
            from(files.zipTree(decompJar))
            into(sourceDir)
            includeEmptyDirs = false
        }

        git.add().addFilepattern(".").call()
        git.commit()
            .setMessage("Decompiled")
            .setAuthor(macheIdent)
            .setSign(false)
            .call()
        git.tag().setName("decompiled").setTagger(macheIdent).setSigned(false).call()

        files.sync {
            from(files.zipTree(patchedJar))
            into(sourceDir)
            includeEmptyDirs = false
        }

        git.add().addFilepattern(".").call()
        git.commit()
            .setMessage("Patched")
            .setAuthor(macheIdent)
            .setSign(false)
            .call()
        git.tag().setName("patched").setTagger(macheIdent).setSigned(false).call()

        files.copy {
            from(files.zipTree(failedPatchJar))
            into(sourceDir)
            includeEmptyDirs = false
        }
    }

    private fun setupInitialRepo(path: Path): Git {
        path.deleteRecursively()
        path.createDirectories()
        val git = Git.init()
            .setDirectory(path.toFile())
            .setInitialBranch("mache")
            .call()

        // initial commit is an empty commit
        // this lets us reset to it without deleting the repo
        git.commit()
            .setAllowEmpty(true)
            .setMessage("Initial")
            .setAuthor(macheIdent)
            .setSign(false)
            .call()
        git.tag().setName(INITIAL_TAG).setTagger(macheIdent).setSigned(false).call()

        return git
    }

    private fun clearDirectory(path: Path) {
        for (entry in path.listDirectoryEntries()) {
            if (entry.name == ".git") {
                continue
            }
            entry.deleteRecursively()
        }
    }

    companion object {
        private const val INITIAL_TAG = "initial"

        private val macheIdent = PersonIdent("Papier-mâché", "paper@mache.gradle")
    }
}
