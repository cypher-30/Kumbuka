package dev.kumbuka.app.domain.model

/** Kinds of dated work a unit can carry; shown on Today's "Coming up" list. */
enum class DeadlineKind { CAT, ASSIGNMENT, EXAM }

/** Kinds of saved result; [PAST_PAPER] exists only for marks, not deadlines. */
enum class AssessmentKind { CAT, ASSIGNMENT, PAST_PAPER, EXAM }
