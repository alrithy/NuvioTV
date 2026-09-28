package com.nuvio.tv.prototype.concept08

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
 * Concept 08 — Ambient TV OS.
 * Screens fade through black slowly, like a lamp dimming and coming back up. Nothing slides.
 */
@Composable
fun Concept08(session: ProtoSession) {
    Box(Modifier.fillMaxSize().background(C08.Black)) {
        ConceptHost(
            session = session,
            pivot = ScrollPivot.Center(durationMs = 380, easing = ProtoEasing.Decelerate),
            transition = { fadeIn(tween(700, 250, ProtoEasing.Decelerate)) togetherWith fadeOut(tween(350)) },
        ) { route ->
            when (route) {
                ProtoRoute.Home -> C08Home(session)
                is ProtoRoute.Details -> C08Details(session, route.titleId)
                is ProtoRoute.Episodes -> C08Episodes(session, route.titleId, route.season)
                ProtoRoute.Search -> C08Search(session)
                ProtoRoute.Library -> C08Library(session)
                is ProtoRoute.Streams -> C08Streams(session, route.titleId)
                is ProtoRoute.Player -> C08Player(session, route.titleId, route.panel)
                ProtoRoute.Profile -> C08Profile(session)
            }
        }
    }
}
