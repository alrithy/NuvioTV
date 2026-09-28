package com.nuvio.tv.prototype.concept01

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
 * Concept 01 — Cinematic Black.
 * Navigation is a dissolve between frames: every screen change is a cut to black and a fade up,
 * the way a film moves between scenes. The focused reel frame always sits at the reel's start.
 */
@Composable
fun Concept01(session: ProtoSession) {
    Box(Modifier.fillMaxSize().background(C01.Black)) {
        ConceptHost(
            session = session,
            pivot = ScrollPivot.Anchor(fraction = 0f, durationMs = 420, easing = ProtoEasing.Cinematic),
            transition = {
                fadeIn(tween(520, delayMillis = 180, easing = ProtoEasing.Cinematic)) togetherWith fadeOut(tween(200))
            },
        ) { route ->
            when (route) {
                ProtoRoute.Home -> C01Home(session)
                is ProtoRoute.Details -> C01Details(session, route.titleId)
                is ProtoRoute.Episodes -> C01Episodes(session, route.titleId, route.season)
                ProtoRoute.Search -> C01Search(session)
                ProtoRoute.Library -> C01Library(session)
                is ProtoRoute.Streams -> C01Streams(session, route.titleId)
                is ProtoRoute.Player -> C01Player(session, route.titleId, route.panel)
                ProtoRoute.Profile -> C01Profile(session)
            }
        }
    }
}
