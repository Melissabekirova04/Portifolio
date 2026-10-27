package org.example.service;

import io.javalin.http.BadGatewayResponse;
import io.javalin.http.ServiceUnavailableResponse;
import org.example.model.AirportWeather;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WeatherServiceTest {

    @Test
    void returnsWeatherForAllDestinationsInOrder() {
        Map<String, AirportWeather> fakeResults =
                new ConcurrentHashMap<>();

        WeatherService service = new WeatherService() {
            @Override
            public AirportWeather getWeatherByAirportCode(String code) {
                AirportWeather weather = fakeWeather(code);

                fakeResults.put(code, weather);

                return weather;
            }
        };

        List<AirportWeather> results =
                service.getAllAirportWeather();

        List<String> expectedCodes = List.of(
                "CPH", "AAR", "AAL", "BLL",
                "OSL", "BGO", "TRD", "SVG",
                "ARN", "GOT", "MMX", "UME"
        );

        assertEquals(12, results.size());
        assertEquals(12, fakeResults.size());

        for (int i = 0; i < expectedCodes.size(); i++) {
            assertSame(
                    fakeResults.get(expectedCodes.get(i)),
                    results.get(i)
            );
        }
    }

    @Test
    @Timeout(20)
    void runsFourWeatherTasksAtTheSameTime() {
        AtomicInteger activeTasks = new AtomicInteger();
        AtomicInteger highestActiveCount = new AtomicInteger();

        // De første fire opgaver venter på hinanden.
        CountDownLatch firstFourStarted = new CountDownLatch(4);

        WeatherService service = new WeatherService() {
            @Override
            public AirportWeather getWeatherByAirportCode(String code) {
                int active = activeTasks.incrementAndGet();

                highestActiveCount.accumulateAndGet(
                        active,
                        Math::max
                );

                firstFourStarted.countDown();

                try {
                    boolean allStarted = firstFourStarted.await(
                            3,
                            TimeUnit.SECONDS
                    );

                    if (!allStarted) {
                        throw new IllegalStateException(
                                "Fire opgaver blev ikke startet samtidig."
                        );
                    }

                    return fakeWeather(code);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    throw new IllegalStateException(
                            "Testopgaven blev afbrudt.",
                            e
                    );
                } finally {
                    activeTasks.decrementAndGet();
                }
            }
        };

        List<AirportWeather> results =
                service.getAllAirportWeather();

        assertEquals(12, results.size());
        assertEquals(4, highestActiveCount.get());
        assertEquals(0, activeTasks.get());
    }

    @Test
    void preservesServiceUnavailableError() {
        WeatherService service = new WeatherService() {
            @Override
            public AirportWeather getWeatherByAirportCode(String code) {
                throw new ServiceUnavailableResponse(
                        "Vejrtjenestens grænse er nået."
                );
            }
        };

        ServiceUnavailableResponse error = assertThrows(
                ServiceUnavailableResponse.class,
                service::getAllAirportWeather
        );

        assertEquals(
                "Vejrtjenestens grænse er nået.",
                error.getMessage()
        );
    }

    @Test
    void unexpectedTaskFailureBecomesBadGateway() {
        WeatherService service = new WeatherService() {
            @Override
            public AirportWeather getWeatherByAirportCode(String code) {
                throw new IllegalStateException(
                        "Simuleret fejl i vejropgaven"
                );
            }
        };

        BadGatewayResponse error = assertThrows(
                BadGatewayResponse.class,
                service::getAllAirportWeather
        );

        assertEquals(
                "Kunne ikke hente vejret for alle destinationer.",
                error.getMessage()
        );
    }

    private AirportWeather fakeWeather(String code) {
        return new AirportWeather(
                "Testby",
                "Testland",
                code,
                15.0,
                10.0,
                0,
                "Klart vejr"
        );
    }
}