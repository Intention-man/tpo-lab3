import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.openqa.selenium.support.ui.WebDriverWait; // Добавьте, если еще нет
import org.openqa.selenium.support.ui.ExpectedConditions; // Добавьте, если еще нет
import org.openqa.selenium.By;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.time.Duration;


public class FlightsSearchTests extends BaseTest {

    private FlightsSearchPage flightsSearchPage;
    private final String DEPARTURE_CITY_FULL = "Москва";
    private final String ARRIVAL_CITY_SPB_FULL = "Санкт-Петербург";
    private final String ARRIVAL_CITY_SOCHI_FULL = "Сочи";

    private final String DEPARTURE_CITY_SEARCH = "Москва";
    private final String ARRIVAL_CITY_SPB_SEARCH = "Санкт-П";
    private final String ARRIVAL_CITY_SOCHI_SEARCH = "Сочи";

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
        flightsSearchPage.selectDepartureDate(departureDate);
        flightsSearchPage.selectOneWayTripModeInCalendar();
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
        Assert.assertTrue(currentUrl.contains("/one-way/"), "URL should contain '/one-way/'.");
        Assert.assertTrue(currentUrl.contains("/MOW-LED/"), "URL should contain departure-arrival codes like '/MOW-LED/'.");
        Assert.assertTrue(currentUrl.matches(".*/flights/one-way/[A-Z]{3}-[A-Z]{3}/" + formattedDepartureDate + "/\\?.*"),
                "URL structure for one-way trip is incorrect. Expected format like .../one-way/MOW-LED/MM-dd/?...");
    }

    @Test(description = "TC2: Успешный поиск билета туда-обратно")
    public void testSuccessfulRoundTripFlightSearch() {
        flightsSearchPage.enterDepartureCity(DEPARTURE_CITY_FULL, DEPARTURE_CITY_SEARCH);
        flightsSearchPage.enterArrivalCity(ARRIVAL_CITY_SOCHI_FULL, ARRIVAL_CITY_SOCHI_SEARCH);
        LocalDate departureDate = LocalDate.now().plusDays(10);
        LocalDate returnDate = LocalDate.now().plusDays(17);
        flightsSearchPage.selectDepartureDate(departureDate);
        flightsSearchPage.selectRoundTripModeInCalendar();
        flightsSearchPage.selectReturnDate(returnDate);

        String originalWindowHandle = driver.getWindowHandle();
        Set<String> originalHandles = driver.getWindowHandles();

        flightsSearchPage.clickSearchButton();

        switchToNewTab(originalWindowHandle, originalHandles);

        Assert.assertTrue(flightsSearchPage.isOnResultsPage(), "Should be on search results page for round trip in the new tab.");
        String currentUrl = flightsSearchPage.getCurrentUrl();
        String formattedDepartureDate = departureDate.format(DateTimeFormatter.ofPattern("MM-dd"));
        String formattedReturnDate = returnDate.format(DateTimeFormatter.ofPattern("MM-dd"));
        Assert.assertTrue(currentUrl.contains("/round-trip/"), "URL should contain '/round-trip/'.");
        Assert.assertTrue(currentUrl.contains("/MOW-AER/"), "URL should contain departure-arrival codes like '/MOW-AER/'.");
        Assert.assertTrue(currentUrl.matches(".*/flights/round-trip/[A-Z]{3}-[A-Z]{3}/" + formattedDepartureDate + "/" + formattedReturnDate + "/\\?.*"),
                "URL structure for round trip is incorrect. Expected format like .../round-trip/MOW-AER/MM-dd/MM-dd/?...");
    }

    // ... (остальные тесты TC3, TC4, TC5 - там нет клика "Найти", поэтому переключения не нужно) ...
    @Test(description = "TC3: Попытка поиска без указания города отправления")
    public void testSearchWithoutDepartureCity() {
        flightsSearchPage.enterArrivalCity(ARRIVAL_CITY_SPB_FULL, ARRIVAL_CITY_SPB_SEARCH);
        flightsSearchPage.selectDepartureDate(LocalDate.now().plusDays(5));
        flightsSearchPage.ensureCalendarClosed();
        Assert.assertFalse(flightsSearchPage.isSearchButtonEnabled(), "Search button should be disabled without departure city.");
    }

    @Test(description = "TC4: Попытка поиска без указания города назначения")
    public void testSearchWithoutArrivalCity() {
        flightsSearchPage.enterDepartureCity(DEPARTURE_CITY_FULL, DEPARTURE_CITY_SEARCH);
        flightsSearchPage.selectDepartureDate(LocalDate.now().plusDays(5));
        flightsSearchPage.ensureCalendarClosed();
        Assert.assertFalse(flightsSearchPage.isSearchButtonEnabled(), "Search button should be disabled without arrival city.");
    }

    @Test(description = "TC5: Попытка поиска с одинаковыми городами отправления и назначения")
    public void testSearchWithSameDepartureAndArrivalCity() {
        flightsSearchPage.enterDepartureCity(DEPARTURE_CITY_FULL, DEPARTURE_CITY_SEARCH);
        flightsSearchPage.enterArrivalCity(DEPARTURE_CITY_FULL, DEPARTURE_CITY_SEARCH);

        try {
            flightsSearchPage.selectDepartureDate(LocalDate.now().plusDays(12));
            flightsSearchPage.ensureCalendarClosed();
        } catch (Exception e) {
            System.out.println("Date selection might fail if city input error occurred, which is expected.");
        }

        Assert.assertFalse(flightsSearchPage.isSearchButtonEnabled(), "Search button should be disabled if cities are identical.");

        String fromFieldError = flightsSearchPage.getFromFieldError();
        WebElement fromInput = driver.findElement(By.xpath("//input[@aria-labelledby='Input_Откуда']")); // Используем XPath напрямую, как в PageObject
        String fromFieldValue = fromInput.getAttribute("value");
        boolean isFromFieldEmptyOrShowsError = fromFieldValue.isEmpty() || fromFieldError != null;
        Assert.assertTrue(isFromFieldEmptyOrShowsError, "Departure city field should be empty or show an error after selecting identical arrival city: current value '" + fromFieldValue + "', error: '" + fromFieldError + "'");
    }
}
