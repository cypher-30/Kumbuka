package dev.kumbuka.app.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.scheduler.TodayCard
import dev.kumbuka.app.domain.scheduler.TodayPlan
import dev.kumbuka.app.domain.scheduler.TodayState
import dev.kumbuka.app.ui.components.KbBanner
import dev.kumbuka.app.ui.components.KbDateTile
import dev.kumbuka.app.ui.components.KbEmptyState
import dev.kumbuka.app.ui.components.KbListRow
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSectionHeader
import dev.kumbuka.app.ui.components.KbSettingsAction
import dev.kumbuka.app.ui.components.KbStatus
import dev.kumbuka.app.ui.components.KbStatusPill
import dev.kumbuka.app.ui.components.KbSurface
import dev.kumbuka.app.ui.components.KbUnitLabel
import dev.kumbuka.app.ui.components.kbContentWidth
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeTabContent(
    now: LocalDateTime,
    loaded: Boolean,
    plan: TodayPlan,
    topicsById: Map<String, Topic>,
    unitCodes: Map<String, String>,
    deadlines: List<Deadline>,
    recentReviews: Int,
    listState: LazyListState,
    onStart: (TodayCard) -> Unit,
    onOpenSettings: () -> Unit,
    onImportPack: () -> Unit,
    onCreateUnit: () -> Unit,
    onOpenUnits: () -> Unit,
    onOpenAssessments: () -> Unit,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = now.toLocalDate()
    val locale = currentLocale()
    var whyTopicId by rememberSaveable { mutableStateOf<String?>(null) }
    val upcoming = remember(deadlines, today) { upcomingPreview(deadlines, today) }
    val featured = plan.cards.firstOrNull()
    val queue = plan.cards.drop(1)
    val totalMinutes = plan.cards.sumOf { it.minutes }
    val dueDaysByTopic = remember(plan.cards, deadlines, today) {
        plan.cards.associate { it.topicId to calendarDueDays(it.topicId, deadlines, today) }
    }
    val hasDueToday = dueDaysByTopic.values.any { it == 0L }
    val unitKeyFor: (TodayCard) -> String = { card -> topicsById[card.topicId]?.unitId ?: card.unitCode }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "hero") {
            HomeHero(
                now = now,
                summary = when {
                    !loaded -> stringResource(R.string.generic_loading)
                    plan.state == TodayState.NoUnits -> stringResource(R.string.home_hero_setup)
                    plan.state == TodayState.NoActiveTopics -> stringResource(R.string.home_hero_no_active)
                    plan.state == TodayState.AllCaughtUp -> stringResource(R.string.home_hero_caught_up)
                    else -> pluralStringResource(R.plurals.home_hero_ready, plan.cards.size, plan.cards.size, totalMinutes)
                },
                nextDate = upcoming.firstOrNull()?.let { d ->
                    stringResource(R.string.home_hero_next_date, d.title, relativeDayLabel(daysFromToday(d.date, today)).lowercase(locale))
                },
                onOpenSettings = onOpenSettings,
                modifier = Modifier.kbContentWidth(),
            )
        }

        if (!loaded) {
            item(key = "loading") {
                Box(Modifier.kbContentWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LocalKbColors.current.primary)
                }
            }
            return@LazyColumn
        }

        when (plan.state) {
            TodayState.NoUnits -> item(key = "empty") {
                KbEmptyState(
                    icon = Icons.Outlined.AutoStories,
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.today_no_units_body),
                    primaryLabel = stringResource(R.string.import_pack_cta),
                    onPrimary = onImportPack,
                    secondaryLabel = stringResource(R.string.author_start_cta),
                    onSecondary = onCreateUnit,
                    modifier = Modifier.kbContentWidth(),
                )
            }
            TodayState.NoActiveTopics -> item(key = "no-active") {
                KbEmptyState(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.home_hero_no_active),
                    body = stringResource(R.string.today_no_active_topics_body),
                    primaryLabel = stringResource(R.string.home_open_units),
                    onPrimary = onOpenUnits,
                    modifier = Modifier.kbContentWidth(),
                )
            }
            TodayState.AllCaughtUp -> item(key = "caught-up") {
                KbEmptyState(
                    icon = Icons.Outlined.CheckCircle,
                    title = stringResource(R.string.today_all_caught_up_title),
                    body = stringResource(R.string.today_all_caught_up_body),
                    modifier = Modifier.kbContentWidth(),
                )
            }
            TodayState.FreshStart, TodayState.Today -> {
                if (plan.state == TodayState.FreshStart) {
                    item(key = "fresh") { KbBanner(stringResource(R.string.today_fresh_start_body), KbStatus.INFO, Modifier.kbContentWidth()) }
                } else if (hasDueToday) {
                    item(key = "due-today") { KbBanner(stringResource(R.string.today_due_today_body), KbStatus.WARNING, Modifier.kbContentWidth()) }
                }
                if (featured != null) {
                    item(key = "featured-header") {
                        KbSectionHeader(title = stringResource(R.string.home_recommended_next), modifier = Modifier.kbContentWidth())
                    }
                    item(key = "featured-${featured.topicId}") {
                        FeaturedCard(
                            card = featured,
                            unitKey = unitKeyFor(featured),
                            onStart = { onStart(featured) },
                            onWhy = { whyTopicId = featured.topicId },
                            dueDays = dueDaysByTopic[featured.topicId],
                            modifier = Modifier.kbContentWidth(),
                        )
                    }
                }
                if (queue.isNotEmpty()) {
                    item(key = "queue-header") {
                        KbSectionHeader(
                            title = stringResource(R.string.home_revision_queue),
                            supporting = stringResource(R.string.today_ranked_body, plan.sessionLengthMinutes),
                            modifier = Modifier.kbContentWidth(),
                        )
                    }
                    items(queue, key = { "queue-${it.topicId}" }) { card ->
                        QueueRow(
                            card = card,
                            unitKey = unitKeyFor(card),
                            onStart = { onStart(card) },
                            onWhy = { whyTopicId = card.topicId },
                            dueDays = dueDaysByTopic[card.topicId],
                            modifier = Modifier.kbContentWidth(),
                        )
                    }
                }
            }
        }

        if (plan.state != TodayState.NoUnits) {
            item(key = "coming-header") {
                KbSectionHeader(
                    title = stringResource(R.string.home_coming_up),
                    actionLabel = stringResource(R.string.generic_view_all),
                    onAction = onOpenAssessments,
                    modifier = Modifier.kbContentWidth(),
                )
            }
            if (upcoming.isEmpty()) {
                item(key = "coming-empty") {
                    KbSurface(modifier = Modifier.kbContentWidth()) {
                        Text(stringResource(R.string.home_coming_up_empty), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
                    }
                }
            } else {
                items(upcoming, key = { "coming-${it.id}" }) { deadline ->
                    UpcomingRow(
                        deadline = deadline,
                        unitCode = unitCodes[deadline.unitId] ?: deadline.unitId,
                        days = daysFromToday(deadline.date, today),
                        onClick = onOpenAssessments,
                        modifier = Modifier.kbContentWidth(),
                    )
                }
            }
            item(key = "progress-link") {
                KbListRow(
                    onClick = onOpenProgress,
                    leading = { Icon(Icons.Outlined.Insights, contentDescription = null, tint = LocalKbColors.current.primary) },
                    trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = LocalKbColors.current.inkMuted) },
                    modifier = Modifier.kbContentWidth().padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.home_progress_link_title), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
                    Text(
                        pluralStringResource(R.plurals.home_progress_link_body, recentReviews, recentReviews),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalKbColors.current.inkMuted,
                    )
                }
            }
        }
    }

    val whyCard = plan.cards.firstOrNull { it.topicId == whyTopicId }
    if (whyCard != null) {
        WhyThisSheet(
            card = whyCard,
            unitKey = unitKeyFor(whyCard),
            onDismiss = { whyTopicId = null },
            calendarDueDays = dueDaysByTopic[whyCard.topicId],
            onStart = {
                whyTopicId = null
                onStart(whyCard)
            },
        )
    }
}

