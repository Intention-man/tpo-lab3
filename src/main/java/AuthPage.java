import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class AuthPage extends BasePage {

    final By resendCodeButtonXPath = By.xpath("//div[contains(@class, '_Content_') and text()='Отправить еще раз']");
    private final By personalCabinetLinkXPath = By.xpath("//span[normalize-space(text())='Личный кабинет']");
    //    private final By internetBankButtonXPath = By.xpath("//a[.//span[normalize-space(text())='Интернет-банк']]");
    private final By internetBankButtonXPath = By.xpath("//li[.//a//*[contains(text(), 'Интернет-банк')]]");
    private final By phoneInputXPath = By.xpath("//input[@automation-id='phone-input' and @type='tel']");
    private final By continueButtonXPath = By.xpath("//button[@automation-id='button-submit' and @type='submit']");
    private final By errorMessageXPath = By.xpath("//p[@automation-id='server-error' and contains(text(), 'Некорректный номер телефона')]");
    private final By codeInputXPath = By.xpath("//input[@automation-id='otp-input' and @placeholder='••••']");
    private final By errorMessageCodeXPath = By.xpath("//p[@automation-id='server-error' and contains(@class, '_Color_error')]");

    public AuthPage(WebDriver driver) {
        super(driver);
    }

    public void openAuthPage() {
        WebDriverWait waitLong = new WebDriverWait(driver, Duration.ofSeconds(30));
        Actions actions = new Actions(driver);

        WebElement personalCabinetLink = waitLong.until(ExpectedConditions.presenceOfElementLocated(personalCabinetLinkXPath));
        actions.moveToElement(personalCabinetLink).perform(); // Наводим курсор на "Личный кабинет"

        WebElement internetBankButton = waitLong.until(ExpectedConditions.elementToBeClickable(internetBankButtonXPath));
        internetBankButton.click();

        switchToNewTab();
        waitLong.until(ExpectedConditions.visibilityOfElementLocated(phoneInputXPath));
    }

    public void enterPhone(String phone) {
        WebElement phoneField = wait.until(ExpectedConditions.visibilityOfElementLocated(phoneInputXPath));
        phoneField.sendKeys(phone);
    }

    public void clickContinueButton() {
        WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(continueButtonXPath));
        continueButton.click();
    }

    public void enterCode(String code) {
        WebElement codeField = wait.until(ExpectedConditions.visibilityOfElementLocated(codeInputXPath));
        codeField.sendKeys(code);
    }

    public void clickResendCodeButton() {
        WebElement resendButton = waitForElementVisible(resendCodeButtonXPath, 100);
        resendButton.click();
    }

    public String getErrorMessage() {
        try {
            WebElement errorElement = wait.until(ExpectedConditions.visibilityOfElementLocated(errorMessageXPath));
            return errorElement.getText();
        } catch (org.openqa.selenium.TimeoutException e) {
            return null;
        }
    }

    public String getErrorCodeMessage() {
        try {
            WebElement errorElement = wait.until(ExpectedConditions.visibilityOfElementLocated(errorMessageCodeXPath));
            return errorElement.getText();
        } catch (org.openqa.selenium.TimeoutException e) {
            return null;
        }
    }

    private void switchToNewTab() {
        String originalWindowHandle = driver.getWindowHandle();
        wait.until(ExpectedConditions.numberOfWindowsToBe(2));
        for (String windowHandle : driver.getWindowHandles()) {
            if (!windowHandle.equals(originalWindowHandle)) {
                driver.switchTo().window(windowHandle);
                break;
            }
        }
    }

    public WebElement waitForElementVisible(By locator, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public boolean waitForElementInvisible(By locator, int timeoutSeconds) {
        WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return customWait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }
}