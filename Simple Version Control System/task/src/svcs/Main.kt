package svcs

import java.io.File
import java.util.UUID

private const val VCS_PATH = "vcs"

fun main(args: Array<String>) {

    val configFile = File("$VCS_PATH/config.txt")
    val indexFile = File("$VCS_PATH/index.txt")
    val logFile = File("$VCS_PATH/log.txt")

    VCS_PATH.createVCS(configFile, indexFile, logFile)

    when (args.size) {
        0 -> printHelp()
        1 -> when (args[0]) {
            "--help" -> printHelp()
            "config" -> null.config(configFile)
            "add" -> null.add(indexFile)
            "log" -> checkLog(logFile)
            "commit" -> println("Message was not passed.")
            "checkout" -> println("Commit id was not passed.")
            else -> println("\'${args[0]}\' is not a SVCS command.")
        }
        2 -> when (args[0]) {
            "config" -> args[1].config(configFile)
            "add" -> args[1].add(indexFile)
            "commit" -> args[1].commit(VCS_PATH, configFile, indexFile, logFile)
            "checkout" -> args[1].checkout(VCS_PATH, indexFile)
        }
    }
}

private fun String.createVCS(configFile: File, indexFile: File, logFile: File) {
    if (!File(this).exists()) {
        File(this).mkdir() // create vcs directory
        File("$this/commits").mkdir() // create commits directory
        configFile.createNewFile() // create empty config.txt file
        indexFile.createNewFile() // create empty index.txt file
        logFile.createNewFile() // create empty log.txt file
    }
}

private fun printHelp() = println(
    """These are SVCS commands:
config     Get and set a username.
add        Add a file to the index.
log        Show commit logs.
commit     Save changes.
checkout   Restore a file."""
)

private fun String?.config(configFile: File) = when (this) {
    null -> println(
        if (configFile.length() > 0) "The username is ${configFile.readText()}."
        else "Please, tell me who you are."
    )
    else -> {
        configFile.writeText(this)
        println("The username is $this.")
    }
}

private fun String?.add(indexFile: File) = when (this) {
    null -> if (indexFile.length() > 0) {
        println("Tracked files:")
        indexFile.forEachLine { println(it) }
    } else println("Add a file to the index.")

    else -> if (File(this).exists()) {
        indexFile.appendText("$this\n")
        println("The File \'$this\' is tracked.")
    } else println("Can't found \'$this\'.")
}

private fun checkLog(logFile: File) = println(
    if (logFile.length() > 0) "${logFile.readText()}."
    else "No commits yet."
)

private fun String.commit(vcsPath: String, configFile: File, indexFile: File, logFile: File) {
    val lastCommit = if (logFile.length() > 0) logFile.readLines().first().split(" ")[1] else ""

    val changeList = mutableListOf<String>()
    indexFile.forEachLine {
        var oldContent = ""
        val newContent = File(it).readText()
        val path = "$vcsPath/commits/$lastCommit/$it"
        if (File(path).exists()) oldContent = File(path).readText()
        if (oldContent != newContent) changeList.add(it)
    }

    if (changeList.isEmpty()) println("Nothing to commit.")
    else {
        val commitId = UUID.randomUUID().toString()
        val commitPath = "$vcsPath/commits/$commitId"
        changeList.forEach { File(it).copyTo(File("$commitPath/$it")) }
        val commitEntry = "commit $commitId\n" +
                "Author: ${configFile.readText()}\n" +
                this
        logFile.writeOnTop(commitEntry)
        println("Changes are committed.")
    }
}

private fun String.checkout(vcsPath: String, indexFile: File) {
    val commitPath = "$vcsPath/commits/$this"
    if (File(commitPath).exists()) {
        indexFile.forEachLine {
            File(it).deleteRecursively()
            File("$commitPath/$it").copyTo(File(it))
        }
        println("Switched to commit $this.")
    } else println("Commit does not exist.")
}

private fun File.writeOnTop(text: String) {
    val tempString = this.readText()
    return this.writeText("$text\n$tempString")
}
