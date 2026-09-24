package com.nuvio.tv.prototype.concept10

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
 * Concept 10 — Nuvio Signature.
 * Screens cross-fade through black with a two-percent settle: fast enough to feel direct, slow
 * enough to feel like cinema. Vertical scrolling moves only as far as it must; rows keep focus
 * on a fixed start line (see the home rows).
 */
@Composable
fun Concept10(session: ProtoSession) {
    Box(Modifier.fillMaxSize().background(C10.Black)) {
        ConceptHost(
            session = session,
            pivot = ScrollPivot.Padded(56f, 300, ProtoEasing.Decelerate),
            transition = { _ ->
                (fadeIn(tween(300, 90, ProtoEasing.Decelerate)) + scaleIn(tween(340, 90, ProtoEasing.Decelerate), initialScale = 1.02f)) togetherWith fadeOut(tween(180))
            },
        ) { route ->
            when (route) {
                ProtoRoute.Home -> C10Home(session)
                is ProtoRoute.Details -> C10Details(session, route.titleId)
                is ProtoRoute.Episodes -> C10Episodes(session, route.titleId, route.season)
                ProtoRoute.Search -> C10Search(session)
                ProtoRoute.Library -> C10Library(session)
                is ProtoRoute.Streams -> C10Streams(session, route.titleId)
                is ProtoRoute.Player -> C10Player(session, route.titleId, route.panel)
                ProtoRoute.Profile -> C10Profile(session)
            }
        }
    }
}

/** Top-level navigation from the auto-hiding bar. */
internal fun ProtoSession.goTo(section: C10Section) {
    val route = when (section) {
        C10Section.HOME -> null
        C10Section.SEARCH -> ProtoRoute.Search
        C10Section.LIBRARY -> ProtoRoute.Library
        C10Section.PROFILE -> ProtoRoute.Profile
    }
    when {
        route == null -> nav.home()
        nav.current == route -> Unit
        nav.canPop -> nav.replace(route)
        else -> nav.push(route)
    }
}
