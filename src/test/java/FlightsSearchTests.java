import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;


public class FlightsSearchTests extends BaseTest {

    private final String DEPARTURE_CITY_FULL = "Москва";
    private final String ARRIVAL_CITY_SPB_FULL = "Санкт-Петербург";
    private final String ARRIVAL_CITY_SOCHI_FULL = "Сочи";
    private final String DEPARTURE_CITY_SEARCH = "Москва";
    private final String ARRIVAL_CITY_SPB_SEARCH = "Санкт-П";
    private final String ARRIVAL_CITY_SOCHI_SEARCH = "Сочи";
    private FlightsSearchPage flightsSearchPage;

    @BeforeMethod
    public void pageSetup() {
        flightsSearchPage = new FlightsSearchPage(driver);
        flightsSearchPage.open();
    }

    private String switchToNewTab(String originalWindowHandle, Set<String> originalHandles) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10)); // Ожидание появления новой вкладки

        wait.until(ExpectedConditions.numberOfWindowsToBe(originalHandles.size() + 1));

        Set<String> allHandles = driver.getWindowHandles();
        String newWindowHandle = null;

        for (String handle : allHandles) {
            if (!originalHandles.contains(handle)) {
                newWindowHandle = handle;
                break;
            }
        }

        if (newWindowHandle != null) {
            System.out.println("Switching to new tab: " + newWindowHandle);
            driver.switchTo().window(newWindowHandle);
        } else {
            System.err.println("Could not find the new tab handle. Current handles: " + allHandles);
            Assert.fail("Failed to find and switch to the new results tab.");
        }
        return newWindowHandle;
    }

    @Test(description = "TC1: Успешный поиск билета в одну сторону")
    public void testSuccessfulOneWayFlightSearch() {
        flightsSearchPage.enterDepartureCity(DEPARTURE_CITY_FULL, DEPARTURE_CITY_SEARCH);
        flightsSearchPage.enterArrivalCity(ARRIVAL_CITY_SPB_FULL, ARRIVAL_CITY_SPB_SEARCH);
        LocalDate departureDate = LocalDate.now().plusDays(7);

        flightsSearchPage.openCalendar();
        flightsSearchPage.selectOneWayTripModeInCalendar();
        flightsSearchPage.selectDepartureDate(departureDate);
        flightsSearchPage.ensureCalendarClosed();

        Assert.assertTrue(flightsSearchPage.isReturnDateEffectivelyEmptyOrDisabled(),
                "Mode should indicate one-way trip or calendar should be closed.");

        String originalWindowHandle = driver.getWindowHandle();
        Set<String> originalHandles = driver.getWindowHandles();
        flightsSearchPage.clickSearchButton();
        switchToNewTab(originalWindowHandle, originalHandles);

        Assert.assertTrue(flightsSearchPage.isOnResultsPage(), "Should be on search results page in the new tab.");
        String currentUrl = flightsSearchPage.getCurrentUrl();
        String formattedDepartureDate = departureDate.format(DateTimeFormatter.ofPattern("MM-dd"));

        Assert.assertTrue(currentUrl.contains(FlightsSearchPage.ONE_WAY_PATH_SEGMENT), "URL should contain '" + FlightsSearchPage.ONE_WAY_PATH_SEGMENT + "'.");
        Assert.assertTrue(currentUrl.contains("/MOW-LED/"), "URL should contain departure-arrival codes like '/MOW-LED/'.");
        String oneWayRegexSegment = FlightsSearchPage.ONE_WAY_PATH_SEGMENT.replace("/", "\\/");
        Assert.assertTrue(currentUrl.matches(".*/flights" + oneWayRegexSegment + "[A-Z]{3}-[A-Z]{3}/" + formattedDepartureDate + "/\\?.*"),
                "URL structure for one-way trip is incorrect. Expected format like .../flights/one-way/MOW-LED/MM-dd/?...");
    }

    @Test(description = "TC2: Успешный поиск билета туда-обратно (multi-way)")
    public void testSuccessfulRoundTripFlightSearch() {
        String departureCityCodeForUrl = "MOW";
        String arrivalCityCodeForUrl = "AER";

        flightsSearchPage.enterDepartureCity(DEPARTURE_CITY_FULL, DEPARTURE_CITY_SEARCH);
        flightsSearchPage.enterArrivalCity(ARRIVAL_CITY_SOCHI_FULL, ARRIVAL_CITY_SOCHI_SEARCH);
        LocalDate departureDate = LocalDate.now().plusDays(10);
        LocalDate returnDate = LocalDate.now().plusDays(17);
        flightsSearchPage.openCalendar();
        flightsSearchPage.selectRoundTripModeInCalendar();
        flightsSearchPage.selectDepartureDate(departureDate);
        flightsSearchPage.selectReturnDate(returnDate);

        String originalWindowHandle = driver.getWindowHandle();
        Set<String> originalHandles = driver.getWindowHandles();
        flightsSearchPage.clickSearchButton();
        switchToNewTab(originalWindowHandle, originalHandles);

        Assert.assertTrue(flightsSearchPage.isOnResultsPage(), "Should be on search results page for round trip in the new tab.");
        String currentUrl = flightsSearchPage.getCurrentUrl();
        String formattedDepartureDate = departureDate.format(DateTimeFormatter.ofPattern("MM-dd"));
        String formattedReturnDate = returnDate.format(DateTimeFormatter.ofPattern("MM-dd"));

        Assert.assertTrue(currentUrl.contains(FlightsSearchPage.MULTI_WAY_PATH_SEGMENT), "URL should contain '" + FlightsSearchPage.MULTI_WAY_PATH_SEGMENT + "'.");

        Assert.assertTrue(currentUrl.contains("/" + departureCityCodeForUrl + "-" + arrivalCityCodeForUrl + "/" + formattedDepartureDate + "/"),
                "URL should contain the 'there' leg: /" + departureCityCodeForUrl + "-" + arrivalCityCodeForUrl + "/" + formattedDepartureDate + "/");

        Assert.assertTrue(currentUrl.contains("/" + arrivalCityCodeForUrl + "-" + departureCityCodeForUrl + "/" + formattedReturnDate + "/"),
                "URL should contain the 'back' leg: /" + arrivalCityCodeForUrl + "-" + departureCityCodeForUrl + "/" + formattedReturnDate + "/");

        String multiWayRegexSegment = FlightsSearchPage.MULTI_WAY_PATH_SEGMENT.replace("/", "\\/");
        String expectedRouteThere = departureCityCodeForUrl + "-" + arrivalCityCodeForUrl;
        String expectedRouteBack = arrivalCityCodeForUrl + "-" + departureCityCodeForUrl;

        String regexPattern = ".*/flights" + multiWayRegexSegment + // Начало URL и /multi-way/
                expectedRouteThere + "\\/" + formattedDepartureDate + "\\/" + // Рейс туда: MOW-AER/MM-dd/
                expectedRouteBack + "\\/" + formattedReturnDate +            // Рейс обратно: AER-MOW/MM-dd
                "/\\?.*";

        Assert.assertTrue(currentUrl.matches(regexPattern),
                "URL structure for multi-way (round trip) is incorrect.\nExpected pattern: " + regexPattern + "\nActual URL: " + currentUrl);
    }

    @Test(description = "TC3: Попытка поиска без указания города отправления")
    public void testSearchWithoutDepartureCity() {
        flightsSearchPage.clearDepartureCity();
        flightsSearchPage.enterArrivalCity(ARRIVAL_CITY_SPB_FULL, ARRIVAL_CITY_SPB_SEARCH);

        flightsSearchPage.openCalendar();
        flightsSearchPage.selectOneWayTripModeInCalendar();
        flightsSearchPage.selectDepartureDate(LocalDate.now().plusDays(5));
        flightsSearchPage.ensureCalendarClosed();

        Assert.assertFalse(flightsSearchPage.isSearchButtonFunctionallyEnabled(),
                "Search button should be functionally disabled without departure city.");
    }

    @Test(description = "TC4: Попытка поиска без указания города назначения")
    public void testSearchWithoutArrivalCity() {
        flightsSearchPage.enterDepartureCity(DEPARTURE_CITY_FULL, DEPARTURE_CITY_SEARCH);

        flightsSearchPage.openCalendar();
        flightsSearchPage.selectOneWayTripModeInCalendar();
        flightsSearchPage.selectDepartureDate(LocalDate.now().plusDays(5));
        flightsSearchPage.ensureCalendarClosed();

        Assert.assertFalse(flightsSearchPage.isSearchButtonFunctionallyEnabled(),
                "Search button should be functionally disabled without departure city.");
    }

    @Test(description = "TC5: Попытка поиска с одинаковыми городами отправления и назначения")
    public void testSearchWithSameDepartureAndArrivalCity() {
        flightsSearchPage.enterDepartureCity(DEPARTURE_CITY_FULL, DEPARTURE_CITY_SEARCH);
        flightsSearchPage.enterArrivalCity(DEPARTURE_CITY_FULL, DEPARTURE_CITY_SEARCH);

        try {
            flightsSearchPage.openCalendar();
            flightsSearchPage.selectRoundTripModeInCalendar();
            LocalDate departureDate = LocalDate.now().plusDays(12);
            flightsSearchPage.selectDepartureDate(departureDate);
            flightsSearchPage.selectReturnDate(departureDate.plusDays(3));
            flightsSearchPage.ensureCalendarClosed();
        } catch (Exception e) {
            System.out.println("Date selection might fail if city input error occurred, which is expected.");
        }

        Assert.assertFalse(flightsSearchPage.isSearchButtonFunctionallyEnabled(),
                "Search button should be functionally disabled without departure city.");
    }

}
