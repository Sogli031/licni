package com.licnipodaci.data.backup

import java.security.MessageDigest

internal object BackupPayloadPolicy {
    /** Uvek se čuva ovoliko najnovijih prethodnih kopija... */
    const val MAX_PREV_COPIES = 5

    /** ...a od starijih po jedna (najnovija) za svaki dan u poslednjih toliko dana. */
    const val DAILY_KEEP_DAYS = 30

    private const val DAY_MILLIS = 86_400_000L

    fun shouldSkipBackup(jsonData: String): Boolean =
        jsonData.length < 5 || jsonData.trim() == "[]"

    /** MD5 u heksadecimalnom obliku (isti zapis kao `md5Checksum` u Drive-u). */
    fun md5Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("MD5").digest(bytes).joinToString("") { "%02x".format(it) }

    /** "ime.json" -> "ime_prev_<epochMillis>.json" */
    fun prevCopyName(mainFileName: String, nowMillis: Long): String =
        "${prevCopyPrefix(mainFileName)}$nowMillis$PREV_SUFFIX"

    /** Vreme iz imena prethodne kopije, ili null ako ime nije prethodna kopija ovog fajla. */
    fun prevCopyTimestamp(mainFileName: String, fileName: String): Long? {
        val prefix = prevCopyPrefix(mainFileName)
        if (!fileName.startsWith(prefix) || !fileName.endsWith(PREV_SUFFIX)) return null
        return fileName.substring(prefix.length, fileName.length - PREV_SUFFIX.length).toLongOrNull()
    }

    /**
     * Od svih fajlova (id, ime) vraća ID-jeve prethodnih kopija koje treba obrisati.
     * Čuva se [keepRecent] najnovijih, plus najnovija kopija za svaki dan u poslednjih
     * [keepDays] dana. Kopije sa istim vremenom su duplikati (ista verzija podataka) - ostaje
     * jedna. Fajlovi koji nisu kopije se ne diraju.
     */
    fun prevCopiesToDelete(
        mainFileName: String,
        files: List<Pair<String, String>>,
        nowMillis: Long,
        keepRecent: Int = MAX_PREV_COPIES,
        keepDays: Int = DAILY_KEEP_DAYS
    ): List<String> {
        val copies = files
            .mapNotNull { (id, name) -> prevCopyTimestamp(mainFileName, name)?.let { id to it } }
            .sortedByDescending { it.second }

        val seenTimes = HashSet<Long>()
        val unique = copies.filter { seenTimes.add(it.second) }

        val keep = HashSet<String>()
        unique.take(keepRecent).forEach { keep.add(it.first) }

        val seenDays = HashSet<Long>()
        unique.forEach { (id, time) ->
            if (nowMillis - time <= keepDays * DAY_MILLIS && seenDays.add(time / DAY_MILLIS)) {
                keep.add(id)
            }
        }

        return copies.filter { it.first !in keep }.map { it.first }
    }

    private const val PREV_SUFFIX = ".json"

    private fun prevCopyPrefix(mainFileName: String): String =
        mainFileName.removeSuffix(PREV_SUFFIX) + "_prev_"
}
