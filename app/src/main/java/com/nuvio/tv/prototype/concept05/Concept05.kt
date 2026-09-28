package com.nuvio.tv.prototype.concept05

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nuvio.tv.prototype.shared.ConceptHost
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.ScrollPivot

/**
 * Concept 05 — Zero Chrome.
 * Screen changes are plain cross-dissolves: the picture is the constant, only words change.
 */
@Composable
fun Concept05(session: ProtoSession) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ConceptHost(
            session = session,
            pivot = ScrollPivot.Center(durationMs = 300),
            transition = { fadeIn(tween(420)) togetherWith fadeOut(tween(420)) },
        ) { route ->
            when (route) {
                ProtoRoute.Home -> C05Home(session)
                is ProtoRoute.Details -> C05Details(session, route.titleId)
                is ProtoRoute.Episodes -> C05Episodes(session, route.titleId, route.season)
                ProtoRoute.Search -> C05Search(session)
                ProtoRoute.Library -> C05Library(session)
                is ProtoRoute.Streams -> C05Streams(session, route.titleId)
                is ProtoRoute.Player -> C05Player(session, route.titleId, route.panel)
                ProtoRoute.Profile -> C05Profile(session)
            }
        }
    }
}
