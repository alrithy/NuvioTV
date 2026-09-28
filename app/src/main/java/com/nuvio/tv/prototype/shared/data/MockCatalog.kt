package com.nuvio.tv.prototype.shared.data

import androidx.compose.ui.graphics.Color
import com.nuvio.tv.prototype.shared.Bi

/**
 * Mock content shared by all ten concepts so visual differences are easy to compare.
 * Synopses are written for the prototype; ratings are illustrative.
 */
object MockCatalog {

    // --- Vocabulary -----------------------------------------------------------------------------

    private object G {
        val SciFi = Bi("Science Fiction", "خيال علمي")
        val Adventure = Bi("Adventure", "مغامرة")
        val Drama = Bi("Drama", "دراما")
        val History = Bi("History", "تاريخي")
        val Thriller = Bi("Thriller", "إثارة")
        val Crime = Bi("Crime", "جريمة")
        val Action = Bi("Action", "أكشن")
        val Romance = Bi("Romance", "رومانسي")
        val Comedy = Bi("Comedy", "كوميديا")
        val Mystery = Bi("Mystery", "غموض")
        val Fantasy = Bi("Fantasy", "فانتازيا")
        val Family = Bi("Family", "عائلي")
    }

    private object C {
        val USA = Bi("United States", "الولايات المتحدة")
        val UK = Bi("United Kingdom", "المملكة المتحدة")
        val Saudi = Bi("Saudi Arabia", "السعودية")
        val Jordan = Bi("Jordan", "الأردن")
        val Lebanon = Bi("Lebanon", "لبنان")
        val Korea = Bi("South Korea", "كوريا الجنوبية")
    }

    private fun cast(en: String, ar: String, roleEn: String, roleAr: String) = CastMember(Bi(en, ar), Bi(roleEn, roleAr))
    private fun p(sky: Long, horizon: Long, mid: Long, ground: Long, accent: Long, ui: Long) =
        ArtPalette(Color(sky), Color(horizon), Color(mid), Color(ground), Color(accent), Color(ui))

    private fun genericChapters(): List<Chapter> = listOf(
        Chapter(Bi("Opening", "الافتتاحية"), 0f),
        Chapter(Bi("Chapter II", "الفصل الثاني"), 0.16f),
        Chapter(Bi("Chapter III", "الفصل الثالث"), 0.34f),
        Chapter(Bi("Chapter IV", "الفصل الرابع"), 0.52f),
        Chapter(Bi("Chapter V", "الفصل الخامس"), 0.7f),
        Chapter(Bi("Finale", "الختام"), 0.86f),
    )

    private val genericEpisodeLines = listOf(
        Bi("Old loyalties are tested as a new threat surfaces.", "تُختبر الولاءات القديمة مع ظهور تهديد جديد."),
        Bi("A quiet decision sets everything in motion.", "قرار صامت يحرّك كل شيء."),
        Bi("The team is forced to improvise when the plan collapses.", "يضطر الفريق للارتجال حين تنهار الخطة."),
        Bi("A reunion no one wanted brings the truth closer.", "لقاء لم يرغب به أحد يقرّب الحقيقة."),
        Bi("Pressure builds on every side before a long night.", "يتصاعد الضغط من كل جانب قبل ليلة طويلة."),
        Bi("An unexpected ally changes the balance of power.", "حليف غير متوقع يغيّر موازين القوة."),
        Bi("Secrets surface and the cost of silence becomes clear.", "تطفو الأسرار ويتضح ثمن الصمت."),
        Bi("Everything converges in a tense, sleepless finale.", "كل شيء يلتقي في ختام متوتر بلا نوم."),
    )

    private fun genericSeason(
        number: Int,
        year: Int,
        count: Int,
        runtime: Int,
        watchedThrough: Int = 0,
        progressOn: Int? = null,
        progress: Float = 0f,
        newFrom: Int? = null,
    ) = Season(
        number, year,
        (1..count).map { e ->
            Episode(
                season = number,
                number = e,
                title = Bi("Chapter $e", "الفصل $e"),
                synopsis = genericEpisodeLines[(e + number) % genericEpisodeLines.size],
                runtimeMin = runtime + ((e * 7 + number * 3) % 11) - 5,
                progress = if (e == progressOn) progress else null,
                watched = e <= watchedThrough,
                isNew = newFrom != null && e >= newFrom,
            )
        },
    )

    // --- Movies -----------------------------------------------------------------------------------

    val dune2 = ProtoTitle(
        id = "dune2", imdb = "tt15239678", type = MediaType.MOVIE,
        title = Bi("Dune: Part Two", "كثيب: الجزء الثاني"),
        year = 2024, runtimeMin = 166, genres = listOf(G.SciFi, G.Adventure), rating = 8.5, cert = "PG-13",
        tagline = Bi("Long live the fighters.", "عاش المقاتلون."),
        synopsis = Bi(
            "Paul Atreides walks with Chani and the Fremen on a path of revenge against those who destroyed his family — while a vision of the future pulls him toward the holy war he swore to prevent.",
            "يسير بول أتريديس مع تشاني والفريمن في طريق الثأر ممن دمّروا عائلته، بينما تجرّه رؤيا المستقبل نحو الحرب المقدسة التي أقسم أن يمنعها.",
        ),
        director = Bi("Denis Villeneuve", "دوني فيلنوف"),
        cast = listOf(
            cast("Timothée Chalamet", "تيموثي شالاميه", "Paul Atreides", "بول أتريديس"),
            cast("Zendaya", "زندايا", "Chani", "تشاني"),
            cast("Rebecca Ferguson", "ريبيكا فيرغسون", "Lady Jessica", "السيدة جيسيكا"),
            cast("Javier Bardem", "خافيير بارديم", "Stilgar", "ستيلغار"),
            cast("Austin Butler", "أوستن بتلر", "Feyd-Rautha", "فيد-راوثا"),
            cast("Florence Pugh", "فلورنس بيو", "Princess Irulan", "الأميرة إيرولان"),
        ),
        tech = Tech(uhd = true, dolbyVision = true, hdr10 = true, atmos = true, imax = true),
        palette = p(0xFF5E2F17, 0xFFE8A05A, 0xFFB5602C, 0xFF24120A, 0xFFF7C882, 0xFFE9A560),
        motif = ArtMotif.DUNES, seed = 11,
        country = C.USA,
        progress = 0.42f, remainingMin = 96,
        award = Bi("Winner — 2 Academy Awards", "حائز على جائزتي أوسكار"),
        quote = Bi("A towering work of cinema that feels carved out of the desert itself.", "عمل سينمائي شاهق كأنه منحوت من الصحراء ذاتها."),
        quoteSource = "Nuvio Review",
        chapters = listOf(
            Chapter(Bi("Arrakis", "أراكيس"), 0f),
            Chapter(Bi("The Fremen", "الفريمن"), 0.12f),
            Chapter(Bi("Sietch Tabr", "سيتش تابر"), 0.26f),
            Chapter(Bi("The Water of Life", "ماء الحياة"), 0.45f),
            Chapter(Bi("Giedi Prime", "جيدي برايم"), 0.58f),
            Chapter(Bi("The South", "الجنوب"), 0.72f),
            Chapter(Bi("Arrakeen", "أراكين"), 0.86f),
        ),
    )

