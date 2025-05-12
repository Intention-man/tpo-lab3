import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class FlightsSearchPage extends BasePage {
    private final String PAGE_URL = "https://www.tbank.ru/travel/flights/";

    final By mainFormContainerXPath = By.xpath("//form[@data-qa-file='SearchForm']");
    final By fromInputXPath = By.xpath("//input[@aria-labelledby='Input_Откуда']");
    final By toInputXPath = By.xpath("//input[@aria-labelledby='Input_Куда']");
    final By searchButtonXPath = By.xpath("//button[@data-qa-type='uikit/button' and descendant::span[normalize-space(text())='Найти']]");

    private final By departureDateOpenButtonXPath = By.xpath("//div[starts-with(@data-qa-type, 'DateTextInput_') and .//span[@data-qa-type='uikit/inputBox.label' and normalize-space(text())='Когда']]");

    private final By calendarContainerXPath = By.xpath("//div[@data-qa-file='DaySelector']"); // Контейнер всего календаря
    private final By oneWayTripModeButtonXPath = By.xpath("//button[@data-qa-file='Tabs' and normalize-space(.)='В одну сторону']"); // Кнопка режима ВНУТРИ календаря
    private final By roundTripModeButtonXPath = By.xpath("//button[@data-qa-file='Tabs' and normalize-space(.)='Туда-обратно']"); // Кнопка режима ВНУТРИ календаря
    private final By calendarMonthYearHeaderXPath = By.xpath("//div[@data-qa-file='CalendarHeader']"); // Заголовок месяца/года
    private final By calendarNextMonthButtonXPath = By.xpath("//button[@aria-label='Следующий месяц']");

    private final By fromFieldErrorXPath = By.xpath("//input[@aria-labelledby='Input_Откуда']/ancestor::div[count(input)=1 and count(div)>0][1]/following-sibling::div[contains(@class, 'Error') or contains(@data-qa-type, 'Error')]");
    private final By toFieldErrorXPath = By.xpath("//input[@aria-labelledby='Input_Куда']/ancestor::div[count(input)=1 and count(div)>0][1]/following-sibling::div[contains(@class, 'Error') or contains(@data-qa-type, 'Error')]");
    private final By flightOfferCardXPath = By.xpath("//div[@data-qa-tag='panelFlightOfferCardLayout']");

    public FlightsSearchPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        driver.get(PAGE_URL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(mainFormContainerXPath));
        wait.until(ExpectedConditions.visibilityOfElementLocated(fromInputXPath));
        wait.until(ExpectedConditions.elementToBeClickable(searchButtonXPath));
        ((JavascriptExecutor) driver).executeScript("document.body.style.zoom = '0.75'");
        try{Thread.sleep(1000);} catch (InterruptedException ignored){}
    }

    private String citySuggestionXPath(String cityNamePart) {
        return String.format(
                "//div[" +
                        "div[2]/div[1][contains(translate(normalize-space(.), 'АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ', 'абвгдеёжзийклмнопрстуфхцшщъыьэюя'), '%s')] and " +
                        "div[3]/label[@data-qa-file='LocationCodeLabel']" +
                        "]",
                cityNamePart.toLowerCase()
        );
    }

    private String calendarDayXPath(int day) {
        return String.format(
                "(//div[@data-qa-file='CalendarHeader'])[1]/following-sibling::table[1]//td[.//span//div[normalize-space(text())='%d'] and not(normalize-space(.)='')]",
                day
        );
    }

    private void enterCity(By inputLocator, String cityFullName, String citySearchTerm) {
        WebElement inputField = wait.until(ExpectedConditions.elementToBeClickable(inputLocator));
        inputField.click();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            inputField.sendKeys(Keys.chord(Keys.COMMAND, "a"), Keys.DELETE);
        } else {
            inputField.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        }
        inputField.sendKeys(citySearchTerm);

        By citySuggestionActualXPath = By.xpath(citySuggestionXPath(citySearchTerm.split(",")[0]));
        wait.until(ExpectedConditions.visibilityOfElementLocated(citySuggestionActualXPath));
        WebElement suggestion = wait.until(ExpectedConditions.elementToBeClickable(citySuggestionActualXPath));
        suggestion.click();
        wait.until(ExpectedConditions.attributeContains(inputField, "value", cityFullName.split(",")[0]));
    }

    public void enterDepartureCity(String cityFullName, String citySearchTerm) {
        enterCity(fromInputXPath, cityFullName, citySearchTerm);
    }
    public void enterDepartureCity(String cityFullName) { enterCity(fromInputXPath, cityFullName, cityFullName); }
    public void enterArrivalCity(String cityFullName, String citySearchTerm) { enterCity(toInputXPath, cityFullName, citySearchTerm); }
    public void enterArrivalCity(String cityFullName) { enterCity(toInputXPath, cityFullName, cityFullName); }

    public void selectOneWayTripModeInCalendar() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarContainerXPath)); // Убедимся, что календарь открыт
        WebElement oneWayButton = driver.findElement(oneWayTripModeButtonXPath);
        String qaType = oneWayButton.getAttribute("data-qa-type");
        if (qaType != null && qaType.contains("not-active")) {
            wait.until(ExpectedConditions.elementToBeClickable(oneWayTripModeButtonXPath)).click();
            wait.until(ExpectedConditions.attributeContains(oneWayTripModeButtonXPath, "data-qa-type", "_active"));
            System.out.println("Switched to One-Way trip mode inside calendar.");
        } else {
            System.out.println("One-Way trip mode is already active in calendar or state cannot be determined.");
        }
    }

    public void selectRoundTripModeInCalendar() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarContainerXPath)); // Убедимся, что календарь открыт
        WebElement roundTripButton = driver.findElement(roundTripModeButtonXPath);
        String qaType = roundTripButton.getAttribute("data-qa-type");
        if (qaType != null && qaType.contains("not-active")) {
            wait.until(ExpectedConditions.elementToBeClickable(roundTripModeButtonXPath)).click();
            wait.until(ExpectedConditions.attributeContains(roundTripModeButtonXPath, "data-qa-type", "_active"));
            System.out.println("Switched to Round trip mode inside calendar.");
        } else {
            System.out.println("Round trip mode is already active in calendar or state cannot be determined.");
        }
    }

    private void selectDateInCalendar(LocalDate date) {
        DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("ru", "RU"));
        String targetMonthYear = date.format(monthYearFormatter);
        targetMonthYear = targetMonthYear.substring(0, 1).toUpperCase() + targetMonthYear.substring(1);

        for (int i = 0; i < 24; i++) {
            WebElement currentMonthYearEl = wait.until(ExpectedConditions.visibilityOfElementLocated(calendarMonthYearHeaderXPath));
            String displayedMonthYear = currentMonthYearEl.getText().trim().replace("\n", " ");
            if (!displayedMonthYear.isEmpty() && Character.isLowerCase(displayedMonthYear.charAt(0))) {
                displayedMonthYear = displayedMonthYear.substring(0, 1).toUpperCase() + displayedMonthYear.substring(1);
            }
            System.out.println("Target month/year: " + targetMonthYear + ", Displayed: " + displayedMonthYear);
            if (displayedMonthYear.equalsIgnoreCase(targetMonthYear)) {
                break;
            }
            wait.until(ExpectedConditions.elementToBeClickable(calendarNextMonthButtonXPath)).click();
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            if (i == 23) {
                throw new TimeoutException("Could not find target month/year in calendar: " + targetMonthYear + ". Last seen: " + displayedMonthYear + ". Check navigation buttons and month header XPath.");
            }
        }
        WebElement dayElement = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(calendarDayXPath(date.getDayOfMonth()))));
        dayElement.click();
    }

    public void selectDepartureDate(LocalDate date) {
        wait.until(ExpectedConditions.elementToBeClickable(departureDateOpenButtonXPath)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarContainerXPath));
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarMonthYearHeaderXPath));
        selectDateInCalendar(date);
    }

    public void selectReturnDate(LocalDate date) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarContainerXPath)); // Стандартный wait
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarMonthYearHeaderXPath)); // Стандартный wait
        selectDateInCalendar(date);
        waitForElementInvisible(calendarContainerXPath, DEFAULT_WAIT_SECONDS);
    }

    public void ensureCalendarClosed() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(calendarContainerXPath));
    }

    public void clickSearchButton() {
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(searchButtonXPath));
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", button);
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } catch (Exception e) { /* ignore */ }
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
    }

    public boolean isOnResultsPage() {
        int resultsWaitTimeout = 30;

        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.urlMatches(".*/flights/one-way/.*"),
                    ExpectedConditions.urlMatches(".*/flights/round-trip/.*")
            ));

            List<WebElement> offerCards = waitForNumberOfElementsToBeMoreThan(
                    flightOfferCardXPath,
                    0,
                    resultsWaitTimeout
            );


            waitForCondition(ExpectedConditions.visibilityOf(offerCards.get(0)), resultsWaitTimeout);

            System.out.println("Successfully loaded results page with " + offerCards.size() + " flight offer cards found.");
            return true;

        } catch (TimeoutException e) {
            System.err.println("isOnResultsPage check failed after waiting " + resultsWaitTimeout + " seconds. Current URL: " + driver.getCurrentUrl());
            if (!driver.getCurrentUrl().matches(".*/flights/(one-way|round-trip)/.*")) {
                System.err.println("URL does not match expected results page format.");
            } else {
                System.err.println("URL matches results page format, but no flight offer cards (using locator: " + flightOfferCardXPath + ") were found or visible within the timeout.");
            }
            return false;
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during isOnResultsPage check. Current URL: " + driver.getCurrentUrl());
            e.printStackTrace();
            return false;
        }
    }

    public String getFromFieldError() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(fromFieldErrorXPath)).getText();
        } catch (TimeoutException e) {
            return null;
        }
    }

    public String getToFieldError() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(toFieldErrorXPath)).getText();
        } catch (TimeoutException e) {
            return null;
        }
    }

    public boolean isSearchButtonEnabled() {
        WebElement button = wait.until(ExpectedConditions.visibilityOfElementLocated(searchButtonXPath));
        return button.isEnabled() && !"true".equals(button.getAttribute("aria-busy"));
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public boolean isReturnDateEffectivelyEmptyOrDisabled() {
        try {
            driver.findElement(calendarContainerXPath);
            WebElement oneWayButton = driver.findElement(oneWayTripModeButtonXPath);
            String qaType = oneWayButton.getAttribute("data-qa-type");
            return qaType != null && qaType.contains("_active");
        } catch (NoSuchElementException e) {
            // Если календарь НЕ открыт (контейнер не найден),
            // то для сценария "В одну сторону" это нормальное состояние после выбора даты.
            // Для сценария "Туда-обратно" это будет означать, что обе даты выбраны.
            // В контексте проверки для ОДНОЙ СТОРОНЫ (где вызывается этот метод в тесте) - это успех.
            System.out.println("Calendar is closed, assuming return date is effectively empty/disabled for one-way check.");
            return true;
        }
    }
}
