package com.nuvio.tv.prototype.concept02

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
 * Concept 02 — Riyadh After Dark.
 * Screens rise gently into place like a lift arriving at a floor: short upward drift + fade.
 */
@Composable
fun Concept02(session: ProtoSession) {
    Box(Modifier.fillMaxSize().background(C02.Night)) {
        ConceptHost(
            session = session,
            pivot = ScrollPivot.Padded(paddingDp = 48f, durationMs = 280, easing = ProtoEasing.Decelerate),
            transition = { forward ->
                (fadeIn(tween(420, 80, ProtoEasing.Decelerate)) + slideInVertically(tween(480, easing = ProtoEasing.Decelerate)) { if (forward) it / 18 else -it / 18 }) togetherWith
                    fadeOut(tween(200))
            },
        ) { route ->
            when (route) {
                ProtoRoute.Home -> C02Home(session)
                is ProtoRoute.Details -> C02Details(session, route.titleId)
                is ProtoRoute.Episodes -> C02Episodes(session, route.titleId, route.season)
                ProtoRoute.Search -> C02Search(session)
                ProtoRoute.Library -> C02Library(session)
                is ProtoRoute.Streams -> C02Streams(session, route.titleId)
                is ProtoRoute.Player -> C02Player(session, route.titleId, route.panel)
                ProtoRoute.Profile -> C02Profile(session)
            }
        }
    }
}
