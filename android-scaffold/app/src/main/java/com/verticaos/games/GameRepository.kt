package com.verticaos.games

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Environment
import android.util.Log
import java.io.File

data class GameEntry(
    val packageName: String,
    val title: String,
    val icon: Drawable?,
    val snapFile: File?,
    var creditDelaySeconds: Int = 10,
    var pulsesPerCredit: Int = 1,
    val manufacturer: String = "",
    val year: String = ""
)

data class MameGameInfo(
    val packageName: String,
    val description: String,
    val manufacturer: String,
    val year: String
)

/**
 * Arcade ROM-list repository for "VERTICAOS CROSS COLLECTION".
 * Manages all installed apps for Settings and parses MAME-style syntax from Downloads/list/jogos.txt for main carousel.
 */
class GameRepository(private val pm: PackageManager, private val config: ConfigManager) {

    private var _list: List<GameEntry> = emptyList()
    private var _allAvailable: List<GameEntry> = emptyList()
    
    val list: List<GameEntry> get() = _list
    val allAvailable: List<GameEntry> get() = _allAvailable
    val size: Int get() = _list.size

    init {
        setupStorageAndTemplate()
    }

    /**
     * 1. FOLDER CREATION & AUTO-TEMPLATE (On Boot)
     */
    private fun setupStorageAndTemplate() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val listDir = File(downloadsDir, "list")
            val snapsDir = File(downloadsDir, "snaps")
            val snapDir = File(downloadsDir, "snap")

            if (!listDir.exists()) listDir.mkdirs()
            if (!snapsDir.exists()) snapsDir.mkdirs()
            if (!snapDir.exists()) snapDir.mkdirs()

            val jogosFile1 = File(listDir, "jogos.txt")
            if (!jogosFile1.exists()) {
                val sampleLine = "# Verticaos Games - Lista de Jogos MAME\n# Exemplo:\ngame ( name \"com.example.game\" description \"Exemplo Jogo Arcade\" manufacturer \"Verticaos\" year 2026 )\n"
                jogosFile1.writeText(sampleLine)
            }

