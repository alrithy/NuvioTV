package com.nuvio.tv.prototype.concept06

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
 * Concept 06 — Luxury Media Console.
 * Screen changes are quick and mechanical: a 12dp rise with a short fade, like a display
 * switching pages in a car. Scrolling only moves when focus reaches the padded edge.
 */
@Composable
fun Concept06(session: ProtoSession) {
    Box(Modifier.fillMaxSize().background(C06.Base)) {
        ConceptHost(
            session = session,
            pivot = ScrollPivot.Padded(paddingDp = 24f, durationMs = 220, easing = ProtoEasing.Detent),
            transition = { (fadeIn(tween(220, 40, ProtoEasing.Detent)) + slideInVertically(tween(260, easing = ProtoEasing.Detent)) { 24 }) togetherWith fadeOut(tween(120)) },
        ) { route ->
            when (route) {
                ProtoRoute.Home -> C06Home(session)
                is ProtoRoute.Details -> C06Details(session, route.titleId)
                is ProtoRoute.Episodes -> C06Episodes(session, route.titleId, route.season)
                ProtoRoute.Search -> C06Search(session)
                ProtoRoute.Library -> C06Library(session)
                is ProtoRoute.Streams -> C06Streams(session, route.titleId)
                is ProtoRoute.Player -> C06Player(session, route.titleId, route.panel)
                ProtoRoute.Profile -> C06Profile(session)
            }
        }
    }
}
