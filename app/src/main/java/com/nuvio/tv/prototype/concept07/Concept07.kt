package com.nuvio.tv.prototype.concept07

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nuvio.tv.prototype.shared.ConceptHost
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.ScrollPivot

/**
 * Concept 07 — Editorial Cinema.
 * Navigating is turning a page: the new page slides in from the reading edge over the old one,
 * which recedes slightly. The page scrolls so the focused block sits a third of the way down.
 */
@Composable
fun Concept07(session: ProtoSession) {
    Box(Modifier.fillMaxSize().background(C07.Paper)) {
        ConceptHost(
            session = session,
            pivot = ScrollPivot.Anchor(fraction = 0.18f, durationMs = 420, easing = ProtoEasing.Standard),
            transition = { forward ->
                (slideInHorizontally(tween(480, easing = ProtoEasing.Standard)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(360, 60))) togetherWith
                    (slideOutHorizontally(tween(480, easing = ProtoEasing.Standard)) { if (forward) -it / 10 else it / 10 } + fadeOut(tween(260)))
            },
        ) { route ->
            when (route) {
                ProtoRoute.Home -> C07Home(session)
                is ProtoRoute.Details -> C07Details(session, route.titleId)
                is ProtoRoute.Episodes -> C07Episodes(session, route.titleId, route.season)
                ProtoRoute.Search -> C07Search(session)
                ProtoRoute.Library -> C07Library(session)
                is ProtoRoute.Streams -> C07Streams(session, route.titleId)
                is ProtoRoute.Player -> C07Player(session, route.titleId, route.panel)
                ProtoRoute.Profile -> C07Profile(session)
            }
        }
    }
}