    val oppenheimer = ProtoTitle(
        id = "oppenheimer", imdb = "tt15398776", type = MediaType.MOVIE,
        title = Bi("Oppenheimer", "أوبنهايمر"),
        year = 2023, runtimeMin = 180, genres = listOf(G.Drama, G.History), rating = 8.3, cert = "R",
        tagline = Bi("The world forever changes.", "العالم يتغير إلى الأبد."),
        synopsis = Bi(
            "The story of the physicist who led the race to build the atomic bomb, told through the triumph of creation and the long, quiet reckoning that followed.",
            "قصة الفيزيائي الذي قاد السباق لصنع القنبلة الذرية، بين نشوة الإنجاز والحساب الطويل الصامت الذي تلاه.",
        ),
        director = Bi("Christopher Nolan", "كريستوفر نولان"),
        cast = listOf(
            cast("Cillian Murphy", "كيليان مورفي", "J. Robert Oppenheimer", "ج. روبرت أوبنهايمر"),
            cast("Emily Blunt", "إميلي بلنت", "Kitty Oppenheimer", "كيتي أوبنهايمر"),
            cast("Matt Damon", "مات ديمون", "Leslie Groves", "ليزلي غروفز"),
            cast("Robert Downey Jr.", "روبرت داوني جونيور", "Lewis Strauss", "لويس ستراوس"),
            cast("Florence Pugh", "فلورنس بيو", "Jean Tatlock", "جين تاتلوك"),
        ),
        tech = Tech(uhd = true, dolbyVision = false, hdr10 = true, atmos = true, imax = true),
        palette = p(0xFF0B0706, 0xFF4A1A0A, 0xFFC4471B, 0xFF050303, 0xFFFFB763, 0xFFEB7D3C),
        motif = ArtMotif.FIRE, seed = 23, country = C.USA,
        award = Bi("Winner — 7 Academy Awards incl. Best Picture", "حائز على 7 جوائز أوسكار منها أفضل فيلم"),
        quote = Bi("Nolan turns history into a thunderclap.", "نولان يحوّل التاريخ إلى دويّ رعد."),
        quoteSource = "Nuvio Review",
        chapters = genericChapters(),
    )

    val br2049 = ProtoTitle(
        id = "br2049", imdb = "tt1856101", type = MediaType.MOVIE,
        title = Bi("Blade Runner 2049", "بليد رنر 2049"),
        year = 2017, runtimeMin = 164, genres = listOf(G.SciFi, G.Mystery), rating = 8.0, cert = "R",
        tagline = Bi("The key to the future is finally unearthed.", "مفتاح المستقبل يُكتشف أخيراً."),
        synopsis = Bi(
            "A new blade runner uncovers a buried secret with the power to plunge what is left of society into chaos, and a trail that leads to a man missing for thirty years.",
            "يكشف عميل جديد سرّاً دفيناً قادراً على إغراق ما تبقى من المجتمع في الفوضى، وأثراً يقوده إلى رجل مفقود منذ ثلاثين عاماً.",
        ),
        director = Bi("Denis Villeneuve", "دوني فيلنوف"),
        cast = listOf(
            cast("Ryan Gosling", "رايان غوسلينغ", "K", "كيه"),
            cast("Harrison Ford", "هاريسون فورد", "Rick Deckard", "ريك ديكارد"),
            cast("Ana de Armas", "آنا دي أرماس", "Joi", "جوي"),
            cast("Sylvia Hoeks", "سيلفيا هوكس", "Luv", "لوف"),
        ),
        tech = Tech(uhd = true, dolbyVision = true, hdr10 = true, atmos = true),
        palette = p(0xFF07131A, 0xFF12404A, 0xFF1B6B75, 0xFF04080A, 0xFFFF5AA8, 0xFF56CFD6),
        motif = ArtMotif.NEON_RAIN, seed = 37, country = C.USA,
        chapters = genericChapters(),
    )

    val interstellar = ProtoTitle(
        id = "interstellar", imdb = "tt0816692", type = MediaType.MOVIE,
        title = Bi("Interstellar", "بين النجوم"),
        year = 2014, runtimeMin = 169, genres = listOf(G.SciFi, G.Drama), rating = 8.7, cert = "PG-13",
        tagline = Bi("Mankind was born on Earth. It was never meant to die here.", "وُلدت البشرية على الأرض، لكنها لم تُخلق لتموت فيها."),
        synopsis = Bi(
            "With Earth failing, a former pilot leaves his children behind to cross a wormhole in search of a new home — racing time itself to keep a promise.",
            "مع احتضار الأرض، يترك طيار سابق أطفاله ليعبر ثقباً دودياً بحثاً عن موطن جديد، في سباق مع الزمن نفسه ليفي بوعده.",
        ),
        director = Bi("Christopher Nolan", "كريستوفر نولان"),
        cast = listOf(
            cast("Matthew McConaughey", "ماثيو ماكونهي", "Cooper", "كوبر"),
            cast("Anne Hathaway", "آن هاثاواي", "Brand", "براند"),
            cast("Jessica Chastain", "جيسيكا تشاستين", "Murph", "مورف"),
            cast("Michael Caine", "مايكل كين", "Professor Brand", "البروفيسور براند"),
        ),
        tech = Tech(uhd = true, dolbyVision = false, hdr10 = true, atmos = true, imax = true),
        palette = p(0xFF020306, 0xFF0A1628, 0xFF1E3B5E, 0xFF000000, 0xFFF5D7A1, 0xFFC9D7EA),
        motif = ArtMotif.SPACE, seed = 41, country = C.USA,
        chapters = genericChapters(),
    )

    val batman = ProtoTitle(
        id = "batman", imdb = "tt1877830", type = MediaType.MOVIE,
        title = Bi("The Batman", "باتمان"),
        year = 2022, runtimeMin = 176, genres = listOf(G.Crime, G.Mystery), rating = 7.8, cert = "PG-13",
        tagline = Bi("Unmask the truth.", "اكشف الحقيقة."),
        synopsis = Bi(
            "In his second year of fighting crime, Batman follows a serial killer's riddles into the corruption beneath Gotham — and into his own family's past.",
            "في عامه الثاني في مكافحة الجريمة، يتتبع باتمان ألغاز قاتل متسلسل إلى الفساد المختبئ في غوثام، وإلى ماضي عائلته.",
        ),
        director = Bi("Matt Reeves", "مات ريفز"),
        cast = listOf(
            cast("Robert Pattinson", "روبرت باتينسون", "Bruce Wayne", "بروس واين"),
            cast("Zoë Kravitz", "زوي كرافيتز", "Selina Kyle", "سيلينا كايل"),
            cast("Paul Dano", "بول دانو", "The Riddler", "ريدلر"),
            cast("Colin Farrell", "كولن فاريل", "The Penguin", "البطريق"),
        ),
        tech = Tech(uhd = true, dolbyVision = true, hdr10 = true, atmos = true),
        palette = p(0xFF0A0303, 0xFF3D0806, 0xFF8E1A12, 0xFF050202, 0xFFFF3B2F, 0xFFE0463A),
        motif = ArtMotif.NEON_RAIN, seed = 53, country = C.USA,
        progress = 0.71f, remainingMin = 51,
        chapters = genericChapters(),
    )

