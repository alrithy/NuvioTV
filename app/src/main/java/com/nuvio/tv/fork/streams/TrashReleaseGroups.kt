package com.nuvio.tv.fork.streams

/**
 * Release-group tiers for the G8b ranker (feature 160). Data from Cxsmo `TrashReleaseGroups` @
 * 3e0d0fa, itself extracted from the TRaSH Guides Radarr custom formats (github.com/TRaSH-Guides/Guides,
 * docs/json/radarr/cf, commit 1027cd5; MIT, Copyright (c) 2021 TRaSH). Kept as tiers rather than one
 * flat ladder: groups inside a tier are listed alphabetically upstream, so only the tier carries
 * meaning. [LOW_QUALITY] (TRaSH LQ, Bad Dual Groups, Generated Dynamic HDR) ranks below unknown
 * groups; nothing here removes a stream. Matched case-insensitively against official's parsed group.
 */
object TrashReleaseGroups {

    val TIERS: List<List<String>> = listOf(
        // Remux Tier 01
        listOf("3L", "BiZKiT", "BLURANiUM", "BMF", "CiNEPHiLES", "FraMeSToR", "PiRAMiDHEAD", "PmP", "WiLDCAT", "ZQ"),
        // Remux Tier 02
        listOf("ATELiER", "NCmt", "playBD", "SiCFoI", "SURFINBIRD", "TEPES"),
        // Remux Tier 03
        listOf(
            "12GaugeShotgun", "decibeL", "EPSiLON", "HiFi", "iFT", "KRaLiMaRKo", "NTb", "PTP", "SumVision", "TOA",
            "TRiToN",
        ),
        // UHD BluRay Tier 01
        listOf("CtrlHD", "MainFrame", "DON", "W4NK3R"),
        // UHD BluRay Tier 02
        listOf("HiDt", "HQMUX"),
        // UHD BluRay Tier 03
        listOf("BHDStudio", "hallowed", "HONE", "PTer", "SPHD", "WEBDV"),
        // HD BluRay Tier 01
        listOf(
            "BBQ", "c0kE", "Chotab", "CRiSC", "D-Z0N3", "Dariush", "EbP", "EDPH", "Geek", "LolHD", "TayTO", "TDD",
            "TnP", "VietHD", "ZoroSenpai",
        ),
        // HD BluRay Tier 02
        listOf("EA", "HiSD", "QOQ", "SA89", "sbR"),
        // HD BluRay Tier 03
        listOf("LoRD", "playHD"),
        // WEB Tier 01
        listOf(
            "ABBIE", "AJP69", "APEX", "PAXA", "PEXA", "XEPA", "BLUTONiUM", "BYNDR", "CMRG", "CRFW", "CRUD", "FLUX",
            "GNOME", "KiNGS", "Kitsune", "MADSKY", "NOSiViD", "NTG", "RAWR", "SiC", "TheFarm",
        ),
        // WEB Tier 02
        listOf("dB", "Flights", "MiU", "monkee", "MZABI", "PHOENiX", "playWEB", "SMURF", "TOMMY", "XEBEC", "4KBEC", "CEBEX"),
        // WEB Tier 03
        listOf("BLOOM", "Dooky", "GNOMiSSiON", "HHWEB", "NINJACENTRAL", "NPMS", "ROCCaT", "SiGMA", "SLiGNOME", "SwAgLaNdEr"),
    )

    val LOW_QUALITY: List<String> = listOf(
        // LQ
        "24xHD", "41RGB", "4K4U", "AOC", "AROMA", "aXXo", "AZAZE", "BARC0DE", "BAUCKLEY", "BdC", "beAst", "BTM",
        "C1NEM4", "C4K", "CDDHD", "CHAOS", "CHD", "CiNE", "CLEANUP", "COLLECTiVE", "CREATiVE24", "CrEwSaDe", "CTFOH",
        "d3g", "DDR", "DNL", "DRX", "E", "EPiC", "EuReKA", "FaNGDiNG0", "Feranki1980", "FGT", "FMD", "FRDS", "FS",
        "FZHD", "GalaxyRG", "GHD", "GPTHD", "HDHUB4U", "HDS", "HDT", "HDTime", "HDWinG", "iNTENSO", "iPlanet", "iVy",
        "jennaortegaUHD", "jennaortega", "JFF", "KC", "KiNGDOM", "KIRA", "L0SERNIGHT", "LAMA", "Leffe", "Liber8",
        "LiGaS", "LUCY", "MarkII", "MeGusta", "Mesc", "mHD", "mSD", "MTeam", "MT", "MySiLU", "NhaNc3", "nHD",
        "nikt0", "NoGroup", "NoGrp", "nSD", "OFT", "Pahe.ph", "Pahe", "Pahe.in", "PATOMiEL", "PRODJi", "PSA", "PTNK",
        "RARBG", "RBB", "RDN", "Rifftrax", "RU4HD", "SANTi", "Scene", "SHD", "ShieldBearer", "STUTTERSHIT",
        "SUNSCREEN", "SyncUP", "TBS", "TEKNO3D", "Tigole", "TIKO", "VISIONPLUSHDR-X", "VISIONPLUSHDR1000",
        "VISIONPLUSHDR", "WAF", "WiKi", "x0r", "YIFY", "YTS.MX", "YTS", "YTS.LT", "YTS.AG", "Zeus",
        // Bad Dual Groups
        "alfaHD", "BAT", "BlackBit", "BNd", "C.A.A", "C76", "Cory", "CYPHER", "EniaHD", "EXTREME", "FF", "FOXX",
        "G4RiS", "GUEIRA", "LCD", "MGE", "MLH", "N3G4N", "ONLYMOViE", "PD", "PTHome", "RiPER", "RK", "SiGLA", "Tars",
        "TM", "tokar86a", "TURG", "TvR", "vnlls", "WTV", "XiQUEXiQUE", "Yatogam1", "YusukeFLA", "ZigZag", "ZNM",
        // Generated Dynamic HDR ("Flights" stays in WEB Tier 02, as upstream's preferred listing wins)
        "BiTOR", "DepraveD", "GuyZo", "BR-GuyZo", "SasukeducK", "tarunk9c", "VD0N", "VECTOR", "VisionXpert",
    )

    private val tierByGroup: Map<String, Int> = buildMap {
        TIERS.forEachIndexed { tier, groups -> groups.forEach { putIfAbsent(it.lowercase(), tier) } }
    }
    private val lowQuality: Set<String> = LOW_QUALITY.mapTo(HashSet()) { it.lowercase() }

    /** Tier of an unlisted (or missing) group, between the listed tiers and [LOW_QUALITY]. */
    val UNKNOWN_TIER: Int = TIERS.size
    val LOW_QUALITY_TIER: Int = TIERS.size + 1

    /** Official's parser returns `group.mkv` when the file name ends the stream text. */
    private val FILE_EXTENSION = Regex("\\.(mkv|mp4|m4v|avi|ts|m2ts|webm)$")

    fun tier(group: String?): Int {
        val key = group?.trim()?.lowercase()?.replace(FILE_EXTENSION, "").orEmpty()
        if (key.isEmpty()) return UNKNOWN_TIER
        return tierByGroup[key] ?: if (key in lowQuality) LOW_QUALITY_TIER else UNKNOWN_TIER
    }
}
