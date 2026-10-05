package com.nuvio.tv.ui.util

import com.nuvio.tv.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogNameFormatterTest {
    @Test
    fun wellKnownCatalogNamesAreLocalized() {
        assertEquals(R.string.catalog_name_popular, catalogNameResource("Popular"))
        assertEquals(R.string.catalog_name_top_rated, catalogNameResource("  top   RATED "))
        assertEquals(R.string.catalog_name_decades, catalogNameResource("Decades"))
        assertEquals(R.string.catalog_name_series, catalogNameResource("TV Shows"))
    }

    @Test
    fun brandAndCustomNamesStayAsTheyAre() {
        assertNull(catalogNameResource("Netflix"))
        assertNull(catalogNameResource("Cinemeta Popular Picks"))
        assertNull(catalogNameResource(""))
    }
}