    val arrival = ProtoTitle(
        id = "arrival", imdb = "tt2543164", type = MediaType.MOVIE,
        title = Bi("Arrival", "الوصول"),
        year = 2016, runtimeMin = 116, genres = listOf(G.SciFi, G.Drama), rating = 7.9, cert = "PG-13",
        tagline = Bi("Why are they here?", "لماذا هم هنا؟"),
        synopsis = Bi(
            "When twelve vessels touch down around the world, a linguist is asked to find a way to talk to the visitors — and discovers a language that rewrites how she sees time.",
            "حين تهبط اثنتا عشرة مركبة حول العالم، تُكلَّف عالمة لغويات بإيجاد طريقة للتواصل مع الزوار، فتكتشف لغة تعيد تشكيل رؤيتها للزمن.",
        ),
        director = Bi("Denis Villeneuve", "دوني فيلنوف"),
        cast = listOf(
            cast("Amy Adams", "إيمي آدامز", "Louise Banks", "لويز بانكس"),
            cast("Jeremy Renner", "جيريمي رينر", "Ian Donnelly", "إيان دونيلي"),
            cast("Forest Whitaker", "فورست ويتكر", "Colonel Weber", "العقيد ويبر"),
        ),
        tech = Tech(uhd = true, dolbyVision = true, hdr10 = true, atmos = true),
        palette = p(0xFF6F7A7C, 0xFFA9B2AF, 0xFF47524F, 0xFF1C2322, 0xFFE3E8E3, 0xFFAEBCB6),
        motif = ArtMotif.MOUNTAIN, seed = 67, country = C.USA,
        chapters = genericChapters(),
    )

    val madmax = ProtoTitle(
        id = "madmax", imdb = "tt1392190", type = MediaType.MOVIE,
        title = Bi("Mad Max: Fury Road", "ماد ماكس: طريق الغضب"),
        year = 2015, runtimeMin = 120, genres = listOf(G.Action, G.Adventure), rating = 8.1, cert = "R",
        tagline = Bi("What a lovely day.", "يا له من يوم جميل."),
        synopsis = Bi(
            "In a scorched wasteland, a drifter and a rebel commander flee a tyrant across the desert in a war rig carrying his most precious cargo.",
            "في أرض قاحلة محترقة، يفرّ رحّالة وقائدة متمردة من طاغية عبر الصحراء في شاحنة حربية تحمل أثمن ما يملك.",
        ),
        director = Bi("George Miller", "جورج ميلر"),
        cast = listOf(
            cast("Tom Hardy", "توم هاردي", "Max", "ماكس"),
            cast("Charlize Theron", "تشارليز ثيرون", "Furiosa", "فيوريوسا"),
            cast("Nicholas Hoult", "نيكولاس هولت", "Nux", "نكس"),
        ),
        tech = Tech(uhd = true, dolbyVision = true, hdr10 = true, atmos = true),
        palette = p(0xFF0E4A55, 0xFFE0823C, 0xFFC4622A, 0xFF3B1A0C, 0xFF7FD3D6, 0xFFE88A48),
        motif = ArtMotif.DUNES, seed = 71, country = C.USA,
        chapters = genericChapters(),
    )

    val topgun = ProtoTitle(
        id = "topgun", imdb = "tt1745960", type = MediaType.MOVIE,
        title = Bi("Top Gun: Maverick", "توب غن: مافريك"),
        year = 2022, runtimeMin = 131, genres = listOf(G.Action, G.Drama), rating = 8.2, cert = "PG-13",
        tagline = Bi("Feel the need.", "اشعر بالحاجة."),
        synopsis = Bi(
            "Thirty years on, Maverick is called back to train a squad of elite graduates for an impossible mission — including the son of the friend he lost.",
            "بعد ثلاثين عاماً، يُستدعى مافريك لتدريب نخبة من الخريجين على مهمة مستحيلة، بينهم ابن الصديق الذي فقده.",
        ),
        director = Bi("Joseph Kosinski", "جوزيف كوسينسكي"),
        cast = listOf(
            cast("Tom Cruise", "توم كروز", "Maverick", "مافريك"),
            cast("Miles Teller", "مايلز تيلر", "Rooster", "روستر"),
            cast("Jennifer Connelly", "جينيفر كونيلي", "Penny", "بيني"),
        ),
        tech = Tech(uhd = true, dolbyVision = true, hdr10 = true, atmos = true, imax = true),
        palette = p(0xFF1C2E4A, 0xFFF2A65A, 0xFF7C5A52, 0xFF1A1A22, 0xFFFFD28A, 0xFFF2B066),
        motif = ArtMotif.SKY, seed = 83, country = C.USA,
        chapters = genericChapters(),
    )

    val pastlives = ProtoTitle(
        id = "pastlives", imdb = "tt13238346", type = MediaType.MOVIE,
        title = Bi("Past Lives", "حيوات سابقة"),
        year = 2023, runtimeMin = 105, genres = listOf(G.Romance, G.Drama), rating = 7.8, cert = "PG-13",
        tagline = Bi("What if?", "ماذا لو؟"),
        synopsis = Bi(
            "Two childhood friends, separated when one family emigrates from Seoul, meet again in New York two decades later for one week that asks what might have been.",
            "صديقا طفولة افترقا حين هاجرت إحدى العائلتين من سيول، يلتقيان في نيويورك بعد عقدين في أسبوع واحد يسأل عمّا كان يمكن أن يكون.",
        ),
        director = Bi("Celine Song", "سيلين سونغ"),
        cast = listOf(
            cast("Greta Lee", "غريتا لي", "Nora", "نورا"),
            cast("Teo Yoo", "تيو يو", "Hae Sung", "هاي سونغ"),
            cast("John Magaro", "جون ماغارو", "Arthur", "آرثر"),
        ),
        tech = Tech(uhd = true, dolbyVision = false, hdr10 = true, atmos = false),
        palette = p(0xFF1A2033, 0xFFC98A8F, 0xFF4E4C6B, 0xFF0C0E16, 0xFFFFD6C2, 0xFFE3A6A9),
        motif = ArtMotif.CITY_NIGHT, seed = 97, country = C.USA,
        chapters = genericChapters(),
    )

    val parasite = ProtoTitle(
        id = "parasite", imdb = "tt6751668", type = MediaType.MOVIE,
        title = Bi("Parasite", "طفيلي"),
        year = 2019, runtimeMin = 132, genres = listOf(G.Thriller, G.Drama), rating = 8.5, cert = "R",
        tagline = Bi("Act like you own the place.", "تصرّف كأن المكان ملكك."),
        synopsis = Bi(
            "A struggling family schemes its way, one job at a time, into the household of a wealthy one — until an unexpected discovery upends both.",
            "عائلة معدمة تتسلل بالحيلة، وظيفةً بعد أخرى، إلى منزل عائلة ثرية، حتى يقلب اكتشاف غير متوقع حياة العائلتين.",
        ),
        director = Bi("Bong Joon Ho", "بونغ جون-هو"),
        cast = listOf(
            cast("Song Kang-ho", "سونغ كانغ-هو", "Kim Ki-taek", "كيم كي-تيك"),
            cast("Cho Yeo-jeong", "تشو يو-جونغ", "Park Yeon-kyo", "بارك يون-كيو"),
            cast("Choi Woo-shik", "تشوي وو-شيك", "Kim Ki-woo", "كيم كي-وو"),
        ),
        tech = Tech(uhd = true, dolbyVision = true, hdr10 = true, atmos = false),
        palette = p(0xFF1A2A1E, 0xFF9DBF7A, 0xFF3E5B3A, 0xFF0D120D, 0xFFE8EFD2, 0xFFA9C88A),
        motif = ArtMotif.INTERIOR, seed = 101, country = C.Korea,
        award = Bi("Winner — Best Picture", "حائز على جائزة أفضل فيلم"),
        chapters = genericChapters(),
    )

