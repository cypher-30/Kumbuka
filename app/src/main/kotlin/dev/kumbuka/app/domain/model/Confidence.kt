package dev.kumbuka.app.domain.model

/**
 * DESIGN.md §3: the fuzzy self-rated signal, taken twice per session -
 * once before restudying (the label the recall model learns from) and
 * once after (which seeds the next interval).
 */
enum class Confidence(val score: Int) {
    BLANK(0),
    SHAKY(1),
    OK(2),
    SOLID(3),
    ;

    /** 0f (fully recalled) .. 1f (nothing recalled) - the `gap` term in §4's formula. */
    fun asGap(): Float = 1f - (score / SOLID.score.toFloat())

    companion object {
        fun fromScore(score: Int): Confidence = entries.first { it.score == score }
    }
}
