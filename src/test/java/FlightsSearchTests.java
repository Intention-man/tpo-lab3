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
    private final String MOSCOW_FULL = "Москва";
    private final String SPB_FULL = "Санкт-Петербург";
    private final String SOCHI_FULL = "Сочи";
    private final String KGD_FULL = "Калининград";

    private final String MOSCOW_SEARCH = "Москва";
    private final String SPB_SEARCH = "Санкт-П";
    private final String SOCHI_SEARCH = "Сочи";
    private final String KGD_SEARCH = "Калининград";

    private final String MOW_CODE = "MOW";
    private final String LED_CODE = "LED";
    private final String AER_CODE = "AER";
    private final String KGD_CODE = "KGD";

    private FlightsSearchPage flightsSearchPage;

    @BeforeMethod
    public void pageSetup() {
        flightsSearchPage = new FlightsSearchPage(driver);
        flightsSearchPage.open();
    }

    private void switchToNewTab(String originalWindowHandle, Set<String> originalHandles) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

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
    }

    @Test(description = "TC1: Успешный поиск билета в одну сторону")
    public void testSuccessfulOneWayFlightSearch() {
        flightsSearchPage.enterDepartureCityForSegment(1, MOSCOW_FULL, MOSCOW_SEARCH);
        flightsSearchPage.enterArrivalCityForSegment(1, SPB_FULL, SPB_SEARCH);
        LocalDate departureDate = LocalDate.now().plusDays(7);

        flightsSearchPage.openCalendarForSegment(1);
        flightsSearchPage.selectOneWayTripModeInCalendar();
        flightsSearchPage.selectDepartureDate(departureDate);
        flightsSearchPage.ensureCalendarClosed();

        Assert.assertTrue(flightsSearchPage.isReturnDateEffectivelyEmptyOrDisabled());

        String originalWindowHandle = driver.getWindowHandle();
        Set<String> originalHandles = driver.getWindowHandles();
        flightsSearchPage.clickSearchButton();
        switchToNewTab(originalWindowHandle, originalHandles);

        Assert.assertTrue(flightsSearchPage.isOnResultsPage());
        String currentUrl = flightsSearchPage.getCurrentUrl();
        String formattedDepartureDate = departureDate.format(DateTimeFormatter.ofPattern("MM-dd"));
        Assert.assertTrue(currentUrl.contains(FlightsSearchPage.ONE_WAY_PATH_SEGMENT));
        Assert.assertTrue(currentUrl.contains("/" + MOW_CODE + "-" + LED_CODE + "/"));
        String oneWayRegexSegment = FlightsSearchPage.ONE_WAY_PATH_SEGMENT.replace("/", "\\/");
        Assert.assertTrue(currentUrl.matches(".*/flights" + oneWayRegexSegment + MOW_CODE + "-" + LED_CODE + "\\/" + formattedDepartureDate + "/\\?.*"));
    }

    @Test(description = "TC2: Успешный поиск билета туда-обратно (multi-way)")
    public void testSuccessfulRoundTripFlightSearch() {
        String departureCityCodeForUrl = "MOW";
        String arrivalCityCodeForUrl = "AER";

        flightsSearchPage.enterDepartureCityForSegment(1, MOSCOW_FULL, MOSCOW_SEARCH);
        flightsSearchPage.enterArrivalCityForSegment(1, SOCHI_FULL, SOCHI_SEARCH);
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

        flightsSearchPage.enterArrivalCityForSegment(1, SPB_FULL, SPB_SEARCH);

        flightsSearchPage.openCalendar();
        flightsSearchPage.selectOneWayTripModeInCalendar();
        flightsSearchPage.selectDepartureDate(LocalDate.now().plusDays(5));
        flightsSearchPage.ensureCalendarClosed();

        Assert.assertFalse(flightsSearchPage.isSearchButtonFunctionallyEnabled(),
                "Search button should be functionally disabled without departure city.");
    }

    @Test(description = "TC4: Попытка поиска без указания города назначения")
    public void testSearchWithoutArrivalCity() {
        flightsSearchPage.enterDepartureCityForSegment(1, MOSCOW_FULL, MOSCOW_SEARCH);

        flightsSearchPage.openCalendar();
        flightsSearchPage.selectOneWayTripModeInCalendar();
        flightsSearchPage.selectDepartureDate(LocalDate.now().plusDays(5));
        flightsSearchPage.ensureCalendarClosed();

        Assert.assertFalse(flightsSearchPage.isSearchButtonFunctionallyEnabled(),
                "Search button should be functionally disabled without departure city.");
    }

    @Test(description = "TC5: Попытка поиска с одинаковыми городами отправления и назначения")
    public void testSearchWithSameDepartureAndArrivalCity() {
        flightsSearchPage.enterDepartureCityForSegment(1, MOSCOW_FULL, MOSCOW_SEARCH);
        flightsSearchPage.enterArrivalCityForSegment(1, MOSCOW_FULL, MOSCOW_SEARCH);

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

    @Test(description = "TC6: Успешный поиск сложного маршрута (2 сегмента)")
    public void testSuccessfulComplexRouteTwoSegmentsSearch() {
        LocalDate date1 = LocalDate.now().plusDays(7);
        LocalDate date2 = LocalDate.now().plusDays(14);

        flightsSearchPage.switchToComplexRouteMode();

        flightsSearchPage.enterDepartureCityForSegment(1, MOSCOW_FULL, MOSCOW_SEARCH);
        flightsSearchPage.enterArrivalCityForSegment(1, SPB_FULL, SPB_SEARCH);
        flightsSearchPage.selectDateForSegment(1, date1);

        flightsSearchPage.enterDepartureCityForSegment(2, SPB_FULL, SPB_SEARCH); // Вылет из СПБ
        flightsSearchPage.enterArrivalCityForSegment(2, KGD_FULL, KGD_SEARCH);    // Прилет в Калининград
        flightsSearchPage.selectDateForSegment(2, date2);

        flightsSearchPage.clickSearchButton();

        Assert.assertTrue(flightsSearchPage.isOnResultsPage(), "Should be on search results page for complex route.");
        String currentUrl = flightsSearchPage.getCurrentUrl();
        String formattedDate1 = date1.format(DateTimeFormatter.ofPattern("MM-dd"));
        String formattedDate2 = date2.format(DateTimeFormatter.ofPattern("MM-dd"));

        Assert.assertTrue(currentUrl.contains(FlightsSearchPage.MULTI_WAY_PATH_SEGMENT), "URL should contain multi-way segment for complex route.");

        String expectedUrlPattern = ".*/flights" + FlightsSearchPage.MULTI_WAY_PATH_SEGMENT.replace("/", "\\/") +
                MOW_CODE + "-" + LED_CODE + "\\/" + formattedDate1 + "\\/" +
                LED_CODE + "-" + KGD_CODE + "\\/" + formattedDate2 +
                "/\\?.*";
        Assert.assertTrue(currentUrl.matches(expectedUrlPattern),
                "URL structure for complex route (2 segments) is incorrect. \nExpected pattern: " + expectedUrlPattern + "\nActual URL: " + currentUrl);
    }

}