    val lawrence = ProtoTitle(
        id = "lawrence", imdb = "tt0056172", type = MediaType.MOVIE,
        title = Bi("Lawrence of Arabia", "لورنس العرب"),
        year = 1962, runtimeMin = 228, genres = listOf(G.Adventure, G.History), rating = 8.3, cert = "PG",
        tagline = Bi("Restored in 4K from the original 65mm negative.", "مرمّم بدقة 4K من النيجاتيف الأصلي 65 ملم."),
        synopsis = Bi(
            "A British officer's journey through the Arabian desert during the First World War, and the alliances with Arab tribes that turned him into a legend.",
            "رحلة ضابط بريطاني عبر صحراء الجزيرة العربية خلال الحرب العالمية الأولى، والتحالفات مع القبائل العربية التي صنعت منه أسطورة.",
        ),
        director = Bi("David Lean", "ديفيد لين"),
        cast = listOf(
            cast("Peter O'Toole", "بيتر أوتول", "T. E. Lawrence", "ت. إ. لورنس"),
            cast("Omar Sharif", "عمر الشريف", "Sherif Ali", "الشريف علي"),
            cast("Anthony Quinn", "أنتوني كوين", "Auda abu Tayi", "عودة أبو تايه"),
            cast("Alec Guinness", "أليك غينيس", "Prince Faisal", "الأمير فيصل"),
        ),
        tech = Tech(uhd = true, dolbyVision = true, hdr10 = true, atmos = false),
        palette = p(0xFF3C6E9E, 0xFFF2C58A, 0xFFD7A15E, 0xFF6B4222, 0xFFFFF1D0, 0xFFE9B872),
        motif = ArtMotif.DUNES, seed = 113, country = C.UK,
        chapters = genericChapters(),
    )

    // --- Arabic cinema ------------------------------------------------------------------------------

    val wadjda = ProtoTitle(
        id = "wadjda", imdb = "tt2258858", type = MediaType.MOVIE,
        title = Bi("Wadjda", "وجدة"),
        year = 2012, runtimeMin = 98, genres = listOf(G.Drama, G.Family), rating = 7.6, cert = "PG",
        tagline = Bi("Dream big.", "احلمي كثيراً."),
        synopsis = Bi(
            "In a Riyadh neighbourhood, a spirited ten-year-old sets her heart on a green bicycle — and enters a Quran recitation competition to win the money to buy it.",
            "في أحد أحياء الرياض، تتعلق طفلة في العاشرة بدرّاجة خضراء، وتشارك في مسابقة لتلاوة القرآن لتفوز بثمنها.",
        ),
        director = Bi("Haifaa Al-Mansour", "هيفاء المنصور"),
        cast = listOf(
            cast("Waad Mohammed", "وعد محمد", "Wadjda", "وجدة"),
            cast("Reem Abdullah", "ريم عبدالله", "Mother", "الأم"),
            cast("Abdullrahman Al Gohani", "عبدالرحمن الجهني", "Abdullah", "عبدالله"),
        ),
        tech = Tech(uhd = false, dolbyVision = false, hdr10 = false, atmos = false),
        palette = p(0xFFD9B98C, 0xFFF0DDBE, 0xFFA77E55, 0xFF4A3322, 0xFF2F8F5B, 0xFF4FB07A),
        motif = ArtMotif.ARCHES, seed = 127, country = C.Saudi,
        progress = 0.18f, remainingMin = 80,
        award = Bi("The first feature film shot entirely in Saudi Arabia", "أول فيلم روائي طويل يُصوَّر بالكامل في السعودية"),
        quote = Bi("Small in scale, enormous in heart.", "صغير في حجمه، عظيم في روحه."),
        quoteSource = "Nuvio Review",
        chapters = genericChapters(),
    )

    val theeb = ProtoTitle(
        id = "theeb", imdb = "tt3170902", type = MediaType.MOVIE,
        title = Bi("Theeb", "ذيب"),
        year = 2014, runtimeMin = 100, genres = listOf(G.Adventure, G.Drama), rating = 7.2, cert = "PG-13",
        tagline = Bi("In the desert, the wolf learns fast.", "في الصحراء، يتعلّم الذيب سريعاً."),
        synopsis = Bi(
            "Hijaz, 1916. A young Bedouin boy follows his older brother on a perilous desert crossing to guide a British officer to a secret destination.",
            "الحجاز، 1916. فتى بدوي صغير يتبع أخاه الأكبر في رحلة صحراوية محفوفة بالمخاطر لإرشاد ضابط بريطاني إلى وجهة سرّية.",
        ),
        director = Bi("Naji Abu Nowar", "ناجي أبو نوار"),
        cast = listOf(
            cast("Jacir Eid Al-Hwietat", "جاسر عيد الحويطات", "Theeb", "ذيب"),
            cast("Hassan Mutlag Al-Maraiyeh", "حسن مطلق المراعية", "Hussein", "حسين"),
            cast("Jack Fox", "جاك فوكس", "Edward", "إدوارد"),
        ),
        tech = Tech(uhd = true, dolbyVision = false, hdr10 = true, atmos = false),
        palette = p(0xFFE3B27A, 0xFFF4D2A0, 0xFFA3482A, 0xFF3D160C, 0xFFFFE3B8, 0xFFD9784E),
        motif = ArtMotif.MOUNTAIN, seed = 131, country = C.Jordan,
        chapters = genericChapters(),
    )

    val capernaum = ProtoTitle(
        id = "capernaum", imdb = "tt8267604", type = MediaType.MOVIE,
        title = Bi("Capernaum", "كفرناحوم"),
        year = 2018, runtimeMin = 126, genres = listOf(G.Drama), rating = 8.4, cert = "R",
        tagline = Bi("A child sues his parents for giving him life.", "طفل يقاضي والديه لأنهما منحاه الحياة."),
        synopsis = Bi(
            "A streetwise twelve-year-old in Beirut takes his parents to court — and tells the story of the hard, luminous year that brought him there.",
            "طفل في الثانية عشرة من شوارع بيروت يقاضي والديه، ويروي حكاية العام القاسي والمضيء الذي قاده إلى المحكمة.",
        ),
        director = Bi("Nadine Labaki", "نادين لبكي"),
        cast = listOf(
            cast("Zain Al Rafeea", "زين الرفاعي", "Zain", "زين"),
            cast("Yordanos Shiferaw", "يوردانوس شيفيراو", "Rahil", "رحيل"),
            cast("Nadine Labaki", "نادين لبكي", "Nadine", "نادين"),
        ),
        tech = Tech(uhd = false, dolbyVision = false, hdr10 = false, atmos = false),
        palette = p(0xFF2C2F36, 0xFFB9A88F, 0xFF6D6154, 0xFF16130F, 0xFFF0C987, 0xFFCBB38C),
        motif = ArtMotif.INTERIOR, seed = 137, country = C.Lebanon,
        award = Bi("Jury Prize — Cannes", "جائزة لجنة التحكيم — كان"),
        chapters = genericChapters(),
    )

