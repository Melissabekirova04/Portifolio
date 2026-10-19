package org.example.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ExperienceService {

    public record Experience(
            String id,
            String destination,
            String name,
            String category,
            String description,
            String details,
            Double rating,
            BigDecimal priceAmount,
            String currency,
            String priceNotice,
            String sourceUrl
    ) {}

    private final Map<String, List<Experience>> experiences = Map.ofEntries(
            Map.entry("CPH", List.of(
                    create(
                            "CPH-tivoli", "CPH", "Tivoli",
                            "Forlystelsespark",
                            "Forlystelser og haveoplevelser i København.",
                            "Et forslag til en dag med forlystelser "
                                    + "og en tur gennem haven. Kontrollér "
                                    + "åbningsdage og billetmuligheder før besøget.",
                            "https://www.visitdenmark.com/denmark/highlights/"
                                    + "copenhagen/must-sees-copenhagen"
                    ),
                    create(
                            "CPH-nyhavn", "CPH", "Nyhavn",
                            "Byvandring",
                            "Gåtur langs kanalen og de farverige huse.",
                            "Udforsk havneområdet til fods og oplev "
                                    + "stemningen langs vandet. Mad, drikke "
                                    + "og eventuelle bådture købes separat.",
                            "https://www.visitdenmark.com/denmark/highlights/"
                                    + "copenhagen/must-sees-copenhagen"
                    ),
                    create(
                            "CPH-christiania", "CPH", "Christiania",
                            "Kultur",
                            "Et særpræget område med kunst og alternative bymiljøer.",
                            "Gå på opdagelse i områdets arkitektur "
                                    + "og kreative miljø. Følg lokale "
                                    + "skilte og regler under besøget.",
                            "https://www.visitdenmark.com/denmark/highlights/"
                                    + "copenhagen/must-sees-copenhagen"
                    )
            )),
            Map.entry("AAR", List.of(
                    create(
                            "AAR-aros", "AAR", "ARoS",
                            "Kunstmuseum",
                            "Kunst og udstillinger i Aarhus.",
                            "Et forslag til dig, der vil opleve "
                                    + "kunst og udsigt over byen. Se museets "
                                    + "aktuelle udstillinger før besøget.",
                            "https://www.visitdenmark.com/travel-trade/"
                                    + "various/wonders-west-denmark"
                    ),
                    create(
                            "AAR-den-gamle-by", "AAR", "Den Gamle By",
                            "Historie",
                            "Oplev historiske bymiljøer og dansk hverdagsliv.",
                            "Gå gennem forskellige tidsperioder "
                                    + "og oplev, hvordan byliv og boliger "
                                    + "har ændret sig.",
                            "https://www.visitdenmark.com/press/"
                                    + "press-room/denmarks-architecture"
                    ),
                    create(
                            "AAR-moesgaard", "AAR", "Moesgaard Museum",
                            "Historie",
                            "Udstillinger om mennesker og fortid syd for Aarhus.",
                            "Et forslag til en museumsdag med "
                                    + "arkæologi og kulturhistorie. Planlæg "
                                    + "transporten fra Aarhus centrum.",
                            "https://www.visitdenmark.com/denmark/"
                                    + "plan-your-trip/moesgaard-gdk644192"
                    )
            )),
            Map.entry("AAL", List.of(
                    create(
                            "AAL-utzon", "AAL", "Utzon Center",
                            "Arkitektur",
                            "Arkitektur og design ved Aalborgs havnefront.",
                            "Udforsk udstillinger med fokus på "
                                    + "arkitektur og design. Besøget kan "
                                    + "kombineres med en tur langs havnen.",
                            "https://www.visitdenmark.com/denmark/"
                                    + "destinations/jutland/aalborg"
                    ),
                    create(
                            "AAL-kunsten", "AAL", "Kunsten",
                            "Kunstmuseum",
                            "Moderne kunst i en markant museumsbygning.",
                            "Oplev kunst og arkitektur på samme "
                                    + "besøg. Kontrollér hvilke udstillinger "
                                    + "der vises på dine rejsedatoer.",
                            "https://www.visitdenmark.com/denmark/"
                                    + "destinations/jutland/aalborg"
                    ),
                    create(
                            "AAL-zoo", "AAL", "Aalborg Zoo",
                            "Dyrepark",
                            "En oplevelse med dyr i Aalborg.",
                            "Et forslag til en udflugt for både "
                                    + "børn og voksne. Kontrollér "
                                    + "åbningstider og dagens program.",
                            "https://www.visitdenmark.com/denmark/"
                                    + "destinations/jutland/aalborg"
                    )
            )),
            Map.entry("BLL", List.of(
                    create(
                            "BLL-legoland", "BLL", "LEGOLAND Billund",
                            "Forlystelsespark",
                            "LEGO-tema og forlystelser i Billund.",
                            "Et forslag til en dag med LEGO-miljøer "
                                    + "og forlystelser. Kontrollér parkens "
                                    + "åbningskalender før planlægning.",
                            "https://www.visitdenmark.com/travel-trade/"
                                    + "various/wonders-west-denmark"
                    ),
                    create(
                            "BLL-lego-house", "BLL", "LEGO House",
                            "Kreativ oplevelse",
                            "Kreative oplevelser med LEGO.",
                            "Udforsk LEGO-universet gennem leg "
                                    + "og byggeri. Se billetmuligheder "
                                    + "og praktiske oplysninger før besøget.",
                            "https://www.visitdenmark.com/travel-trade/"
                                    + "various/wonders-west-denmark"
                    ),
                    create(
                            "BLL-wow-park", "BLL", "WOW PARK Billund",
                            "Udendørs oplevelse",
                            "Aktiv leg og oplevelser i skoven.",
                            "Et forslag til en aktiv udflugt "
                                    + "i naturen. Vælg tøj efter vejret "
                                    + "og kontrollér sæsonens åbningsdage.",
                            "https://www.visitdenmark.com/denmark/"
                                    + "destinations/jutland/billund"
                    )
            )),
            Map.entry("OSL", List.of(
                    create(
                            "OSL-opera", "OSL", "Operahuset i Oslo",
                            "Arkitektur",
                            "Markant arkitektur ved vandet i Bjørvika.",
                            "Oplev bygningen og området ved havnen. "
                                    + "Taget giver mulighed for udsigt "
                                    + "over byen. Forestillinger kræver "
                                    + "separat planlægning og billet.",
                            "https://www.visitnorway.dk/aktiviteter/oslo/"
                    ),
                    create(
                            "OSL-vigeland", "OSL", "Vigelandsparken",
                            "Kunst og park",
                            "Skulpturer af Gustav Vigeland i Frognerparken.",
                            "Gå gennem skulpturområdet og oplev "
                                    + "kunst i det fri. Besøget passer "
                                    + "til en rolig gåtur i Oslo.",
                            "https://www.visitnorway.dk/aktiviteter/oslo/"
                    ),
                    create(
                            "OSL-munch", "OSL", "MUNCH",
                            "Kunstmuseum",
                            "Oplev Edvard Munchs kunst i Oslo.",
                            "Museet i Bjørvika er et forslag til "
                                    + "kunstinteresserede. Kontrollér "
                                    + "aktuelle udstillinger og billetter.",
                            "https://www.visitnorway.dk/aktiviteter/oslo/"
                    )
            )),
            Map.entry("BGO", List.of(
                    create(
                            "BGO-bryggen", "BGO", "Bryggen",
                            "Historie",
                            "Historiske havnebygninger i Bergen.",
                            "Udforsk området omkring de gamle "
                                    + "bygninger og oplev butikker, "
                                    + "caféer og byens havnemiljø.",
                            "https://www.visitnorway.com/listings/"
                                    + "bryggen-in-bergen/18045/"
                    ),
                    create(
                            "BGO-floyen", "BGO", "Fløyen og Fløibanen",
                            "Natur og udsigt",
                            "En tur mod Fløyen med udsigt over Bergen.",
                            "Planlæg en tur med Fløibanen og "
                                    + "en gåtur på Fløyen. Kontrollér "
                                    + "vejr, drift og billetpriser.",
                            "https://www.visitnorway.com/places-to-go/"
                                    + "fjord-norway/bergen/plan-your-trip/"
                    ),
                    create(
                            "BGO-fisketorget", "BGO", "Fisketorget",
                            "Mad og byliv",
                            "Oplev Bergens fiskemarked ved havnen.",
                            "Gå på opdagelse i havneområdet og "
                                    + "markedets madoplevelser. Udgifter "
                                    + "afhænger af, hvad du vælger at købe.",
                            "https://www.visitnorway.com/places-to-go/"
                                    + "fjord-norway/bergen/plan-your-trip/"
                    )
            )),
            Map.entry("TRD", List.of(
                    create(
                            "TRD-nidaros", "TRD", "Nidarosdomen",
                            "Historie",
                            "Besøg Trondheims kendte domkirke.",
                            "Et forslag til dig, der interesserer "
                                    + "dig for historie og arkitektur. "
                                    + "Se besøgstider og billetmuligheder.",
                            "https://www.visitnorway.no/reisemal/"
                                    + "trondelag/trondheim/planlegg-reisen/"
                    ),
                    create(
                            "TRD-bakklandet", "TRD", "Bakklandet",
                            "Byvandring",
                            "Gåtur i et charmerende område ved Nidelva.",
                            "Udforsk gaderne og området omkring "
                                    + "Gamle Bybro. Besøget kan kombineres "
                                    + "med et caféstop.",
                            "https://www.visitnorway.com/places-to-go/"
                                    + "trondelag/trondheim/summer/"
                    ),
                    create(
                            "TRD-rockheim", "TRD", "Rockheim",
                            "Musikmuseum",
                            "Oplev norsk populærmusiks historie.",
                            "Museet bruger interaktive oplevelser "
                                    + "til at fortælle om musik og "
                                    + "musikliv i Norge.",
                            "https://www.visitnorway.com/places-to-go/"
                                    + "trondelag/trondheim/summer/"
                    )
            )),
            Map.entry("SVG", List.of(
                    create(
                            "SVG-gamle-stavanger", "SVG", "Gamle Stavanger",
                            "Byvandring",
                            "Historiske gader med hvide træhuse.",
                            "Gå på opdagelse i det gamle byområde "
                                    + "og oplev brostensgader, "
                                    + "caféer og gallerier.",
                            "https://www.visitnorway.com/places-to-go/"
                                    + "fjord-norway/the-stavanger-region/summer/"
                    ),
                    create(
                            "SVG-oliemuseum", "SVG", "Norsk Oljemuseum",
                            "Museum",
                            "Udstillinger om Norges offshoreindustri.",
                            "Lær om olieindustrien og dens "
                                    + "betydning for Norge gennem "
                                    + "museets udstillinger.",
                            "https://www.visitnorway.com/places-to-go/"
                                    + "fjord-norway/the-stavanger-region/summer/"
                    ),
                    create(
                            "SVG-kunstmuseum", "SVG", "Stavanger kunstmuseum",
                            "Kunstmuseum",
                            "Norsk kunst fra forskellige perioder.",
                            "Et forslag til en museumsdag med "
                                    + "kunst fra det nittende århundrede "
                                    + "og frem til samtidskunst.",
                            "https://www.visitnorway.com/places-to-go/"
                                    + "fjord-norway/the-stavanger-region/"
                                    + "winter-adventure/"
                    )
            )),
            Map.entry("ARN", List.of(
                    create(
                            "ARN-vasa", "ARN", "Vasamuseet",
                            "Historie",
                            "Et museum med fokus på skibet Vasa.",
                            "Udforsk skibets historie på Djurgården. "
                                    + "Besøget kan kombineres med andre "
                                    + "oplevelser på øen.",
                            "https://visitsweden.com/where-to-go/"
                                    + "middle-sweden/stockholm/culture-stockholm/"
                    ),
                    create(
                            "ARN-skansen", "ARN", "Skansen",
                            "Frilandsmuseum",
                            "Historiske bygninger og svensk kultur.",
                            "Oplev traditionelle miljøer og "
                                    + "bygninger fra forskellige dele "
                                    + "af Sverige på Djurgården.",
                            "https://visitsweden.com/where-to-go/"
                                    + "middle-sweden/stockholm/"
                                    + "royal-djurgarden-park-and-skansen/"
                    ),
                    create(
                            "ARN-gamla-stan", "ARN", "Gamla Stan",
                            "Byvandring",
                            "Stockholms gamle bydel med smalle gader.",
                            "Udforsk historiske gader, torve "
                                    + "og caféer til fods. Et forslag "
                                    + "til at opleve byens ældre centrum.",
                            "https://visitsweden.com/where-to-go/"
                                    + "middle-sweden/stockholm/culture-stockholm/"
                    )
            )),
            Map.entry("GOT", List.of(
                    create(
                            "GOT-liseberg", "GOT", "Liseberg",
                            "Forlystelsespark",
                            "Forlystelser og underholdning i Göteborg.",
                            "Et forslag til en dag i forlystelsespark. "
                                    + "Kontrollér åbningskalender "
                                    + "og billetmuligheder.",
                            "https://visitsweden.com/where-to-go/"
                                    + "southern-sweden/goteborg/"
                    ),
                    create(
                            "GOT-universeum", "GOT", "Universeum",
                            "Videnskab",
                            "Interaktive oplevelser med natur og videnskab.",
                            "Udforsk udstillinger om blandt andet "
                                    + "teknologi, dyreliv og regnskov. "
                                    + "Et forslag til både børn og voksne.",
                            "https://visitsweden.com/where-to-go/"
                                    + "southern-sweden/goteborg/"
                    ),
                    create(
                            "GOT-haga", "GOT", "Haga",
                            "Byvandring",
                            "Historisk kvarter med caféer og træhuse.",
                            "Gå gennem områdets brostensgader "
                                    + "og tag en pause på en café. "
                                    + "Et forslag til en rolig tur til fods.",
                            "https://visitsweden.com/where-to-go/"
                                    + "southern-sweden/goteborg/"
                    )
            )),
            Map.entry("MMX", List.of(
                    create(
                            "MMX-malmoehus", "MMX", "Malmöhus",
                            "Historie og museum",
                            "Historisk slot med museumsoplevelser.",
                            "Udforsk slottet og dets museumsområder. "
                                    + "Se aktuelle udstillinger og "
                                    + "billetmuligheder før besøget.",
                            "https://visitsweden.com/where-to-go/"
                                    + "southern-sweden/malmo/"
                    ),
                    create(
                            "MMX-vastra-hamnen", "MMX", "Västra Hamnen",
                            "Arkitektur og byvandring",
                            "Moderne havnekvarter ved Øresund.",
                            "Oplev områdets arkitektur, promenader "
                                    + "og udsigt over vandet. Turning Torso "
                                    + "kan ses fra området.",
                            "https://visitsweden.com/where-to-go/"
                                    + "southern-sweden/malmo/"
                    ),
                    create(
                            "MMX-ribersborg", "MMX", "Ribersborgsstranden",
                            "Natur og strand",
                            "En strandtur tæt på Malmö.",
                            "Gå langs vandet eller planlæg "
                                    + "en stranddag efter vejret. "
                                    + "Kallbadhuset er en separat oplevelse.",
                            "https://visitsweden.com/where-to-go/"
                                    + "southern-sweden/malmo/"
                    )
            )),
            Map.entry("UME", List.of(
                    create(
                            "UME-bildmuseet", "UME", "Bildmuseet",
                            "Kunstmuseum",
                            "Samtidskunst ved Umeälven.",
                            "Oplev udstillinger med kunst, design "
                                    + "og arkitektur ved Umeå Arts Campus.",
                            "https://visitsweden.com/where-to-go/"
                                    + "northern-sweden/vasterbotten/umea/"
                    ),
                    create(
                            "UME-vasterbotten", "UME", "Västerbottens museum",
                            "Historie",
                            "Oplev Västerbottens kultur og historie.",
                            "Udforsk regionale traditioner og "
                                    + "historie gennem museets udstillinger.",
                            "https://visitsweden.com/where-to-go/"
                                    + "northern-sweden/vasterbotten/umea/"
                    ),
                    create(
                            "UME-umedalen", "UME", "Umedalen Skulpturpark",
                            "Kunst og park",
                            "Skulpturer i et udendørs parkområde.",
                            "Oplev værker af svenske og internationale "
                                    + "kunstnere på en gåtur uden for "
                                    + "Umeås centrum.",
                            "https://visitsweden.com/where-to-go/"
                                    + "northern-sweden/vasterbotten/umea/"
                    )
            ))
    );

    public List<Experience> findByDestination(String destination) {
        return experiences.getOrDefault(
                normalize(destination),
                List.of()
        );
    }

    public boolean supportsDestination(String destination) {
        return experiences.containsKey(normalize(destination));
    }

    public Optional<Experience> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        String normalizedId = id.trim();

        return experiences.values()
                .stream()
                .flatMap(List::stream)
                .filter(experience ->
                        experience.id().equalsIgnoreCase(normalizedId))
                .findFirst();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
    }

    private Experience create(
            String id,
            String destination,
            String name,
            String category,
            String description,
            String details,
            String sourceUrl
    ) {
        return new Experience(
                id,
                destination,
                name,
                category,
                description,
                details,
                null,
                null,
                null,
                "Pris og rating er ikke oplyst. "
                        + "Kontrollér aktuelle oplysninger hos udbyderen. "
                        + "Forslaget er ikke kontrolleret mod dine rejsedatoer.",
                sourceUrl
        );
    }
}