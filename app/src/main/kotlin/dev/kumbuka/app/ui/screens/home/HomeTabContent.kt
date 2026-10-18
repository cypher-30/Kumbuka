package dev.kumbuka.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import dev.kumbuka.app.ui.components.KbStatus
import dev.kumbuka.app.ui.components.KbStatusPill
import dev.kumbuka.app.ui.components.KbSurface
import dev.kumbuka.app.ui.components.KbTonalButton
import dev.kumbuka.app.ui.components.KbUnitLabel
import dev.kumbuka.app.ui.components.kbContentWidth
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** A deadline closer than this many days pulls its topics to the front and tints its date tile as urgent. */
private const val SoonDays = 3L

@Composable
fun HomeTabContent(
    now: LocalDateTime,
    loaded: Boolean,
    plan: TodayPlan,
    topicsById: Map<String, Topic>,
    unitCodes: Map<String, String>,
    deadlines: List<Deadline>,
    doneToday: Int,
    selectedTopicId: String?,
    listState: LazyListState,
    onStart: (TodayCard) -> Unit,
    onOpenTopic: (String) -> Unit,
    onImportPack: () -> Unit,
    onCreateUnit: () -> Unit,
    onTrySample: () -> Unit,
    onOpenUnits: () -> Unit,
    onOpenAssessments: () -> Unit,
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
    val soonDeadline = remember(upcoming, today) {
        upcoming.firstOrNull { daysFromToday(it.date, today) in 0..SoonDays }
    }
    val unitKeyFor: (TodayCard) -> String = { card -> topicsById[card.topicId]?.unitId ?: card.unitCode }
    val hasPlan = plan.state == TodayState.Today || plan.state == TodayState.FreshStart
    val dateText = remember(today, locale) { today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)) }
    val greeting = when (greetingPeriodFor(now.hour)) {
        GreetingPeriod.MORNING -> stringResource(R.string.greeting_morning)
        GreetingPeriod.AFTERNOON -> stringResource(R.string.greeting_afternoon)
        GreetingPeriod.EVENING -> stringResource(R.string.greeting_evening)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "header") {
            Column(modifier = Modifier.kbContentWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    dateText,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, letterSpacing = 0.26.sp),
                    color = LocalKbColors.current.inkMuted,
                )
                Text(
                    greeting,
                    style = MaterialTheme.typography.displayMedium,
                    color = LocalKbColors.current.ink,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }

        if (!loaded) {
            item(key = "loading") {
                Box(Modifier.kbContentWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LocalKbColors.current.primary)
                }
            }
            return@LazyColumn
        }

        if (hasPlan) {
            item(key = "hero") {
                TodayHero(
                    summary = pluralStringResource(R.plurals.home_hero_ready, plan.cards.size, plan.cards.size, totalMinutes),
                    doneLabel = stringResource(R.string.home_hero_done, doneToday, doneToday + plan.cards.size),
                    progress = doneToday.toFloat() / (doneToday + plan.cards.size).coerceAtLeast(1),
                    nextDate = upcoming.firstOrNull()?.let { d ->
                        stringResource(R.string.home_hero_next_date, d.title, relativeDayLabel(daysFromToday(d.date, today)).lowercase(locale))
                    },
                    onOpenNextDate = onOpenAssessments,
                    modifier = Modifier.kbContentWidth(),
                )
            }
        }

        when (plan.state) {
            TodayState.NoUnits -> item(key = "empty") {
                KbEmptyState(
                    icon = Icons.Outlined.AutoStories,
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.today_no_units_body),
                    primaryLabel = stringResource(R.string.import_pack_cta),
                    onPrimary = onImportPack,
                    secondaryLabel = stringResource(R.string.import_try_sample_cta),
                    onSecondary = onTrySample,
                    tertiaryLabel = stringResource(R.string.author_start_cta),
                    onTertiary = onCreateUnit,
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
                    tone = KbStatus.SUCCESS,
                    title = stringResource(R.string.today_all_caught_up_title),
                    body = stringResource(R.string.today_all_caught_up_body),
                    modifier = Modifier.kbContentWidth(),
                )
            }
            TodayState.FreshStart, TodayState.Today -> {
                if (plan.state == TodayState.FreshStart) {
                    item(key = "fresh") { KbBanner(stringResource(R.string.today_fresh_start_body), KbStatus.INFO, Modifier.kbContentWidth()) }
                } else if (soonDeadline != null) {
                    item(key = "due-soon") {
                        KbBanner(
                            text = stringResource(
                                R.string.today_due_soon_banner,
                                soonDeadline.title,
                                unitCodes[soonDeadline.unitId] ?: soonDeadline.unitId,
                                relativeDayLabel(daysFromToday(soonDeadline.date, today)).lowercase(locale),
                            ),
                            status = KbStatus.WARNING,
                            icon = Icons.Outlined.Schedule,
                            modifier = Modifier.kbContentWidth(),
                        )
                    }
                }
                if (featured != null) {
                    item(key = "featured-header") {
                        KbSectionHeader(title = stringResource(R.string.home_recommended_next), modifier = Modifier.kbContentWidth())
                    }
                    item(key = "featured-${featured.topicId}") {
                        FeaturedCard(
                            card = featured,
                            unitKey = unitKeyFor(featured),
                            selected = selectedTopicId == featured.topicId,
                            onOpen = { onOpenTopic(featured.topicId) },
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
                            selected = selectedTopicId == card.topicId,
                            onOpen = { onOpenTopic(card.topicId) },
                            onStart = { onStart(card) },
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

/**
 * The Midnight hero: today's summary, a saffron progress bar and the next
 * deadline as a tappable chip. The big translucent disc and small solid sun in
 * the corner are decoration only.
 */
@Composable
private fun TodayHero(
    summary: String,
    doneLabel: String,
    progress: Float,
    nextDate: String?,
    onOpenNextDate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKbColors.current
    Box(modifier = modifier.clip(RoundedCornerShape(24.dp)).background(colors.brand)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 18.dp, y = (-18).dp)
                .size(96.dp)
                .background(colors.accent.copy(alpha = 0.14f), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 14.dp)
                .size(28.dp)
                .background(colors.accent, CircleShape),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                summary,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold),
                color = colors.onHero,
                modifier = Modifier.padding(end = 48.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.onHero.copy(alpha = 0.16f))) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(colors.accent, RoundedCornerShape(3.dp)),
                    )
                }
                Text(doneLabel, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = colors.onHeroMuted)
            }
            if (nextDate != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.onHero.copy(alpha = 0.12f))
                        .clickable(onClick = onOpenNextDate)
                        .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.Event, contentDescription = null, tint = colors.onHero, modifier = Modifier.size(18.dp))
                    Text(
                        nextDate,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        color = colors.onHero,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedCard(
    card: TodayCard,
    unitKey: String,
    selected: Boolean,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    onWhy: () -> Unit,
    dueDays: Long?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKbColors.current
    KbSurface(
        modifier = modifier,
        onClick = onOpen,
        contentPadding = 18.dp,
        shape = RoundedCornerShape(24.dp),
        borderColor = if (selected) colors.primary else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KbUnitLabel(unitKey = unitKey, unitCode = card.unitCode, emphasized = true, modifier = Modifier.weight(1f))
            KbStatusPill(stringResource(R.string.today_minutes_short, card.minutes), KbStatus.INFO)
        }
        Text(
            card.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
            color = colors.ink,
        )
        if (card.objective.isNotBlank()) {
            Text(
                card.objective,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 22.sp),
                color = colors.inkMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(16.dp))
            Text(todayReasonText(card, dueDays), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = colors.inkMuted)
        }
        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KbPrimaryButton(
                text = stringResource(R.string.home_start_revision),
                onClick = onStart,
                icon = Icons.Filled.PlayArrow,
                fillWidth = false,
                modifier = Modifier.weight(1f),
            )
            KbTonalButton(text = stringResource(R.string.today_why_this), onClick = onWhy)
        }
    }
}