    val hajjan = ProtoTitle(
        id = "hajjan", imdb = null, type = MediaType.MOVIE,
        title = Bi("Hajjan", "هجّان"),
        year = 2023, runtimeMin = 118, genres = listOf(G.Drama, G.Adventure), rating = 7.1, cert = "PG-13",
        tagline = Bi("The race is the only way home.", "السباق هو الطريق الوحيد إلى البيت."),
        synopsis = Bi(
            "After losing his brother, a young camel jockey is drawn into the ruthless world of desert racing — with only the bond with his camel to guide him.",
            "بعد فقدان أخيه، ينجرّ هجّان شاب إلى عالم سباقات الهجن القاسي، ولا يرشده سوى رابطه مع ناقته.",
        ),
        director = Bi("Abu Bakr Shawky", "أبو بكر شوقي"),
        cast = listOf(
            cast("Omar Alatawi", "عمر العطاوي", "Matar", "مطر"),
            cast("Ibrahim Al-Hasawi", "إبراهيم الحساوي", "Jasser", "جاسر"),
        ),
        tech = Tech(uhd = true, dolbyVision = false, hdr10 = true, atmos = true),
        palette = p(0xFF0A0E1F, 0xFF3A3355, 0xFF7A5A48, 0xFF1B130E, 0xFFF2D39B, 0xFFD6B57E),
        motif = ArtMotif.DESERT_NIGHT, seed = 139, country = C.Saudi,
        chapters = genericChapters(),
    )

    val perfectCandidate = ProtoTitle(
        id = "candidate", imdb = null, type = MediaType.MOVIE,
        title = Bi("The Perfect Candidate", "المرشحة المثالية"),
        year = 2019, runtimeMin = 104, genres = listOf(G.Drama, G.Comedy), rating = 6.9, cert = "PG",
        tagline = Bi("She only wanted the road fixed.", "كل ما أرادته هو إصلاح الطريق."),
        synopsis = Bi(
            "A young doctor in a small Saudi town unexpectedly runs for the municipal council — and discovers how much a single campaign can change.",
            "طبيبة شابة في بلدة سعودية صغيرة تترشح على غير توقع لعضوية المجلس البلدي، وتكتشف كم يمكن لحملة واحدة أن تغيّر.",
        ),
        director = Bi("Haifaa Al-Mansour", "هيفاء المنصور"),
        cast = listOf(
            cast("Mila Al Zahrani", "ميلا الزهراني", "Maryam", "مريم"),
            cast("Dae Al Hilali", "ضي الهلالي", "Selma", "سلمى"),
            cast("Khalid Abdulraheem", "خالد عبدالرحيم", "Abdulaziz", "عبدالعزيز"),
        ),
        tech = Tech(uhd = false, dolbyVision = false, hdr10 = false, atmos = false),
        palette = p(0xFF103A3C, 0xFF7FB5B0, 0xFF2F6663, 0xFF0B1818, 0xFFF4E3C3, 0xFF7EC3B8),
        motif = ArtMotif.CORRIDOR, seed = 149, country = C.Saudi,
        chapters = genericChapters(),
    )

    // --- Series -------------------------------------------------------------------------------------

    private val severanceS1 = Season(
        1, 2022,
        listOf(
            Bi("Good News About Hell", "أخبار سارة عن الجحيم") to Bi("Mark is promoted after a colleague vanishes, and a new hire wakes on a conference table.", "يُرقّى مارك بعد اختفاء زميل، وتستيقظ موظفة جديدة على طاولة اجتماعات."),
            Bi("Half Loop", "نصف دورة") to Bi("Helly tests the limits of her new job; Mark meets a stranger who knew his old friend.", "تختبر هيلي حدود وظيفتها الجديدة، ويلتقي مارك غريباً يعرف صديقه القديم."),
            Bi("In Perpetuity", "إلى الأبد") to Bi("A visit to the Perpetuity Wing stirs questions about the company's founder.", "زيارة إلى جناح الأبدية تثير أسئلة عن مؤسس الشركة."),
            Bi("The You You Are", "أنت الذي أنت") to Bi("Irving follows a mysterious painting; Mark reads a book he shouldn't have.", "يتتبع إيرفينغ لوحة غامضة، ويقرأ مارك كتاباً ما كان يجب أن يقرأه."),
            Bi("The Grim Barbarity of Optics and Design", "همجية البصريات والتصميم") to Bi("The team discovers another department exists.", "يكتشف الفريق وجود قسم آخر."),
            Bi("Hide and Seek", "الغميضة") to Bi("A hunt for a hidden map ends with a confession.", "بحث عن خريطة مخبأة ينتهي باعتراف."),
            Bi("Defiant Jazz", "جاز متمرد") to Bi("A celebration in the break room goes badly wrong.", "احتفال في غرفة الاستراحة ينقلب رأساً على عقب."),
            Bi("What's for Dinner?", "ماذا على العشاء؟") to Bi("A party upstairs and a plan downstairs converge.", "حفلة في الأعلى وخطة في الأسفل تلتقيان."),
            Bi("The We We Are", "نحن الذين نحن") to Bi("The innies wake up somewhere they have never been.", "يستيقظ الموظفون في مكان لم يعرفوه من قبل."),
        ).mapIndexed { i, (t, s) -> Episode(1, i + 1, t, s, listOf(57, 53, 49, 52, 46, 44, 52, 50, 40)[i], watched = true) },
    )

    private val severanceS2 = Season(
        2, 2025,
        listOf(
            Bi("Hello, Ms. Cobel", "مرحباً يا آنسة كوبل") to Bi("Mark returns to a severed floor that has quietly changed.", "يعود مارك إلى طابق تغيّر بهدوء."),
            Bi("Goodbye, Mrs. Selvig", "وداعاً يا سيدة سيلفيغ") to Bi("Outside, the fallout of the overtime contingency spreads.", "في الخارج، تتسع تبعات خطة العمل الإضافي."),
            Bi("Who Is Alive?", "من الحيّ؟") to Bi("A new manager arrives with a very specific agenda.", "مدير جديد يصل بأجندة محددة جداً."),
            Bi("Woe's Hollow", "وادي الويل") to Bi("A team-building retreat in the snow reveals more than it should.", "رحلة لبناء الفريق وسط الثلوج تكشف أكثر مما يجب."),
            Bi("Trojan's Horse", "حصان طروادة") to Bi("Loyalties split as the refiners return from the wilderness.", "تنقسم الولاءات مع عودة المنقّحين من البرية."),
            Bi("Attila", "أتيلا") to Bi("An unlikely dinner date; a long-awaited meeting.", "موعد عشاء غير متوقع، ولقاء طال انتظاره."),
            Bi("Chikhai Bardo", "تشيكاي باردو") to Bi("A different perspective on what happened to Gemma.", "منظور مختلف لما حدث لجيما."),
            Bi("Sweet Vitriol", "لاذع حلو") to Bi("Cobel goes home to where it all began.", "تعود كوبل إلى حيث بدأ كل شيء."),
            Bi("The After Hours", "ما بعد الدوام") to Bi("After-hours plans are set in motion.", "خطط ما بعد الدوام تبدأ بالتحرك."),
            Bi("Cold Harbor", "المرفأ البارد") to Bi("The file everyone has been waiting for.", "الملف الذي ينتظره الجميع."),
        ).mapIndexed { i, (t, s) ->
            Episode(
                2, i + 1, t, s, listOf(57, 49, 51, 53, 47, 51, 38, 49, 50, 76)[i],
                watched = i < 3,
                progress = if (i == 3) 0.34f else null,
                isNew = i >= 8,
            )
        },
    )

