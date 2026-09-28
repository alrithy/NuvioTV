package com.nuvio.tv.prototype.concept03

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
 * Concept 03 — Desert Monolith.
 * Screens move like heavy stone: a slow lateral slide with a long deceleration.
 * Lists keep the focused name centred, so the eye never has to travel.
 */
@Composable
fun Concept03(session: ProtoSession) {
    Box(Modifier.fillMaxSize().background(C03.Stone)) {
        ConceptHost(
            session = session,
            pivot = ScrollPivot.Center(durationMs = 440, easing = ProtoEasing.Cinematic),
            transition = { forward ->
                (fadeIn(tween(520, 120, ProtoEasing.Cinematic)) + slideInHorizontally(tween(620, easing = ProtoEasing.Cinematic)) { if (forward) it / 10 else -it / 10 }) togetherWith
                    (fadeOut(tween(260)) + slideOutHorizontally(tween(520, easing = ProtoEasing.Cinematic)) { if (forward) -it / 14 else it / 14 })
            },
        ) { route ->
            when (route) {
                ProtoRoute.Home -> C03Home(session)
                is ProtoRoute.Details -> C03Details(session, route.titleId)
                is ProtoRoute.Episodes -> C03Episodes(session, route.titleId, route.season)
                ProtoRoute.Search -> C03Search(session)
                ProtoRoute.Library -> C03Library(session)
                is ProtoRoute.Streams -> C03Streams(session, route.titleId)
                is ProtoRoute.Player -> C03Player(session, route.titleId, route.panel)
                ProtoRoute.Profile -> C03Profile(session)
            }
        }
    }
}
