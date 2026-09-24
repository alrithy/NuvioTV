package com.nuvio.tv.prototype.concept09

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
 * Concept 09 — Future Arabia.
 * Navigation moves through depth: going forward, the next screen arrives from slightly behind the
 * glass and the current one steps back; going back reverses it. Rails keep focus a fifth of the
 * way in from the reading edge so the curve always bends away from the same place.
 */
@Composable
fun Concept09(session: ProtoSession) {
    Box(Modifier.fillMaxSize().background(C09.Obsidian)) {
        ConceptHost(
            session = session,
            pivot = ScrollPivot.Anchor(fraction = 0.2f, durationMs = 360, easing = ProtoEasing.Decelerate),
            transition = { forward ->
                (fadeIn(tween(340, 80, ProtoEasing.Decelerate)) + scaleIn(tween(420, easing = ProtoEasing.Decelerate), initialScale = if (forward) 0.94f else 1.04f)) togetherWith
                    (fadeOut(tween(220)) + scaleOut(tween(320, easing = ProtoEasing.Exit), targetScale = if (forward) 1.04f else 0.94f))
            },
        ) { route ->
            when (route) {
                ProtoRoute.Home -> C09Home(session)
                is ProtoRoute.Details -> C09Details(session, route.titleId)
                is ProtoRoute.Episodes -> C09Episodes(session, route.titleId, route.season)
                ProtoRoute.Search -> C09Search(session)
                ProtoRoute.Library -> C09Library(session)
                is ProtoRoute.Streams -> C09Streams(session, route.titleId)
                is ProtoRoute.Player -> C09Player(session, route.titleId, route.panel)
                ProtoRoute.Profile -> C09Profile(session)
            }
        }
    }
}

/** Rail navigation between the four top-level sections. */
internal fun ProtoSession.goTo(section: C09Section) {
    when (section) {
        C09Section.TONIGHT -> nav.home()
        C09Section.SEARCH -> if (nav.canPop) nav.replace(ProtoRoute.Search) else nav.push(ProtoRoute.Search)
        C09Section.LIBRARY -> if (nav.canPop) nav.replace(ProtoRoute.Library) else nav.push(ProtoRoute.Library)
        C09Section.PROFILE -> if (nav.canPop) nav.replace(ProtoRoute.Profile) else nav.push(ProtoRoute.Profile)
    }
}