    val severance = ProtoTitle(
        id = "severance", imdb = "tt11280740", type = MediaType.SERIES,
        title = Bi("Severance", "انفصال"),
        year = 2022, runtimeMin = 52, genres = listOf(G.Thriller, G.SciFi), rating = 8.7, cert = "TV-MA",
        tagline = Bi("Work is a mystery.", "العمل لغز."),
        synopsis = Bi(
            "At Lumon Industries, employees undergo a procedure that surgically divides their work and personal memories — until one team begins to uncover what the work really is.",
            "في شركة لومون، يخضع الموظفون لإجراء يفصل ذكريات العمل عن حياتهم الشخصية، إلى أن يبدأ فريق واحد باكتشاف حقيقة ما يعملون عليه.",
        ),
        director = Bi("Created by Dan Erickson", "من تأليف دان إريكسون"),
        cast = listOf(
            cast("Adam Scott", "آدم سكوت", "Mark Scout", "مارك سكاوت"),
            cast("Britt Lower", "بريت لاور", "Helly R.", "هيلي آر."),
            cast("Zach Cherry", "زاك تشيري", "Dylan G.", "ديلان جي."),
            cast("John Turturro", "جون تورتورو", "Irving B.", "إيرفينغ بي."),
            cast("Patricia Arquette", "باتريشيا أركيت", "Harmony Cobel", "هارموني كوبل"),
        ),
        tech = Tech(uhd = true, dolbyVision = true, hdr10 = true, atmos = true),
        palette = p(0xFFE9EEEC, 0xFFCBD6D2, 0xFF7FA69A, 0xFF1F4D3F, 0xFF2F6B5A, 0xFF74BCA6),
        motif = ArtMotif.CORRIDOR, seed = 151, country = C.USA,
        progress = 0.34f, remainingMin = 35,
        seasons = listOf(severanceS1, severanceS2), resumeSeason = 2, resumeEpisode = 4,
        quote = Bi("The most unsettling office on television.", "أكثر مكتب مُقلق على الشاشة."),
        quoteSource = "Nuvio Review",
        chapters = genericChapters(),
    )

    private val shogunS1 = Season(
        1, 2024,
        listOf(
            Bi("Anjin", "أنجين") to Bi("A foreign ship washes ashore as the regents close in on Lord Toranaga.", "سفينة أجنبية تجنح إلى الشاطئ بينما يضيّق الأوصياء الخناق على اللورد توراناغا."),
            Bi("Servants of Two Masters", "خادم لسيدين") to Bi("Blackthorne is brought to Osaka; Mariko is asked to translate.", "يُساق بلاكثورن إلى أوساكا، وتُكلَّف ماريكو بالترجمة."),
            Bi("Tomorrow Is Tomorrow", "الغد هو الغد") to Bi("An escape from Osaka Castle, under the cover of a procession.", "هروب من قلعة أوساكا تحت غطاء موكب."),
            Bi("The Eightfold Fence", "السياج الثماني") to Bi("In Ajiro, Blackthorne tries to live by rules he barely understands.", "في أجيرو، يحاول بلاكثورن العيش بقواعد بالكاد يفهمها."),
            Bi("Broken to the Fist", "مكسور حتى القبضة") to Bi("An unexpected guest brings Toranaga a message.", "ضيف غير متوقع يحمل رسالة إلى توراناغا."),
            Bi("Ladies of the Willow World", "سيدات عالم الصفصاف") to Bi("A game of honour in the willow world.", "لعبة شرف في عالم الصفصاف."),
            Bi("A Stick of Time", "عود من الزمن") to Bi("A memory of Toranaga's youth; a family reckoning.", "ذكرى من شباب توراناغا، وحساب عائلي."),
            Bi("The Abyss of Life", "هاوية الحياة") to Bi("Loyalty is weighed against life itself.", "يُوزن الولاء أمام الحياة نفسها."),
            Bi("Crimson Sky", "سماء قرمزية") to Bi("Mariko makes her stand in Osaka.", "ماريكو تتخذ موقفها في أوساكا."),
            Bi("A Dream of a Dream", "حلم داخل حلم") to Bi("The shape of Toranaga's plan finally becomes clear.", "تتضح أخيراً ملامح خطة توراناغا."),
        ).mapIndexed { i, (t, s) -> Episode(1, i + 1, t, s, listOf(70, 58, 59, 55, 57, 56, 55, 58, 58, 68)[i], watched = i < 2) },
    )

    val shogun = ProtoTitle(
        id = "shogun", imdb = "tt2788316", type = MediaType.SERIES,
        title = Bi("Shōgun", "شوغن"),
        year = 2024, runtimeMin = 59, genres = listOf(G.Drama, G.History), rating = 8.6, cert = "TV-MA",
        tagline = Bi("A world of honour. A war for power.", "عالم من الشرف، وحرب على السلطة."),
        synopsis = Bi(
            "Japan, 1600. As civil war looms, Lord Toranaga fights for his life against the Council of Regents — and a shipwrecked English navigator becomes his most unexpected weapon.",
            "اليابان، 1600. مع اقتراب الحرب الأهلية، يقاتل اللورد توراناغا من أجل حياته أمام مجلس الأوصياء، ويصبح ملّاح إنجليزي غريق سلاحه الأكثر مفاجأة.",
        ),
        director = Bi("Created by Rachel Kondo & Justin Marks", "من ابتكار راشيل كوندو وجاستن ماركس"),
        cast = listOf(
            cast("Hiroyuki Sanada", "هيرويوكي سانادا", "Yoshii Toranaga", "يوشي توراناغا"),
            cast("Cosmo Jarvis", "كوزمو جارفيس", "John Blackthorne", "جون بلاكثورن"),
            cast("Anna Sawai", "آنا ساواي", "Toda Mariko", "تودا ماريكو"),
            cast("Tadanobu Asano", "تادانوبو أسانو", "Kashigi Yabushige", "كاشيغي يابوشيغي"),
        ),
        tech = Tech(uhd = true, dolbyVision = true, hdr10 = true, atmos = true),
        palette = p(0xFF1B1A1C, 0xFF6E2E26, 0xFF3A2A26, 0xFF0A0909, 0xFFD24A36, 0xFFD8604A),
        motif = ArtMotif.STORM, seed = 163, country = C.USA,
        seasons = listOf(shogunS1),
        award = Bi("Winner — 18 Emmy Awards", "حائز على 18 جائزة إيمي"),
        quote = Bi("Epic television, told with the patience of a master.", "دراما ملحمية تُروى بصبر المعلّم."),
        quoteSource = "Nuvio Review",
        chapters = genericChapters(),
    )

