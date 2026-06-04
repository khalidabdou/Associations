package org.associations.project.utils

object ArabicShaper {
    private val FORMS: Map<Char, IntArray> = mapOf(
        '\u0621' to intArrayOf(0xFE80, 0xFE80, 0xFE80, 0xFE80),
        '\u0622' to intArrayOf(0xFE81, 0xFE82, 0xFE81, 0xFE82),
        '\u0623' to intArrayOf(0xFE83, 0xFE84, 0xFE83, 0xFE84),
        '\u0624' to intArrayOf(0xFE85, 0xFE86, 0xFE85, 0xFE86),
        '\u0625' to intArrayOf(0xFE87, 0xFE88, 0xFE87, 0xFE88),
        '\u0626' to intArrayOf(0xFE89, 0xFE8A, 0xFE8B, 0xFE8C),
        '\u0627' to intArrayOf(0xFE8D, 0xFE8E, 0xFE8D, 0xFE8E),
        '\u0628' to intArrayOf(0xFE8F, 0xFE90, 0xFE91, 0xFE92),
        '\u0629' to intArrayOf(0xFE93, 0xFE94, 0xFE93, 0xFE94),
        '\u062A' to intArrayOf(0xFE95, 0xFE96, 0xFE97, 0xFE98),
        '\u062B' to intArrayOf(0xFE99, 0xFE9A, 0xFE9B, 0xFE9C),
        '\u062C' to intArrayOf(0xFE9D, 0xFE9E, 0xFE9F, 0xFEA0),
        '\u062D' to intArrayOf(0xFEA1, 0xFEA2, 0xFEA3, 0xFEA4),
        '\u062E' to intArrayOf(0xFEA5, 0xFEA6, 0xFEA7, 0xFEA8),
        '\u062F' to intArrayOf(0xFEA9, 0xFEAA, 0xFEA9, 0xFEAA),
        '\u0630' to intArrayOf(0xFEAB, 0xFEAC, 0xFEAB, 0xFEAC),
        '\u0631' to intArrayOf(0xFEAD, 0xFEAE, 0xFEAD, 0xFEAE),
        '\u0632' to intArrayOf(0xFEAF, 0xFEB0, 0xFEAF, 0xFEB0),
        '\u0633' to intArrayOf(0xFEB1, 0xFEB2, 0xFEB3, 0xFEB4),
        '\u0634' to intArrayOf(0xFEB5, 0xFEB6, 0xFEB7, 0xFEB8),
        '\u0635' to intArrayOf(0xFEB9, 0xFEBA, 0xFEBB, 0xFEBC),
        '\u0636' to intArrayOf(0xFEBD, 0xFEBE, 0xFEBF, 0xFEC0),
        '\u0637' to intArrayOf(0xFEC1, 0xFEC2, 0xFEC3, 0xFEC4),
        '\u0638' to intArrayOf(0xFEC5, 0xFEC6, 0xFEC7, 0xFEC8),
        '\u0639' to intArrayOf(0xFEC9, 0xFECA, 0xFECB, 0xFECC),
        '\u063A' to intArrayOf(0xFECD, 0xFECE, 0xFECF, 0xFED0),
        '\u0641' to intArrayOf(0xFED1, 0xFED2, 0xFED3, 0xFED4),
        '\u0642' to intArrayOf(0xFED5, 0xFED6, 0xFED7, 0xFED8),
        '\u0643' to intArrayOf(0xFED9, 0xFEDA, 0xFEDB, 0xFEDC),
        '\u0644' to intArrayOf(0xFEDD, 0xFEDE, 0xFEDF, 0xFEE0),
        '\u0645' to intArrayOf(0xFEE1, 0xFEE2, 0xFEE3, 0xFEE4),
        '\u0646' to intArrayOf(0xFEE5, 0xFEE6, 0xFEE7, 0xFEE8),
        '\u0647' to intArrayOf(0xFEE9, 0xFEEA, 0xFEEB, 0xFEEC),
        '\u0648' to intArrayOf(0xFEED, 0xFEEE, 0xFEED, 0xFEEE),
        '\u0649' to intArrayOf(0xFEEF, 0xFEF0, 0xFEEF, 0xFEF0),
        '\u064A' to intArrayOf(0xFEF1, 0xFEF2, 0xFEF3, 0xFEF4)
    )
    private val LAM_ALEF: Map<Char, IntArray> = mapOf(
        '\u0622' to intArrayOf(0xFEF5, 0xFEF6),
        '\u0623' to intArrayOf(0xFEF7, 0xFEF8),
        '\u0625' to intArrayOf(0xFEF9, 0xFEFA),
        '\u0627' to intArrayOf(0xFEFB, 0xFEFC)
    )
    private val DUAL: Set<Char> = setOf(
        '\u0626', '\u0628', '\u062A', '\u062B', '\u062C', '\u062D', '\u062E',
        '\u0633', '\u0634', '\u0635', '\u0636', '\u0637', '\u0638', '\u0639',
        '\u063A', '\u0641', '\u0642', '\u0643', '\u0644', '\u0645', '\u0646',
        '\u0647', '\u064A', '\u0640'
    )

    private fun joinsNext(c: Char) = DUAL.contains(c)
    private fun joinsPrev(c: Char) = FORMS.containsKey(c) && c != '\u0621'

    fun shape(input: String): String {
        if (input.isEmpty()) return input
        val sb = StringBuilder(input.length)
        val n = input.length
        var i = 0
        while (i < n) {
            val c = input[i]
            if (!FORMS.containsKey(c)) { sb.append(c); i++; continue }
            if (c == '\u0644' && i + 1 < n && LAM_ALEF.containsKey(input[i + 1])) {
                val prev = if (i > 0) input[i - 1] else null
                val joinPrev = prev != null && joinsNext(prev)
                val codes = LAM_ALEF[input[i + 1]]!!
                sb.append((if (joinPrev) codes[1] else codes[0]).toChar())
                i += 2
                continue
            }
            val prev = if (i > 0) input[i - 1] else null
            val next = if (i + 1 < n) input[i + 1] else null
            val joinPrev = prev != null && joinsNext(prev) && joinsPrev(c)
            val joinNext = next != null && joinsPrev(next) && joinsNext(c)
            val form = when {
                joinPrev && joinNext -> 3
                joinPrev -> 1
                joinNext -> 2
                else -> 0
            }
            sb.append(FORMS[c]!![form].toChar())
            i++
        }
        return sb.toString()
    }
}
