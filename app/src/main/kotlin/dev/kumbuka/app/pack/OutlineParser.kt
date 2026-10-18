package dev.kumbuka.app.pack

import dev.kumbuka.app.domain.model.DeadlineKind
import java.time.LocalDate
import java.time.Month

/** A dated line found in a pasted outline, e.g. "CAT 1 - 6th Nov (topics 1-3)". */
data class OutlineDate(
    val title: String,
    val day: Int,
    val month: Month,
    /** 1-based topic range from "(topics 1-3)", or null when the line named none. */
    val topicRange: IntRange?,
) {
    val kind: DeadlineKind
        get() = when {
            title.contains("exam", ignoreCase = true) -> DeadlineKind.EXAM
            title.contains("assign", ignoreCase = true) || title.contains("lab", ignoreCase = true) ||
                title.contains("project", ignoreCase = true) || title.contains("report", ignoreCase = true) -> DeadlineKind.ASSIGNMENT
            else -> DeadlineKind.CAT
        }

    /** The next occurrence of this day and month on or after [today]; 29 Feb falls back to 28 Feb in non-leap years. */
    fun resolve(today: LocalDate): LocalDate {
        fun inYear(year: Int): LocalDate = runCatching { LocalDate.of(year, month, day) }
            .getOrElse { LocalDate.of(year, month, month.length(java.time.Year.isLeap(year.toLong())).coerceAtMost(day)) }
        val thisYear = inYear(today.year)
        return if (thisYear.isBefore(today)) inYear(today.year + 1) else thisYear
    }
}

data class ParsedOutline(
    val code: String,
    val title: String,
    val topics: List<String>,
    val dates: List<OutlineDate>,
    val references: List<String>,
    /** Lines that matched nothing, shown to the student so nothing is silently dropped. */
    val skipped: List<String>,
) {
    val isUsable: Boolean get() = topics.isNotEmpty()
}

/**
 * Turns pasted course-outline text into a unit: the first "ICS 3108 Title" line
 * names the unit, numbered or bulleted lines become topics, lines with a day and
 * month become dates (optionally linking "topics a-b"), and "Ref:" lines become
 * resource pointers. A port of the design prototype's parseOutline().
 */
object OutlineParser {
    private val codeLine = Regex("""^([A-Z]{2,4})\s?(\d{3,4})\s*[-:–]?\s*(.*)$""", RegexOption.IGNORE_CASE)
    private val dateInLine = Regex("""(\d{1,2})(?:st|nd|rd|th)?\s+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*""", RegexOption.IGNORE_CASE)
    private val topicRange = Regex("""topics?\s*(\d+)\s*(?:-|–|to)\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val bullet = Regex("""^(?:\d+[.)]|[-•*])\s*(.+)$""")
    private val refLine = Regex("""^(?:refs?|references|reading)\s*[:\-]\s*(.+)$""", RegexOption.IGNORE_CASE)
    private val dateTitleSplit = Regex("""\s[-–:]\s|\(""")

    fun parse(text: String): ParsedOutline {
        var code = ""
        var title = ""
        val topics = ArrayList<String>()
        val dates = ArrayList<OutlineDate>()
        val refs = ArrayList<String>()
        val skipped = ArrayList<String>()

        text.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            if (code.isEmpty()) {
                codeLine.matchEntire(line)?.let { m ->
                    code = m.groupValues[1].uppercase() + " " + m.groupValues[2]
                    title = titleCase(m.groupValues[3])
                    return@forEach
                }
            }
            dateInLine.find(line)?.let { m ->
                val range = topicRange.find(line)?.let { r ->
                    val from = r.groupValues[1].toInt()
                    val to = r.groupValues[2].toInt()
                    if (from <= to) from..to else null
                }
                dates += OutlineDate(
                    title = line.split(dateTitleSplit).first().trim(),
                    day = m.groupValues[1].toInt(),
                    month = monthOf(m.groupValues[2]),
                    topicRange = range,
                )
                return@forEach
            }
            bullet.matchEntire(line)?.let { m ->
                topics += m.groupValues[1].trim()
                return@forEach
            }
            refLine.matchEntire(line)?.let { m ->
                refs += m.groupValues[1].split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
                return@forEach
            }
            skipped += line
        }
        return ParsedOutline(code, title, topics, dates, refs, skipped)
    }

    private fun titleCase(raw: String): String =
        raw.lowercase().replace(Regex("""\b[a-z]""")) { it.value.uppercase() }.trim()

    private fun monthOf(abbreviation: String): Month = when (abbreviation.take(3).lowercase()) {
        "jan" -> Month.JANUARY
        "feb" -> Month.FEBRUARY
        "mar" -> Month.MARCH
        "apr" -> Month.APRIL
        "may" -> Month.MAY
        "jun" -> Month.JUNE
        "jul" -> Month.JULY
        "aug" -> Month.AUGUST
        "sep" -> Month.SEPTEMBER
        "oct" -> Month.OCTOBER
        "nov" -> Month.NOVEMBER
        else -> Month.DECEMBER
    }
}