    val thebear = ProtoTitle(
        id = "thebear", imdb = "tt14452776", type = MediaType.SERIES,
        title = Bi("The Bear", "الدب"),
        year = 2022, runtimeMin = 32, genres = listOf(G.Comedy, G.Drama), rating = 8.5, cert = "TV-MA",
        tagline = Bi("Yes, chef.", "حاضر يا شيف."),
        synopsis = Bi(
            "A young fine-dining chef comes home to Chicago to run his late brother's chaotic sandwich shop — and to hold together the family that comes with it.",
            "طاهٍ شاب من عالم المطاعم الفاخرة يعود إلى شيكاغو ليدير محل السندويشات الفوضوي لأخيه الراحل، ويحافظ على العائلة التي تأتي معه.",
        ),
        director = Bi("Created by Christopher Storer", "من ابتكار كريستوفر ستورر"),
        cast = listOf(
            cast("Jeremy Allen White", "جيريمي ألين وايت", "Carmy", "كارمي"),
            cast("Ayo Edebiri", "أيو إديبيري", "Sydney", "سيدني"),
            cast("Ebon Moss-Bachrach", "إيبون موس-باكراك", "Richie", "ريتشي"),
        ),
        tech = Tech(uhd = true, dolbyVision = false, hdr10 = true, atmos = false),
        palette = p(0xFF2B2320, 0xFFD9A066, 0xFF6B4B3A, 0xFF130D0A, 0xFF8DB8DE, 0xFFE0A46A),
        motif = ArtMotif.INTERIOR, seed = 173, country = C.USA,
        progress = 0.6f, remainingMin = 12,
        seasons = listOf(
            genericSeason(1, 2022, 8, 30, watchedThrough = 8),
            genericSeason(2, 2023, 10, 34, watchedThrough = 5, progressOn = 6, progress = 0.6f),
        ),
        resumeSeason = 2, resumeEpisode = 6,
        chapters = genericChapters(),
    )

    val lastofus = ProtoTitle(
        id = "lastofus", imdb = "tt3581920", type = MediaType.SERIES,
        title = Bi("The Last of Us", "آخر من تبقّى منا"),
        year = 2023, runtimeMin = 55, genres = listOf(G.Drama, G.Adventure), rating = 8.7, cert = "TV-MA",
        tagline = Bi("When you're lost in the darkness, look for the light.", "حين تضيع في الظلام، ابحث عن النور."),
        synopsis = Bi(
            "Twenty years after a pandemic collapses civilisation, a hardened survivor is hired to smuggle a fourteen-year-old girl across a broken America.",
            "بعد عشرين عاماً من وباء أسقط الحضارة، يُكلَّف ناجٍ قاسٍ بتهريب فتاة في الرابعة عشرة عبر أمريكا المحطمة.",
        ),
        director = Bi("Created by Craig Mazin & Neil Druckmann", "من ابتكار كريغ مازن ونيل دركمان"),
        cast = listOf(
            cast("Pedro Pascal", "بيدرو باسكال", "Joel", "جول"),
            cast("Bella Ramsey", "بيلا رامزي", "Ellie", "إيلي"),
        ),
        tech = Tech(uhd = true, dolbyVision = true, hdr10 = true, atmos = true),
        palette = p(0xFF6F7F6A, 0xFFB8C29A, 0xFF3F4D36, 0xFF141A12, 0xFFE8D8A0, 0xFFA9B98A),
        motif = ArtMotif.FOREST, seed = 181, country = C.USA,
        seasons = listOf(
            genericSeason(1, 2023, 9, 55, watchedThrough = 9),
            genericSeason(2, 2025, 7, 55, watchedThrough = 6, newFrom = 7),
        ),
        chapters = genericChapters(),
    )

    val slowhorses = ProtoTitle(
        id = "slowhorses", imdb = "tt5875444", type = MediaType.SERIES,
        title = Bi("Slow Horses", "الخيول البطيئة"),
        year = 2022, runtimeMin = 45, genres = listOf(G.Thriller, G.Drama), rating = 8.2, cert = "TV-MA",
        tagline = Bi("Different kind of spy.", "جواسيس من نوع آخر."),
        synopsis = Bi(
            "A dysfunctional team of MI5 agents, exiled to a dead-end office for career-ending mistakes, keep stumbling into the operations that matter most.",
            "فريق مضطرب من عملاء الاستخبارات البريطانية، منفيّ إلى مكتب مهمَل بسبب أخطاء قاتلة، يتعثر دائماً في العمليات الأكثر أهمية.",
        ),
        director = Bi("Developed by Will Smith", "من تطوير ويل سميث"),
        cast = listOf(
            cast("Gary Oldman", "غاري أولدمان", "Jackson Lamb", "جاكسون لامب"),
            cast("Jack Lowden", "جاك لودن", "River Cartwright", "ريفر كارترايت"),
            cast("Kristin Scott Thomas", "كريستين سكوت توماس", "Diana Taverner", "ديانا تافيرنر"),
        ),
        tech = Tech(uhd = true, dolbyVision = true, hdr10 = true, atmos = true),
        palette = p(0xFF0D1319, 0xFF3E4B55, 0xFF26323B, 0xFF07090B, 0xFFE8C66A, 0xFFC9B26A),
        motif = ArtMotif.CITY_NIGHT, seed = 191, country = C.UK,
        seasons = listOf(genericSeason(4, 2024, 6, 45, watchedThrough = 5, newFrom = 6)),
        chapters = genericChapters(),
    )

    val hotd = ProtoTitle(
        id = "hotd", imdb = "tt11198330", type = MediaType.SERIES,
        title = Bi("House of the Dragon", "آل التنين"),
        year = 2022, runtimeMin = 62, genres = listOf(G.Fantasy, G.Drama), rating = 8.3, cert = "TV-MA",
        tagline = Bi("All must choose.", "على الجميع أن يختار."),
        synopsis = Bi(
            "Two centuries before the events of the old saga, a succession crisis splits House Targaryen — and sets dragon against dragon.",
            "قبل قرنين من أحداث الملحمة الأصلية، تشقّ أزمة وراثة عائلة تارغارين، وتضع التنين في مواجهة التنين.",
        ),
        director = Bi("Created by Ryan Condal & George R. R. Martin", "من ابتكار رايان كوندال وجورج ر. ر. مارتن"),
        cast = listOf(
            cast("Emma D'Arcy", "إيما دارسي", "Rhaenyra Targaryen", "راينيرا تارغارين"),
            cast("Matt Smith", "مات سميث", "Daemon Targaryen", "ديمون تارغارين"),
            cast("Olivia Cooke", "أوليفيا كوك", "Alicent Hightower", "أليسنت هايتاور"),
        ),
        tech = Tech(uhd = true, dolbyVision = true, hdr10 = true, atmos = true),
        palette = p(0xFF0D0710, 0xFF3B0F1E, 0xFF8A1C24, 0xFF040204, 0xFFFFB347, 0xFFE04B4B),
        motif = ArtMotif.FIRE, seed = 199, country = C.USA,
        seasons = listOf(genericSeason(2, 2024, 8, 62, watchedThrough = 3)),
        chapters = genericChapters(),
    )

    // --- Indexes ------------------------------------------------------------------------------------

    val all: List<ProtoTitle> = listOf(
        dune2, oppenheimer, br2049, interstellar, batman, arrival, madmax, topgun, pastlives, parasite, lawrence,
        wadjda, theeb, capernaum, hajjan, perfectCandidate,
        severance, shogun, thebear, lastofus, slowhorses, hotd,
    )

    private val byId = all.associateBy { it.id }
    fun title(id: String): ProtoTitle = byId[id] ?: dune2

    const val featuredMovieId = "dune2"
    const val featuredSeriesId = "severance"

