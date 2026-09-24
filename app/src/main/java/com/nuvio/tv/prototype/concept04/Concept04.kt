package com.nuvio.tv.prototype.concept04

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
 * Concept 04 — Liquid Cinema.
 * Screens surface and sink like objects in water: a gentle scale from 96% with a fade,
 * never a hard slide. Rows keep the focused card centred so the displacement reads symmetrically.
 */
@Composable
fun Concept04(session: ProtoSession) {
    Box(Modifier.fillMaxSize().background(C04.Dark)) {
        ConceptHost(
            session = session,
            pivot = ScrollPivot.Center(durationMs = 360, easing = ProtoEasing.Decelerate),
            transition = { forward ->
                (fadeIn(tween(460, 60, ProtoEasing.Decelerate)) + scaleIn(tween(520, easing = ProtoEasing.Decelerate), initialScale = if (forward) 0.96f else 1.03f)) togetherWith
                    (fadeOut(tween(240)) + scaleOut(tween(300), targetScale = if (forward) 1.03f else 0.97f))
            },
        ) { route ->
            when (route) {
                ProtoRoute.Home -> C04Home(session)
                is ProtoRoute.Details -> C04Details(session, route.titleId)
                is ProtoRoute.Episodes -> C04Episodes(session, route.titleId, route.season)
                ProtoRoute.Search -> C04Search(session)
                ProtoRoute.Library -> C04Library(session)
                is ProtoRoute.Streams -> C04Streams(session, route.titleId)
                is ProtoRoute.Player -> C04Player(session, route.titleId, route.panel)
                ProtoRoute.Profile -> C04Profile(session)
            }
        }
    }
}
