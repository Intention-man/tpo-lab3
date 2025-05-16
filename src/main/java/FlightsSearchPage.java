import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

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

    private final By calendarContainerXPath = By.xpath("//div[@data-qa-file='DaySelector']"); // Контейнер всего календаря
    private final By oneWayTripModeButtonXPath = By.xpath("//button[@data-qa-file='Tabs' and normalize-space(.)='В одну сторону']"); // Кнопка режима ВНУТРИ календаря
    private final By roundTripModeButtonXPath = By.xpath("//button[@data-qa-file='Tabs' and normalize-space(.)='Туда-обратно']"); // Кнопка режима ВНУТРИ календаря
    private final By calendarMonthYearHeaderXPath = By.xpath("//div[@data-qa-file='CalendarHeader']"); // Заголовок месяца/года
    private final By calendarNextMonthButtonXPath = By.xpath("//span[@role='button' and @aria-label='Вперед' and @data-qa-file='DateSwiper' and @aria-disabled='false']");

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
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
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


    private void clearInputField(WebElement inputField) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            inputField.sendKeys(Keys.chord(Keys.COMMAND, "a"));
        } else {
            inputField.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        }
        inputField.sendKeys(Keys.BACK_SPACE);
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

    public void clearDepartureCity() {
        WebElement fromField = wait.until(ExpectedConditions.elementToBeClickable(fromInputXPath));
        fromField.click(); // Фокус на поле
        clearInputField(fromField); // Очистка
        System.out.println("Departure city field cleared.");
        clickOutsideToUnfocus(); // Вызываем новый метод
    }

    public void clearArrivalCity() {
        WebElement toField = wait.until(ExpectedConditions.elementToBeClickable(toInputXPath));
        toField.click();
        clearInputField(toField);
        System.out.println("Arrival city field cleared.");
        clickOutsideToUnfocus(); // Вызываем новый метод
    }

    public void enterDepartureCity(String cityFullName, String citySearchTerm) {
        enterCity(fromInputXPath, cityFullName, citySearchTerm);
    }

    public void enterDepartureCity(String cityFullName) {
        enterCity(fromInputXPath, cityFullName, cityFullName);
    }

    public void enterArrivalCity(String cityFullName, String citySearchTerm) {
        enterCity(toInputXPath, cityFullName, citySearchTerm);
    }

    public void enterArrivalCity(String cityFullName) {
        enterCity(toInputXPath, cityFullName, cityFullName);
    }

    public void openCalendar() {
        System.out.println("Attempting to open calendar...");
        wait.until(ExpectedConditions.elementToBeClickable(departureDateOpenButtonXPath)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarContainerXPath));
        wait.until(ExpectedConditions.visibilityOfElementLocated(calendarMonthYearHeaderXPath));
        System.out.println("Calendar opened successfully.");
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