            val jogosFile2 = File(downloadsDir, "jogos.txt")
            if (!jogosFile2.exists()) {
                val sampleLine = "# Verticaos Games - Lista de Jogos MAME\n# Exemplo:\ngame ( name \"com.example.game\" description \"Exemplo Jogo Arcade\" manufacturer \"Verticaos\" year 2026 )\n"
                jogosFile2.writeText(sampleLine)
            }
        } catch (e: Exception) {
            Log.e("GameRepository", "Error setting up storage and template: ${e.message}", e)
        }
    }

    private fun findJogosFile(): File? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val externalStorage = Environment.getExternalStorageDirectory()

        val candidates = listOf(
            File(downloadsDir, "list/jogos.txt"),
            File(downloadsDir, "jogos.txt"),
            File(externalStorage, "Download/list/jogos.txt"),
            File(externalStorage, "Download/jogos.txt"),
            File(externalStorage, "list/jogos.txt"),
            File(externalStorage, "jogos.txt")
        )
        return candidates.firstOrNull { it.exists() && it.isFile }
    }

    /**
     * 2. PARSING THE MAME-STYLE LIST FILE
     */
    private fun parseMameList(): Map<String, MameGameInfo> {
        val map = mutableMapOf<String, MameGameInfo>()
        try {
            val jogosFile = findJogosFile() ?: return map
            Log.d("GameRepository", "Parsing jogos.txt from: ${jogosFile.absolutePath}")

            val lines = jogosFile.readLines()
            val pattern = Regex("""(\w+)\s+(?:"([^"]*)"|([^\s()]+))""")

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("#") || trimmed.isEmpty() || !trimmed.startsWith("game")) continue

                var pkgName = ""
                var description = ""
                var manufacturer = ""
                var year = ""

                val matches = pattern.findAll(trimmed)
                for (match in matches) {
                    val key = match.groupValues[1]
                    val quoted = match.groupValues[2]
                    val unquoted = match.groupValues[3]
                    val value = if (quoted.isNotEmpty()) quoted else unquoted

                    when (key.lowercase()) {
                        "name" -> pkgName = value
                        "description" -> description = value
                        "manufacturer" -> manufacturer = value
                        "year" -> year = value
                    }
                }

                if (pkgName.isNotEmpty()) {
                    if (description.isEmpty()) description = pkgName
                    val info = MameGameInfo(pkgName, description, manufacturer, year)
                    
                    val pkgLower = pkgName.lowercase()
                    map[pkgLower] = info
                    
                    val shortName = pkgName.substringAfterLast('.').lowercase()
                    if (shortName.isNotEmpty() && !map.containsKey(shortName)) {
                        map[shortName] = info
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GameRepository", "Error parsing jogos.txt: ${e.message}", e)
        }
        return map
    }

    private fun getSnapDirectories(): List<File> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val externalStorage = Environment.getExternalStorageDirectory()

        val candidates = listOf(
            File(downloadsDir, "snaps"),
            File(downloadsDir, "snap"),
            File(downloadsDir, "list/snaps"),
            File(downloadsDir, "list/snap"),
            File(externalStorage, "Download/snaps"),
            File(externalStorage, "Download/snap"),
            File(externalStorage, "snaps"),
            File(externalStorage, "snap")
        )
        return candidates.filter { it.exists() && it.isDirectory }
    }

    /**
     * 3. CUSTOM LOCAL IMAGE LOADING (Snaps)
     */
    private fun findSnapFile(packageName: String, title: String = "", mameDescription: String = ""): File? {
        try {
            val dirs = getSnapDirectories()
            if (dirs.isEmpty()) return null

            val searchTerms = mutableSetOf<String>()
            val pkgLower = packageName.lowercase().trim()
            searchTerms.add(pkgLower)
            searchTerms.add(pkgLower.replace('.', '_'))
            
            val lastSegment = pkgLower.substringAfterLast('.')
            if (lastSegment.isNotEmpty()) searchTerms.add(lastSegment)

            if (title.isNotEmpty()) {
                val titleLower = title.lowercase().trim()
                searchTerms.add(titleLower)
                searchTerms.add(titleLower.replace(' ', '_'))
                searchTerms.add(titleLower.replace(" ", ""))
            }

            if (mameDescription.isNotEmpty()) {
                val mameLower = mameDescription.lowercase().trim()
                searchTerms.add(mameLower)
                searchTerms.add(mameLower.replace(' ', '_'))
                searchTerms.add(mameLower.replace(" ", ""))
            }

            val validExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp")

            for (dir in dirs) {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (!file.isFile) continue
                    val ext = file.extension.lowercase()
                    if (!validExtensions.contains(ext)) continue

                    val nameWithoutExt = file.nameWithoutExtension.lowercase().trim()
                    if (searchTerms.contains(nameWithoutExt)) {
                        return file
                    }

                    val nameNormalized = nameWithoutExt.replace('_', ' ').replace('-', ' ').replace(" ", "")
                    for (term in searchTerms) {
                        val termNormalized = term.replace('_', ' ').replace('-', ' ').replace(" ", "")
                        if (nameNormalized == termNormalized) {
                            return file
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GameRepository", "Error finding snap file for $packageName: ${e.message}", e)
        }
        return null
    }

    /**
     * 2. NEW FEATURE: "GERAR LISTA" (Generate List)
     * Takes ONLY currently enabled apps and overwrites Downloads/list/jogos.txt in MAME format.
     */
    fun generateJogosTxt() {
        try {
            setupStorageAndTemplate()
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val listDir = File(downloadsDir, "list")
            if (!listDir.exists()) listDir.mkdirs()

            val jogosFile1 = File(listDir, "jogos.txt")
            val jogosFile2 = File(downloadsDir, "jogos.txt")

            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = pm.queryIntentActivities(intent, 0)
            val launcherPkg = "com.verticaos.games"
            val enabledPkgs = config.getEnabledPackages()

            val sb = StringBuilder()
            sb.append("# Verticaos Cross Collection - Generated Arcade ROM List\n")
            sb.append("# Edit descriptions below to customize game titles in the arcade frontend.\n\n")

            for (ri in resolved) {
                val app = ri.activityInfo.applicationInfo
                if (app.packageName == launcherPkg) continue
                if (!enabledPkgs.contains(app.packageName)) continue

                val defaultTitle = app.loadLabel(pm).toString().replace("\"", "\\\"")
                sb.append("game ( name \"${app.packageName}\" description \"$defaultTitle\" manufacturer \"Android\" year 2026 )\n")
            }

            val textContent = sb.toString()
            jogosFile1.writeText(textContent)
            try { jogosFile2.writeText(textContent) } catch (e: Exception) {}

            Log.d("GameRepository", "Generated jogos.txt successfully with ${enabledPkgs.size} games.")
        } catch (e: Exception) {
            Log.e("GameRepository", "Error generating jogos.txt: ${e.message}", e)
        }
    }

    fun refresh() {
        setupStorageAndTemplate()

        val mameMap = parseMameList()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)

        val launcherPkg = "com.verticaos.games"

        // 1. Build _allAvailable (ALL launchable apps for Settings Menu checkboxes)
        _allAvailable = resolved.mapNotNull { ri ->
            val app = ri.activityInfo.applicationInfo
            if (app.packageName == launcherPkg) return@mapNotNull null

            val defaultTitle = app.loadLabel(pm).toString()
            val pkgLower = app.packageName.lowercase()
            val shortPkg = pkgLower.substringAfterLast('.')

            val mameInfo = mameMap[pkgLower] ?: mameMap[shortPkg]

            val displayTitle = mameInfo?.description ?: defaultTitle
            val manufacturer = mameInfo?.manufacturer?.ifEmpty { "Android" } ?: "Android"
            val year = mameInfo?.year?.ifEmpty { "2026" } ?: "2026"

            val snap = findSnapFile(app.packageName, defaultTitle, displayTitle)
            val delay = config.getGameDelay(app.packageName)
            val pulses = config.getPulsesPerCredit(app.packageName)

            GameEntry(
                packageName = app.packageName,
                title = displayTitle,
                icon = app.loadIcon(pm),
                snapFile = snap,
                creditDelaySeconds = delay,
                pulsesPerCredit = pulses,
                manufacturer = manufacturer,
                year = year
            )
        }.sortedBy { it.title.lowercase() }

        // 2. Build _list (Main Carousel: Enabled apps enhanced with jogos.txt metadata)
        val enabledPkgs = config.getEnabledPackages()
        _list = _allAvailable.filter { enabledPkgs.contains(it.packageName) }.map { entry ->
            val pkgLower = entry.packageName.lowercase()
            val shortPkg = pkgLower.substringAfterLast('.')
            val mameInfo = mameMap[pkgLower] ?: mameMap[shortPkg]

            val finalSnap = if (entry.snapFile != null && entry.snapFile.exists()) {
                entry.snapFile
            } else {
                findSnapFile(entry.packageName, entry.title, mameInfo?.description ?: "")
            }

            if (mameInfo != null) {
                entry.copy(
                    title = mameInfo.description,
                    snapFile = finalSnap,
                    manufacturer = if (mameInfo.manufacturer.isNotEmpty()) mameInfo.manufacturer else entry.manufacturer,
                    year = if (mameInfo.year.isNotEmpty()) mameInfo.year else entry.year
                )
            } else {
                entry.copy(snapFile = finalSnap)
            }
        }.sortedBy { it.title.lowercase() }
    }
}