    val hero = dune2
    val heroes = listOf(dune2, shogun, oppenheimer, wadjda, severance)

    val continueWatching = listOf(dune2, severance, batman, thebear, wadjda)
    val tonight = listOf(shogun, br2049, arrival, pastlives, theeb, oppenheimer, parasite)
    val newEpisodes = listOf(lastofus, slowhorses, severance, hotd, thebear)
    val becauseDune = listOf(br2049, arrival, interstellar, lawrence, madmax, theeb)
    val dolbyVisionAtmos = listOf(dune2, batman, br2049, topgun, severance, shogun, madmax)
    val arabicCinema = listOf(wadjda, theeb, capernaum, hajjan, perfectCandidate)
    val trending = listOf(dune2, shogun, oppenheimer, lastofus, topgun, parasite, madmax)
    val shortWatches = listOf(thebear, pastlives, wadjda, theeb, perfectCandidate)
    val hiddenGems = listOf(theeb, pastlives, capernaum, hajjan, arrival)
    val recentlyAdded = listOf(perfectCandidate, hajjan, slowhorses, parasite, interstellar)
    val watchlist = listOf(shogun, oppenheimer, lawrence, capernaum, interstellar, parasite, hotd, topgun)
    val watched = listOf(br2049, madmax, arrival, pastlives, theeb, lastofus)

    val collections = listOf(
        Collection("continue", Bi("Continue Watching", "تابع المشاهدة"), Bi("Pick up where you left off", "أكمل من حيث توقفت"), continueWatching),
        Collection("tonight", Bi("Recommended Tonight", "مقترح لهذه الليلة"), Bi("Chosen for a Thursday evening", "مختارة لمساء الخميس"), tonight),
        Collection("new", Bi("New Episodes", "حلقات جديدة"), Bi("From shows you follow", "من المسلسلات التي تتابعها"), newEpisodes),
        Collection("because", Bi("Because You Watched Dune", "لأنك شاهدت كثيب"), Bi("Vast, patient, visionary", "واسعة، هادئة، ذات رؤية"), becauseDune),
        Collection("dv", Bi("Dolby Vision + Atmos", "دولبي فيجن + أتموس"), Bi("Made for this screen", "صُنعت لهذه الشاشة"), dolbyVisionAtmos),
        Collection("arabic", Bi("Arabic Cinema", "السينما العربية"), Bi("New voices from the region", "أصوات جديدة من المنطقة"), arabicCinema),
        Collection("trending", Bi("Trending", "الأكثر رواجاً"), Bi("What everyone is watching", "ما يشاهده الجميع"), trending),
        Collection("short", Bi("Short Watches", "مشاهدات قصيرة"), Bi("Under two hours, or one episode", "أقل من ساعتين أو حلقة واحدة"), shortWatches),
        Collection("gems", Bi("Hidden Gems", "جواهر مخفية"), Bi("Loved by the few who found them", "أحبها القلة الذين وجدوها"), hiddenGems),
        Collection("recent", Bi("Recently Added", "أضيف حديثاً"), Bi("New to your library", "جديد في مكتبتك"), recentlyAdded),
    )

    fun collection(id: String): Collection = collections.first { it.id == id }

    // --- Profiles ------------------------------------------------------------------------------------

    val profiles = listOf(
        ProtoProfile("faisal", Bi("Faisal", "فيصل"), Color(0xFFC9A45C), "F"),
        ProtoProfile("noura", Bi("Noura", "نورة"), Color(0xFFC97B84), "N"),
        ProtoProfile("layan", Bi("Layan", "ليان"), Color(0xFF5BA3A0), "L"),
        ProtoProfile("kids", Bi("Kids", "الأطفال"), Color(0xFFE0B64A), "K", kids = true),
        ProtoProfile("guest", Bi("Guest", "ضيف"), Color(0xFF8A8F98), "G"),
    )
    val currentProfile = profiles.first()

    // --- Tracks --------------------------------------------------------------------------------------

    val subtitles = listOf(
        SubtitleTrack("ar", Bi("Arabic", "العربية"), "AR", null, "Embedded", "SRT", isDefault = true),
        SubtitleTrack("en", Bi("English", "الإنجليزية"), "EN", null, "Embedded", "PGS"),
        SubtitleTrack("en-sdh", Bi("English", "الإنجليزية"), "EN", Bi("SDH", "لضعاف السمع"), "Embedded", "PGS"),
        SubtitleTrack("ar-forced", Bi("Arabic", "العربية"), "AR", Bi("Forced", "إجبارية"), "Embedded", "SRT"),
        SubtitleTrack("fr", Bi("French", "الفرنسية"), "FR", null, "OpenSubtitles", "SRT"),
        SubtitleTrack("es", Bi("Spanish", "الإسبانية"), "ES", null, "OpenSubtitles", "SRT"),
        SubtitleTrack("tr", Bi("Turkish", "التركية"), "TR", null, "OpenSubtitles", "SRT"),
    )

    val audioTracks = listOf(
        AudioTrack("en-atmos", Bi("English", "الإنجليزية"), "EN", "TrueHD Atmos", "7.1", Bi("Original", "الأصلية"), isDefault = true),
        AudioTrack("en-ddp", Bi("English", "الإنجليزية"), "EN", "Dolby Digital+", "5.1", Bi("Compatibility", "توافق")),
        AudioTrack("ar-dd", Bi("Arabic", "العربية"), "AR", "Dolby Digital", "5.1", Bi("Dubbed", "مدبلجة")),
        AudioTrack("fr-ddp", Bi("French", "الفرنسية"), "FR", "Dolby Digital+", "5.1", Bi("Dubbed", "مدبلجة")),
        AudioTrack("en-comm", Bi("English", "الإنجليزية"), "EN", "AAC", "2.0", Bi("Director's commentary", "تعليق المخرج")),
    )

    // --- Search --------------------------------------------------------------------------------------

    fun search(query: String): List<ProtoTitle> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return all.filter { t ->
            t.title.en.lowercase().contains(q) ||
                t.title.ar.contains(q) ||
                t.director.en.lowercase().contains(q) ||
                t.director.ar.contains(q) ||
                t.genres.any { it.en.lowercase().contains(q) || it.ar.contains(q) } ||
                t.cast.any { it.name.en.lowercase().contains(q) || it.name.ar.contains(q) }
        }.sortedByDescending { t ->
            when {
                t.title.en.lowercase().startsWith(q) || t.title.ar.startsWith(q) -> 3
                t.title.en.lowercase().contains(q) || t.title.ar.contains(q) -> 2
                else -> 1
            }
        }
    }

    /** Suggestions shown before typing. */
    val searchSuggestions = listOf(
        Bi("Denis Villeneuve", "دوني فيلنوف"),
        Bi("Arabic cinema", "سينما عربية"),
        Bi("Dolby Vision", "دولبي فيجن"),
        Bi("Desert", "الصحراء"),
        Bi("Under 2 hours", "أقل من ساعتين"),
    )

    fun demoQuery(arabic: Boolean): String = if (arabic) "كثيب" else "Dune"

    /** Titles that also match the demo query loosely, so result grids aren't sparse. */
    fun demoResults(arabic: Boolean): List<ProtoTitle> {
        val direct = search(demoQuery(arabic))
        return (direct + listOf(br2049, arrival, lawrence, madmax, theeb, interstellar)).distinct()
    }
}