@Composable
private fun QueueRow(
    card: TodayCard,
    unitKey: String,
    selected: Boolean,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    dueDays: Long?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKbColors.current
    val startLabel = stringResource(R.string.home_start_topic_cd, card.title)
    KbListRow(
        onClick = onOpen,
        modifier = modifier,
        borderColor = if (selected) colors.primary else null,
        trailing = {
            FilledTonalIconButton(
                onClick = onStart,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colors.primaryTint, contentColor = colors.onPrimaryTint),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = startLabel)
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KbUnitLabel(unitKey = unitKey, unitCode = card.unitCode)
            Text(
                "· " + stringResource(R.string.today_minutes_short, card.minutes),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                color = colors.inkMuted,
            )
        }
        Text(
            card.title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp, fontWeight = FontWeight.Bold),
            color = colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            todayReasonText(card, dueDays),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            color = colors.inkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
        days <= SoonDays -> KbStatus.WARNING
        else -> KbStatus.INFO
    }
    KbListRow(
        onClick = onClick,
        modifier = modifier,
        leading = { KbDateTile(month = shortMonth(date, locale), day = date.dayOfMonth.toString(), status = status) },
    ) {
        Text(
            deadline.title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp, fontWeight = FontWeight.Bold),
            color = colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KbUnitLabel(unitKey = deadline.unitId, unitCode = unitCode)
            Text("·", color = colors.inkFaint, style = MaterialTheme.typography.labelMedium)
            Text(deadlineKindLabel(deadline.kind), style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp), color = colors.inkMuted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                relativeDayLabel(days),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                color = if (status == KbStatus.WARNING) colors.warning else colors.inkMuted,
            )
            if (linkedTopicCount != null && linkedTopicCount > 0) {
                Text(
                    pluralStringResource(R.plurals.assessments_linked_topics, linkedTopicCount, linkedTopicCount),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                    color = colors.inkFaint,
                )
            }
        }
    }
}
