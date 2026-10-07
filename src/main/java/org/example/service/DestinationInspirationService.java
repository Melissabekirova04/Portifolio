package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.NotFoundResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DestinationInspirationService {

    private final ObjectMapper mapper = new ObjectMapper();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public record Area(
            String name,
            String description
    ) {}

    public record DestinationImage(
            String url,
            String alt,
            String sourceUrl
    ) {}

    public record PlanningAction(
            String method,
            String endpoint,
            Map<String, String> body
    ) {}

    public record DestinationDetails(
            String airportCode,
            String city,
            String country,
            String description,
            List<Area> popularAreas,
            List<DestinationImage> images,
            String imageStatus,
            PlanningAction planning
    ) {}

    private record Inspiration(
            String city,
            String country,
            String wikipediaTitle,
            String description,
            List<Area> areas
    ) {}

    private final Map<String, Inspiration> catalog = Map.ofEntries(

            Map.entry("CPH", new Inspiration(
                    "København",
                    "Danmark",
                    "Copenhagen",
                    "København kombinerer historiske gader, havneliv "
                            + "og moderne bykultur. Byen passer godt til "
                            + "en storbyferie med mad, shopping og seværdigheder.",
                    List.of(
                            new Area(
                                    "Nyhavn",
                                    "Havneområde med farverige huse og restauranter."
                            ),
                            new Area(
                                    "Vesterbro",
                                    "Bydel med caféer, butikker og Kødbyen."
                            ),
                            new Area(
                                    "Christianshavn",
                                    "Område med kanaler og historiske bygninger."
                            )
                    )
            )),

            Map.entry("AAR", new Inspiration(
                    "Aarhus",
                    "Danmark",
                    "Aarhus",
                    "Aarhus byder på kunst, historie og et levende "
                            + "cafémiljø. Her kan du kombinere en byferie "
                            + "med ture til skov og strand.",
                    List.of(
                            new Area(
                                    "Latinerkvarteret",
                                    "Gader med små butikker og caféer."
                            ),
                            new Area(
                                    "Aarhus Ø",
                                    "Havneområde med moderne arkitektur."
                            ),
                            new Area(
                                    "Midtbyen",
                                    "Centrum med shopping og adgang til ARoS."
                            )
                    )
            )),

            Map.entry("AAL", new Inspiration(
                    "Aalborg",
                    "Danmark",
                    "Aalborg",
                    "Aalborg ligger ved Limfjorden og har både "
                            + "historiske gader og en moderne havnefront. "
                            + "Byen egner sig til en ferie med kultur og byliv.",
                    List.of(
                            new Area(
                                    "Havnefronten",
                                    "Område ved fjorden med promenader og kultur."
                            ),
                            new Area(
                                    "Midtbyen",
                                    "Centrum med butikker og historiske bygninger."
                            ),
                            new Area(
                                    "Jomfru Ane Gade",
                                    "Gade kendt for restauranter, barer og natteliv."
                            )
                    )
            )),

            Map.entry("BLL", new Inspiration(
                    "Billund",
                    "Danmark",
                    "Billund",
                    "Billund er et oplagt udgangspunkt for en "
                            + "familieferie med LEGO-oplevelser og aktiviteter "
                            + "i byen og dens omgivelser.",
                    List.of(
                            new Area(
                                    "Centrum og LEGO House",
                                    "Byens centrum med LEGO House som oplevelsessted."
                            ),
                            new Area(
                                    "LEGOLAND-området",
                                    "Området omkring forlystelsesparken LEGOLAND."
                            ),
                            new Area(
                                    "Lalandia-området",
                                    "Ferieområde med badeland og familieaktiviteter."
                            )
                    )
            )),

            Map.entry("OSL", new Inspiration(
                    "Oslo",
                    "Norge",
                    "Oslo",
                    "Oslo kombinerer fjord, natur og storbyliv. "
                            + "Du kan opleve museer og moderne arkitektur "
                            + "eller tage på ture i byens grønne omgivelser.",
                    List.of(
                            new Area(
                                    "Aker Brygge",
                                    "Havneområde med restauranter og udsigt over fjorden."
                            ),
                            new Area(
                                    "Bjørvika",
                                    "Område med Operahuset og moderne kulturbygninger."
                            ),
                            new Area(
                                    "Grünerløkka",
                                    "Bydel med caféer, små butikker og parker."
                            )
                    )
            )),

            Map.entry("BGO", new Inspiration(
                    "Bergen",
                    "Norge",
                    "Bergen",
                    "Bergen er omgivet af bjerge og har en historisk "
                            + "havn. Byen er et godt udgangspunkt for "
                            + "naturoplevelser og ture i fjordlandskabet.",
                    List.of(
                            new Area(
                                    "Bryggen",
                                    "Historisk havneområde med træbygninger."
                            ),
                            new Area(
                                    "Vågen",
                                    "Havneområde tæt på Fisketorget."
                            ),
                            new Area(
                                    "Fløyen",
                                    "Bjergområde med udsigt og vandrestier."
                            )
                    )
            )),

            Map.entry("TRD", new Inspiration(
                    "Trondheim",
                    "Norge",
                    "Trondheim",
                    "Trondheim har historiske bygninger, et aktivt "
                            + "studieliv og hyggelige områder langs Nidelva. "
                            + "Byen passer til en ferie med kultur og gåture.",
                    List.of(
                            new Area(
                                    "Bakklandet",
                                    "Historisk bydel med træhuse og caféer."
                            ),
                            new Area(
                                    "Midtbyen",
                                    "Centrum med butikker og Nidarosdomen."
                            ),
                            new Area(
                                    "Solsiden",
                                    "Tidligere industriområde med restauranter."
                            )
                    )
            )),

            Map.entry("SVG", new Inspiration(
                    "Stavanger",
                    "Norge",
                    "Stavanger",
                    "Stavanger kombinerer en historisk bykerne "
                            + "med kystliv. Byen kan bruges som udgangspunkt "
                            + "for udflugter til Lysefjorden.",
                    List.of(
                            new Area(
                                    "Gamle Stavanger",
                                    "Historisk område med hvide træhuse."
                            ),
                            new Area(
                                    "Vågen",
                                    "Byens havneområde med restauranter."
                            ),
                            new Area(
                                    "Fargegaten",
                                    "Farverig gade med caféer og butikker."
                            )
                    )
            )),

            Map.entry("ARN", new Inspiration(
                    "Stockholm",
                    "Sverige",
                    "Stockholm",
                    "Stockholm er bygget på øer og byder på "
                            + "historiske kvarterer, museer og udsigt over vandet. "
                            + "Byen passer godt til en varieret storbyferie.",
                    List.of(
                            new Area(
                                    "Gamla stan",
                                    "Den gamle bydel med historiske gader."
                            ),
                            new Area(
                                    "Södermalm",
                                    "Bydel med caféer, butikker og udsigtspunkter."
                            ),
                            new Area(
                                    "Djurgården",
                                    "Grønt område med museer og seværdigheder."
                            )
                    )
            )),

            Map.entry("GOT", new Inspiration(
                    "Göteborg",
                    "Sverige",
                    "Gothenburg",
                    "Göteborg har kanaler, caféer og en maritim "
                            + "atmosfære. Her kan du kombinere byoplevelser "
                            + "med udflugter mod kysten og skærgården.",
                    List.of(
                            new Area(
                                    "Haga",
                                    "Historisk kvarter med caféer og små butikker."
                            ),
                            new Area(
                                    "Avenyn",
                                    "Central boulevard med restauranter og kultur."
                            ),
                            new Area(
                                    "Linnéstaden",
                                    "Bydel med spisesteder tæt på Slottsskogen."
                            )
                    )
            )),

            Map.entry("MMX", new Inspiration(
                    "Malmö",
                    "Sverige",
                    "Malmö",
                    "Malmö kombinerer historiske pladser og "
                            + "moderne arkitektur med et varieret madmiljø. "
                            + "Byen er velegnet til en kort storbyferie.",
                    List.of(
                            new Area(
                                    "Gamla staden",
                                    "Historisk centrum med blandt andet Lilla Torg."
                            ),
                            new Area(
                                    "Västra Hamnen",
                                    "Moderne havneområde med Turning Torso."
                            ),
                            new Area(
                                    "Möllevången",
                                    "Område med torveliv og mange spisesteder."
                            )
                    )
            )),

            Map.entry("UME", new Inspiration(
                    "Umeå",
                    "Sverige",
                    "Umeå",
                    "Umeå er en universitetsby i det nordlige "
                            + "Sverige med kunst, kultur og adgang til natur. "
                            + "Byen passer til en ferie med både byliv og ro.",
                    List.of(
                            new Area(
                                    "Centrum",
                                    "Byens centrale område med butikker og caféer."
                            ),
                            new Area(
                                    "Åpromenaden",
                                    "Område langs Umeälven til gåture ved vandet."
                            ),
                            new Area(
                                    "Öst på stan",
                                    "Bydel med adgang til Bildmuseet og kunstområdet."
                            )
                    )
            ))
    );

    public DestinationDetails getDetails(String airportCode) {
        String code = airportCode.trim().toUpperCase(Locale.ROOT);
        Inspiration inspiration = catalog.get(code);

        if (inspiration == null) {
            throw new NotFoundResponse("Destinationen findes ikke.");
        }

        List<DestinationImage> images = fetchImages(inspiration);

        return new DestinationDetails(
                code,
                inspiration.city(),
                inspiration.country(),
                inspiration.description(),
                inspiration.areas(),
                images,
                images.isEmpty() ? "unavailable" : "available",
                new PlanningAction(
                        "POST",
                        "/api/trip-draft",
                        Map.of("destination", code)
                )
        );
    }

    private List<DestinationImage> fetchImages(
            Inspiration inspiration
    ) {
        String title = URLEncoder.encode(
                inspiration.wikipediaTitle(),
                StandardCharsets.UTF_8
        ).replace("+", "%20");

        String articleUrl = "https://en.wikipedia.org/wiki/" + title;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://en.wikipedia.org/api/rest_v1/page/summary/"
                                + title
                ))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .header(
                        "User-Agent",
                        "TravelPlanner/1.0 "
                                + "(https://melissabekirova04.github.io/Portifolio/)"
                )
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                return List.of();
            }

            JsonNode result = mapper.readTree(response.body());

            String imageUrl = result.path("originalimage")
                    .path("source")
                    .asText("");

            if (imageUrl.isBlank()) {
                imageUrl = result.path("thumbnail")
                        .path("source")
                        .asText("");
            }

            if (imageUrl.isBlank()) {
                return List.of();
            }

            return List.of(new DestinationImage(
                    imageUrl,
                    "Billede fra artiklen om " + inspiration.city(),
                    articleUrl
            ));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();

        } catch (IOException e) {
            return List.of();
        }
    }
}