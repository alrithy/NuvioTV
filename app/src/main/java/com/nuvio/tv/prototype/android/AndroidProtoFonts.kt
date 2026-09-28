package com.nuvio.tv.prototype.android

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.nuvio.tv.R
import com.nuvio.tv.prototype.shared.ProtoFonts

/** Bundled OFL fonts used only by the prototypes (res/font/proto_*). */
internal fun androidProtoFonts(): ProtoFonts {
    fun f(res: Int, weight: Int, italic: Boolean = false) =
        Font(res, FontWeight(weight), if (italic) FontStyle.Italic else FontStyle.Normal)

    return ProtoFonts(
        jost = FontFamily(f(R.font.proto_jost_300, 300), f(R.font.proto_jost_400, 400), f(R.font.proto_jost_500, 500)),
        // Reuses the app's existing variable Inter instead of shipping a second copy.
        inter = FontFamily(
            f(R.font.inter_variable, 300), f(R.font.inter_variable, 400), f(R.font.inter_variable, 500),
            f(R.font.inter_variable, 600), f(R.font.inter_variable, 700),
        ),
        alexandria = FontFamily(f(R.font.proto_alexandria_200, 200), f(R.font.proto_alexandria_300, 300), f(R.font.proto_alexandria_500, 500)),
        sora = FontFamily(f(R.font.proto_sora_200, 200), f(R.font.proto_sora_300, 300), f(R.font.proto_sora_600, 600)),
        readex = FontFamily(f(R.font.proto_readex_300, 300), f(R.font.proto_readex_400, 400), f(R.font.proto_readex_600, 600)),
        lexendExa = FontFamily(f(R.font.proto_lexendexa_200, 200), f(R.font.proto_lexendexa_400, 400)),
        notoKufi = FontFamily(f(R.font.proto_notokufi_300, 300), f(R.font.proto_notokufi_500, 500), f(R.font.proto_notokufi_700, 700)),
        manrope = FontFamily(f(R.font.proto_manrope_400, 400), f(R.font.proto_manrope_600, 600), f(R.font.proto_manrope_800, 800)),
        plexArabic = FontFamily(f(R.font.proto_plexarabic_300, 300), f(R.font.proto_plexarabic_400, 400), f(R.font.proto_plexarabic_600, 600)),
        plexMono = FontFamily(f(R.font.proto_plexmono_300, 300), f(R.font.proto_plexmono_400, 400), f(R.font.proto_plexmono_500, 500)),
        barlow = FontFamily(f(R.font.proto_barlow_300, 300), f(R.font.proto_barlow_400, 400), f(R.font.proto_barlow_500, 500), f(R.font.proto_barlow_600, 600)),
        cairo = FontFamily(f(R.font.proto_cairo_300, 300), f(R.font.proto_cairo_500, 500), f(R.font.proto_cairo_700, 700)),
        instrumentSerif = FontFamily(f(R.font.proto_instrumentserif_400, 400), f(R.font.proto_instrumentserif_400_italic, 400, italic = true)),
        instrumentSans = FontFamily(f(R.font.proto_instrumentsans_400, 400), f(R.font.proto_instrumentsans_500, 500), f(R.font.proto_instrumentsans_600, 600)),
        amiri = FontFamily(f(R.font.proto_amiri_400, 400), f(R.font.proto_amiri_700, 700)),
        outfit = FontFamily(f(R.font.proto_outfit_200, 200), f(R.font.proto_outfit_300, 300), f(R.font.proto_outfit_500, 500)),
        tajawal = FontFamily(f(R.font.proto_tajawal_200, 200), f(R.font.proto_tajawal_300, 300), f(R.font.proto_tajawal_500, 500)),
        reemKufi = FontFamily(f(R.font.proto_reemkufi_400, 400), f(R.font.proto_reemkufi_600, 600)),
        geist = FontFamily(f(R.font.proto_geist_300, 300), f(R.font.proto_geist_400, 400), f(R.font.proto_geist_500, 500), f(R.font.proto_geist_600, 600)),
    )
}
