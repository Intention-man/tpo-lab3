import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FlightsSearchPage extends BasePage {
    public static final String ONE_WAY_PATH_SEGMENT = "/one-way/";
    public static final String MULTI_WAY_PATH_SEGMENT = "/multi-way/";

    final By mainFormContainerXPath = By.xpath("//form[@data-qa-file='SearchForm']");
    final By fromInputXPath = By.xpath("//input[@aria-labelledby='Input_Откуда']");
    final By toInputXPath = By.xpath("//input[@aria-labelledby='Input_Куда']");
    final By searchButtonXPath = By.xpath("//button[@data-qa-type='uikit/button' and descendant::span[normalize-space(text())='Найти']]");

    private final String PAGE_URL = "https://www.tbank.ru/travel/flights/";
    private final By pageHeaderForUnfocusClickXPath = By.xpath("//h1[@data-test='htmlTag title']");
    private final By departureDateOpenButtonXPath = By.xpath("//div[starts-with(@data-qa-type, 'DateTextInput_') and .//span[@data-qa-type='uikit/inputBox.label' and normalize-space(text())='Когда']]");

    private final By oneWayTripModeButtonXPath = By.xpath("//button[@data-qa-file='Tabs' and normalize-space(.)='В одну сторону']"); // Кнопка режима ВНУТРИ календаря
    private final By roundTripModeButtonXPath = By.xpath("//button[@data-qa-file='Tabs' and normalize-space(.)='Туда-обратно']");
    private final By complexRouteButtonXPath = By.xpath("//button[@data-qa-file='SwitchRouteButton' and .//span[normalize-space(text())='Сложный маршрут']]");

    private final By calendarContainerXPath = By.xpath("//div[@data-qa-file='DaySelector']");
    private final By calendarMonthYearHeaderXPath = By.xpath("//div[@data-qa-file='CalendarHeader']"); // Заголовок месяца/года
    private final By calendarNextMonthButtonXPath = By.xpath("//span[@role='button' and @aria-label='Вперед' and @data-qa-file='DateSwiper' and @aria-disabled='false']");

    private final By fromFieldErrorXPath = By.xpath("//input[@aria-labelledby='Input_Откуда']/ancestor::div[count(input)=1 and count(div)>0][1]/following-sibling::div[contains(@class, 'Error') or contains(@data-qa-type, 'Error')]");
    private final By toFieldErrorXPath = By.xpath("//input[@aria-labelledby='Input_Куда']/ancestor::div[count(input)=1 and count(div)>0][1]/following-sibling::div[contains(@class, 'Error') or contains(@data-qa-type, 'Error')]");
    private final By flightOfferCardXPath = By.xpath("//div[@data-qa-tag='panelFlightOfferCardLayout']");

    private final String ROUTE_SEGMENT_CONTAINER_BASE_XPATH = "//div[@data-qa-tag='travelSearchFormRoute']";

    public FlightsSearchPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        driver.get(PAGE_URL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(mainFormContainerXPath));
        wait.until(ExpectedConditions.visibilityOfElementLocated(fromInputXPath));
        wait.until(ExpectedConditions.elementToBeClickable(searchButtonXPath));
        ((JavascriptExecutor) driver).executeScript("document.body.style.zoom = '0.75'");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
    }

    private By getFromInputXPathForSegment(int segmentIndex) {
        if (segmentIndex < 1) throw new IllegalArgumentException("Segment index must be 1 or greater.");
        return By.xpath(String.format("(%s)[%d]//input[@aria-labelledby='Input_Откуда']", ROUTE_SEGMENT_CONTAINER_BASE_XPATH, segmentIndex));
    }
    private By getToInputXPathForSegment(int segmentIndex) {
        if (segmentIndex < 1) throw new IllegalArgumentException("Segment index must be 1 or greater.");
        return By.xpath(String.format("(%s)[%d]//input[@aria-labelledby='Input_Куда']", ROUTE_SEGMENT_CONTAINER_BASE_XPATH, segmentIndex));
    }
    private By getDepartureDateOpenButtonXPathForSegment(int segmentIndex) {
        if (segmentIndex < 1) throw new IllegalArgumentException("Segment index must be 1 or greater.");
        return By.xpath(String.format("(%s)[%d]//div[starts-with(@data-qa-type, 'DateTextInput_') and .//span[normalize-space(text())='Когда']]", ROUTE_SEGMENT_CONTAINER_BASE_XPATH, segmentIndex));
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
        WebElement inputField;
        try {
            inputField = wait.until(ExpectedConditions.presenceOfElementLocated(inputLocator));

            boolean isProblematicScenario = false;
            if (inputLocator.equals(toInputXPath)) {
                try {
                    String fromValue = driver.findElement(fromInputXPath).getAttribute("value");
                    if (fromValue == null || fromValue.isEmpty()) {
                        isProblematicScenario = true;
                    }
                } catch (Exception ignored) {
                }
            }

            if (isProblematicScenario) {
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", inputField);
                    Thread.sleep(100);
                } catch (Exception ignored) {
                }

                try {
                    new Actions(driver).moveToElement(inputField).click().perform();
                } catch (Exception e1) {
                    try {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", inputField);
                    } catch (Exception e2) {
                        System.err.println("CRITICAL: Failed to click input field " + inputLocator + " even with Actions and JS. Error: " + e2.getMessage());
                    }
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } // Пауза после клика
            } else {
                wait.until(ExpectedConditions.elementToBeClickable(inputField));
                inputField.click();
            }

            clearInputField(inputField);
            inputField.sendKeys(citySearchTerm);

        } catch (TimeoutException e) {
            System.err.println("TimeoutException during enterCity for " + inputLocator + ". FullName: " + cityFullName + ". SearchTerm: " + citySearchTerm);
            throw e;
        } catch (Exception e) {
            System.err.println("Unexpected Exception during enterCity for " + inputLocator + ". FullName: " + cityFullName + ". SearchTerm: " + citySearchTerm);
            e.printStackTrace();
            throw e;
        }

        By citySuggestionActualXPath = By.xpath(citySuggestionXPath(citySearchTerm.split(",")[0]));
        wait.until(ExpectedConditions.visibilityOfElementLocated(citySuggestionActualXPath));
        WebElement suggestion = wait.until(ExpectedConditions.elementToBeClickable(citySuggestionActualXPath));
        suggestion.click();

        wait.until(ExpectedConditions.attributeContains(inputField, "value", cityFullName.split(",")[0]));
    }

    private void enterCity(By inputLocator, String cityFullName, String citySearchTerm, int segmentIndexForProblemCheck) {
        WebElement inputField = waitForElementPresent(inputLocator, DEFAULT_WAIT_SECONDS);

        boolean isProblematicScenario = false;
        if (inputLocator.equals(getToInputXPathForSegment(segmentIndexForProblemCheck))) {
            try {
                WebElement fromFieldForThisSegment = driver.findElement(getFromInputXPathForSegment(segmentIndexForProblemCheck));
                String fromValue = fromFieldForThisSegment.getAttribute("value");
                if (fromValue == null || fromValue.isEmpty()) {
                    isProblematicScenario = true;
                }
            } catch (NoSuchElementException ignored) {}
        }

        if (isProblematicScenario) {
            System.out.println("    Applying JS click for problematic scenario " + inputLocator);
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true); arguments[0].click();", inputField);
                Thread.sleep(200); // Пауза после JS клика
            } catch (Exception e) {
                System.err.println("    JS click failed for " + inputLocator + ". Error: " + e.getMessage());
                // Если JS клик не прошел, возможно, элемент действительно недоступен
                // Можно попробовать стандартный клик как fallback, но это маловероятно поможет, если JS не смог
                // waitForElementClickable(inputField, 5).click(); // Короткая попытка стандартного
            }
        } else {
            waitForElementClickable(inputField, DEFAULT_WAIT_SECONDS).click();
        }

        clearInputField(inputField); // Используем новый, упрощенный clearInputField

        // Прямой ввод текста, если sendKeys надежен после активации/очистки
        inputField.sendKeys(citySearchTerm);
        String valueAfterSendKeys = inputField.getAttribute("value");
        System.out.println("    Value after sendKeys for " + inputLocator + ": '" + valueAfterSendKeys + "'");
        if (!valueAfterSendKeys.toLowerCase().contains(citySearchTerm.toLowerCase().split(",")[0])) {
            System.err.println("    WARNING: sendKeys for " + inputLocator + " might not have worked. Re-attempting with JS value set.");
            ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", inputField, citySearchTerm);
            ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", inputField);
            ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", inputField);
            System.out.println("    Value after JS value set for " + inputLocator + ": '" + inputField.getAttribute("value") + "'");
        }

        // Обработка выпадающего списка (оставляем как есть, с коротким ожиданием)
        By citySuggestionActualXPath = By.xpath(citySuggestionXPath(citySearchTerm.split(",")[0]));
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(7));
            WebElement suggestion = shortWait.until(ExpectedConditions.elementToBeClickable(citySuggestionActualXPath));
            suggestion.click();
            WebElement finalInputField = waitForElementPresent(inputLocator, DEFAULT_WAIT_SECONDS); // Перенаходим
            wait.until(ExpectedConditions.attributeContains(finalInputField, "value", cityFullName.split(",")[0]));
        } catch (TimeoutException e) {
            System.out.println("    Suggestion list for '" + citySearchTerm + "' did not appear/was not clickable in 7s.");
            // Проверяем, что значение осталось корректным после прямого ввода
            try {
                WebElement finalInputField = waitForElementPresent(inputLocator, DEFAULT_WAIT_SECONDS);
                String finalValue = finalInputField.getAttribute("value");
                String expectedValueSegment = cityFullName.split(",")[0];
                String searchTermSegment = citySearchTerm.split(",")[0];
                if (!finalValue.toLowerCase().contains(expectedValueSegment.toLowerCase()) &&
                        !finalValue.toLowerCase().contains(searchTermSegment.toLowerCase())) {
                    System.err.println("    WARN: Final value in input " + inputLocator + " is '" + finalValue +
                            "' but expected something containing '" + expectedValueSegment + "' or '" + searchTermSegment + "'.");
                }
            } catch (Exception valEx) {
                System.err.println("    Error checking final input field value: " + valEx.getMessage());
            }
        } catch (StaleElementReferenceException ignored) {}
    }


    public void enterDepartureCityForSegment(int segmentIndex, String cityFullName, String citySearchTerm) {
        enterCity(getFromInputXPathForSegment(segmentIndex), cityFullName, citySearchTerm, segmentIndex);
    }
    public void enterDepartureCityForSegment(int segmentIndex, String cityFullName) {
        enterCity(getFromInputXPathForSegment(segmentIndex), cityFullName, cityFullName, segmentIndex);
    }
    public void enterArrivalCityForSegment(int segmentIndex, String cityFullName, String citySearchTerm) {
        enterCity(getToInputXPathForSegment(segmentIndex), cityFullName, citySearchTerm, segmentIndex);
    }
    public void enterArrivalCityForSegment(int segmentIndex, String cityFullName) {
        enterCity(getToInputXPathForSegment(segmentIndex), cityFullName, cityFullName, segmentIndex);
    }

    public void enterDepartureCity(String cityFullName, String citySearchTerm) {
        enterCity(fromInputXPath, cityFullName, citySearchTerm);
    }

    public void enterArrivalCity(String cityFullName, String citySearchTerm) {
        enterCity(toInputXPath, cityFullName, citySearchTerm);
    }

    private void clearInputField(WebElement inputField) {
        final int MAX_JS_CLEAR_ATTEMPTS = 3;
        final int JS_RETRY_DELAY_MS = 200;

        try {
            // 0. Убедимся, что элемент видим и активен перед попытками
            if (!inputField.isDisplayed() || !inputField.isEnabled()) {
                System.err.println("Input field for clearing is not displayed or not enabled. Locator: " + inputField.toString());
                // Попробовать прокрутить и подождать кликабельности, если не видим/не активен
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", inputField);
                    new WebDriverWait(driver, Duration.ofSeconds(2)).until(ExpectedConditions.elementToBeClickable(inputField));
                } catch (Exception e) {
                    System.err.println("Failed to make field visible/clickable before clearing: " + e.getMessage());
                    // Если даже после этого не доступен, JS может не сработать
                }
            }

            // 1. Попытка стандартного element.clear() - иногда он лучше всего работает с обработчиками событий
            try {
                inputField.clear();
                if (inputField.getAttribute("value").isEmpty()) {
                    System.out.println("Input field cleared with element.clear().");
                    return;
                }
            } catch (Exception e) {
                System.err.println("element.clear() failed: " + e.getMessage());
            }

            // 2. Попытка выделить все и удалить (Ctrl/Cmd+A -> DELETE)
            // Этот метод может быть менее надежен, если JS активно мешает
            try {
                // Клик для фокуса
                inputField.click();
                Thread.sleep(50); // Короткая пауза для фокуса
                String os = System.getProperty("os.name").toLowerCase();
                Keys commandOrControl = os.contains("mac") ? Keys.COMMAND : Keys.CONTROL;
                inputField.sendKeys(Keys.chord(commandOrControl, "a"));
                inputField.sendKeys(Keys.DELETE);
                Thread.sleep(50); // Пауза для обработки
                if (inputField.getAttribute("value").isEmpty()) {
                    System.out.println("Input field cleared with Ctrl/Cmd+A -> DELETE.");
                    return;
                }
            } catch (Exception e) {
                System.err.println("sendKeys clear (Ctrl/Cmd+A -> DELETE) failed: " + e.getMessage());
            }


            // 3. Агрессивная очистка через JavaScript с несколькими попытками
            // Это самый надежный способ изменить DOM, но он может не триггерить некоторые JS-события на странице.
            System.out.println("Standard clear methods failed or skipped. Attempting JS clear. Initial value: '" + inputField.getAttribute("value") + "'");
            for (int i = 0; i < MAX_JS_CLEAR_ATTEMPTS; i++) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", inputField);
                // Диспетчеризуем события, чтобы попытаться уведомить фреймворк об изменении
                ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", inputField);
                ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", inputField);

                // Проверяем значение после небольшой паузы
                try { Thread.sleep(JS_RETRY_DELAY_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

                String currentValue = inputField.getAttribute("value");
                if (currentValue.isEmpty()) {
                    System.out.println("Input field cleared with JS after " + (i + 1) + " attempt(s).");
                    return;
                }
                System.out.println("JS clear attempt " + (i + 1) + " did not result in empty field. Value: '" + currentValue + "'");
            }

            // Если мы дошли сюда, значит, все попытки провалились
            System.err.println("CRITICAL: All clear methods failed for input field. Final value: '" + inputField.getAttribute("value") + "'. Locator: " + inputField.toString());
            // Возможно, стоит выбросить исключение, чтобы тест явно упал на этом этапе
            // throw new RuntimeException("Failed to clear input field: " + inputField.toString());

        } catch (Exception e) { // Общий try-catch для всего метода clearInputField
            System.err.println("CRITICAL: Unexpected exception in clearInputField for locator: " + (inputField != null ? inputField.toString() : "UNKNOWN") + ". Error: " + e.getMessage());
            // e.printStackTrace(); // Можно добавить для полной диагностики
        }
    }

    public void clearDepartureCity() {
        WebElement fromField = wait.until(ExpectedConditions.elementToBeClickable(fromInputXPath));
        fromField.click();
        clearInputField(fromField);
        System.out.println("Departure city field cleared.");
        clickOutsideToUnfocus();
    }

    public void clearDepartureCityForSegment(int segmentIndex) {
        WebElement fromField = waitForElementClickable(getFromInputXPathForSegment(segmentIndex), DEFAULT_WAIT_SECONDS);
        fromField.click();
        clearInputField(fromField);
        clickOutsideToUnfocus();
    }
    public void clearArrivalCityForSegment(int segmentIndex) {
        WebElement toField = waitForElementClickable(getToInputXPathForSegment(segmentIndex), DEFAULT_WAIT_SECONDS);
        toField.click();
        clearInputField(toField);
        clickOutsideToUnfocus();
    }

    void clickOutsideToUnfocus() {
        try {
            WebElement elementToClick = waitForElementPresent(pageHeaderForUnfocusClickXPath, 5);
            elementToClick.click();
            elementToClick.click();
            elementToClick.click();
            System.out.println("Clicked on element (" + pageHeaderForUnfocusClickXPath + ") to unfocus.");
        } catch (Exception e) {
            System.err.println("Warning: Could not click on element " + pageHeaderForUnfocusClickXPath + " to unfocus. " + e.getMessage());
        }
    }

    public void openCalendar() {
        System.out.println("Attempting to open calendar...");
        wait.until(ExpectedConditions.elementToBeClickable(departureDateOpenButtonXPath)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarContainerXPath));
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarMonthYearHeaderXPath));
        System.out.println("Calendar opened successfully.");
    }

    public void openCalendarForSegment(int segmentIndex) {
        WebElement dateButton = waitForElementClickable(getDepartureDateOpenButtonXPathForSegment(segmentIndex), DEFAULT_WAIT_SECONDS);
        dateButton.click();
        waitForElementVisible(calendarContainerXPath, DEFAULT_WAIT_SECONDS);
        waitForElementVisible(calendarMonthYearHeaderXPath, DEFAULT_WAIT_SECONDS);
    }

    public void selectOneWayTripModeInCalendar() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarContainerXPath));
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
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarContainerXPath));
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

    public void selectDateForSegment(int segmentIndex, LocalDate date) {
        openCalendarForSegment(segmentIndex);
        selectDateInCalendar(date);
        ensureCalendarClosed();
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
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (i == 23) {
                throw new TimeoutException("Could not find target month/year in calendar: " + targetMonthYear + ". Last seen: " + displayedMonthYear + ". Check navigation buttons and month header XPath.");
            }
        }
        WebElement dayElement = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(calendarDayXPath(date.getDayOfMonth()))));
        dayElement.click();
    }

    public void selectDepartureDate(LocalDate date) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarContainerXPath));
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarMonthYearHeaderXPath));
        selectDateInCalendar(date);
    }

    public void selectReturnDate(LocalDate date) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarContainerXPath));
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarMonthYearHeaderXPath));
        selectDateInCalendar(date);
        waitForElementInvisible(calendarContainerXPath, DEFAULT_WAIT_SECONDS);
    }

    public void ensureCalendarClosed() {
        waitForElementInvisible(calendarContainerXPath, DEFAULT_WAIT_SECONDS);
        System.out.println("Calendar confirmed to be closed.");
    }

    public void switchToComplexRouteMode() {
        waitForElementClickable(complexRouteButtonXPath, DEFAULT_WAIT_SECONDS).click();

        List<WebElement> segments = waitForNumberOfElementsToBeMoreThan(
                By.xpath(ROUTE_SEGMENT_CONTAINER_BASE_XPATH),
                1,
                DEFAULT_WAIT_SECONDS
        );

        if (segments.size() >= 2) {
            waitForCondition(ExpectedConditions.visibilityOf(segments.get(1)), DEFAULT_WAIT_SECONDS);
        } else {
            System.err.println("switchToComplexRouteMode: Expected at least 2 segments after switching, but found " + segments.size() +
                    ". NumberOfElementsToBeMoreThan might not have waited correctly or DOM changed.");
        }
    }

    public void clickSearchButton() {
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(searchButtonXPath));
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", button);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) { /* ignore */ }
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
    }

    public boolean isOnResultsPage() {
        int resultsWaitTimeout = 30;

        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.urlMatches(".*" + ONE_WAY_PATH_SEGMENT + ".*"),
                    ExpectedConditions.urlMatches(".*" + MULTI_WAY_PATH_SEGMENT + ".*")
            ));

            List<WebElement> offerCards = waitForNumberOfElementsToBeMoreThan(
                    flightOfferCardXPath,
                    0,
                    resultsWaitTimeout
            );

            if (!offerCards.isEmpty()) {
                waitForCondition(
                        ExpectedConditions.visibilityOf(offerCards.get(0)),
                        resultsWaitTimeout
                );
            } else {
                System.err.println("isOnResultsPage: waitForNumberOfElementsToBeMoreThan returned an empty list, cannot check visibility.");
                return false;
            }

            System.out.println("Successfully loaded results page with " + offerCards.size() + " flight offer cards found.");
            return true;

        } catch (TimeoutException e) {
            System.err.println("isOnResultsPage check failed after waiting " + resultsWaitTimeout + " seconds. Current URL: " + driver.getCurrentUrl());
            if (!driver.getCurrentUrl().matches(".*/flights/(" + ONE_WAY_PATH_SEGMENT.replace("/", "") + "|" + MULTI_WAY_PATH_SEGMENT.replace("/", "") + ")/.*")) {
                System.err.println("URL does not match expected results page format (one-way or multi-way).");
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


    public boolean isSearchButtonFunctionallyEnabled() {
        WebElement button = wait.until(ExpectedConditions.visibilityOfElementLocated(searchButtonXPath));

        if (!button.isEnabled() || "true".equals(button.getAttribute("aria-busy"))) {
            return false;
        }

        String urlBeforeClick = driver.getCurrentUrl();
        Set<String> handlesBeforeClick = driver.getWindowHandles();
        String originalWindowHandle = driver.getWindowHandle();

        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
        } catch (Exception e) {
            System.err.println("Error clicking search button during functional check: " + e.getMessage());
            return false;
        }

        try {
            Thread.sleep(700);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Set<String> handlesAfterClick = driver.getWindowHandles();
        String urlAfterClick = driver.getCurrentUrl();

        boolean newTabOpened = handlesAfterClick.size() > handlesBeforeClick.size();
        boolean urlChanged = !urlAfterClick.equals(urlBeforeClick);

        if (newTabOpened) {
            for (String handle : handlesAfterClick) {
                if (!handlesBeforeClick.contains(handle)) {
                    driver.switchTo().window(handle);
                    driver.close();
                    break;
                }
            }
            driver.switchTo().window(originalWindowHandle);
            return true;
        }

        return urlChanged;
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
            System.out.println("Calendar is closed, assuming return date is effectively empty/disabled for one-way check.");
            return true;
        }
    }
}