@Composable
private fun HomeHero(
    now: LocalDateTime,
    summary: String,
    nextDate: String?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKbColors.current
    val locale = currentLocale()
    val period = greetingPeriodFor(now.hour)
    val greeting = when (period) {
        GreetingPeriod.MORNING -> stringResource(R.string.greeting_morning)
        GreetingPeriod.AFTERNOON -> stringResource(R.string.greeting_afternoon)
        GreetingPeriod.EVENING -> stringResource(R.string.greeting_evening)
    }
    val dateText = remember(now.toLocalDate(), locale) {
        now.toLocalDate().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(colors.heroEnd),
    ) {
        SkyMark(
            period = period,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 44.dp, end = 16.dp).size(52.dp),
        )
        Column(modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    dateText,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onHeroMuted,
                    modifier = Modifier.weight(1f),
                )
                KbSettingsAction(onOpenSettings = onOpenSettings, tint = colors.onHero)
            }
            Text(
                greeting,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onHero,
                modifier = Modifier.padding(end = 68.dp).semantics { heading() },
            )
            Text(
                summary,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onHero,
                modifier = Modifier.padding(top = 4.dp, end = 68.dp),
            )
            if (nextDate != null) {
                Text(
                    nextDate,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onHero,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 10.dp, end = 12.dp)
                        .background(colors.onHero.copy(alpha = 0.16f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** Decorative sun (day) or crescent moon (evening). Not announced to screen readers. */
@Composable
private fun SkyMark(period: GreetingPeriod, modifier: Modifier = Modifier) {
    val colors = LocalKbColors.current
    Canvas(modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val r = size.minDimension / 2f
        if (period == GreetingPeriod.EVENING) {
            drawCircle(color = colors.accent.copy(alpha = 0.18f), radius = r)
            drawCircle(color = colors.accent, radius = r * 0.62f)
            drawCircle(
                color = colors.accent,
                radius = r * 0.62f,
                center = Offset(center.x + r * 0.34f, center.y - r * 0.26f),
                blendMode = BlendMode.Clear,
            )
        } else {
            drawCircle(color = colors.accent.copy(alpha = 0.18f), radius = r)
            drawCircle(color = colors.accent.copy(alpha = 0.32f), radius = r * 0.78f)
            drawCircle(color = colors.accent, radius = r * 0.56f)
        }
    }
}

@Composable
private fun FeaturedCard(card: TodayCard, unitKey: String, onStart: () -> Unit, onWhy: () -> Unit, dueDays: Long?, modifier: Modifier = Modifier) {
    val colors = LocalKbColors.current
    KbSurface(modifier = modifier, contentPadding = 16.dp, borderColor = colors.primary.copy(alpha = 0.35f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KbUnitLabel(unitKey = unitKey, unitCode = card.unitCode, emphasized = true, modifier = Modifier.weight(1f))
            KbStatusPill(stringResource(R.string.today_minutes_short, card.minutes), KbStatus.INFO)
        }
        Text(card.title, style = MaterialTheme.typography.titleLarge, color = colors.ink)
        if (card.objective.isNotBlank()) {
            Text(card.objective, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Text(todayReasonText(card, dueDays), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        KbPrimaryButton(
            text = stringResource(R.string.home_start_revision),
            onClick = onStart,
            icon = Icons.Outlined.PlayArrow,
            modifier = Modifier.padding(top = 4.dp),
        )
        TextButton(onClick = onWhy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.today_why_this), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun QueueRow(card: TodayCard, unitKey: String, onStart: () -> Unit, onWhy: () -> Unit, dueDays: Long?, modifier: Modifier = Modifier) {
    val colors = LocalKbColors.current
    val startLabel = stringResource(R.string.home_start_topic_cd, card.title)
    KbListRow(
        onClick = onWhy,
        modifier = modifier,
        trailing = {
            FilledTonalIconButton(
                onClick = onStart,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colors.primaryTint, contentColor = colors.onPrimaryTint),
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = startLabel)
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KbUnitLabel(unitKey = unitKey, unitCode = card.unitCode)
            Text(stringResource(R.string.today_minutes_short, card.minutes), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        }
        Text(card.title, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(todayReasonText(card, dueDays), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun UpcomingRow(
    deadline: Deadline,
    unitCode: String,
    days: Long,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    linkedTopicCount: Int? = null,
) {
    val colors = LocalKbColors.current
    val locale = currentLocale()
    val date = storedCalendarDate(deadline.date)
    val status = when {
        days < 0 -> KbStatus.NEUTRAL
        days <= 2 -> KbStatus.WARNING
        else -> KbStatus.INFO
    }
    KbListRow(
        onClick = onClick,
        modifier = modifier,
        leading = { KbDateTile(month = shortMonth(date, locale), day = date.dayOfMonth.toString(), status = status) },
    ) {
        Text(deadline.title, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KbUnitLabel(unitKey = deadline.unitId, unitCode = unitCode)
            Text("·", color = colors.inkFaint, style = MaterialTheme.typography.labelMedium)
            Text(deadlineKindLabel(deadline.kind), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                relativeDayLabel(days),
                style = MaterialTheme.typography.labelMedium,
                color = if (status == KbStatus.WARNING) colors.warning else colors.inkMuted,
            )
            if (linkedTopicCount != null && linkedTopicCount > 0) {
                Text(
                    pluralStringResource(R.plurals.assessments_linked_topics, linkedTopicCount, linkedTopicCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.inkFaint,
                )
            }
        }
    }
}
