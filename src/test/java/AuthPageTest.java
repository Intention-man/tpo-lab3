import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AuthPageTest extends BaseTest {

    private AuthPage authPage;
    private final String VALID_PHONE = "+79058954595";
    private final String INVALID_PHONE = "+5";
    private final String INVALID_CODE = "000000";
    private final By passwordHeaderXPath = By.xpath("//h1[@automation-id='form-title' and text()='Введите пароль']");


    @BeforeMethod
    public void setupAuthPage() {
        authPage = new AuthPage(driver);
        driver.get("https://www.tbank.ru/travel/");
    }

    @Test(description = "1. Правильно введен телефон и код из смс (ручной ввод кода)")
    public void testSuccessfulLoginWithManualCodeEntry() {
        authPage.openAuthPage();
        authPage.enterPhone(VALID_PHONE);
        authPage.clickContinueButton();
        WebElement passwordHeader = authPage.waitForElementVisible(passwordHeaderXPath, 30);
        Assert.assertTrue(passwordHeader.isDisplayed(), "Должны перейти на страницу ввода пароля после ввода корректного кода.");
    }

    @Test(description = "2. Правильно введен телефон, но код неверный")
    public void testLoginWithValidPhoneAndInvalidCode() {
        authPage.openAuthPage();
        authPage.enterPhone(VALID_PHONE);
        authPage.clickContinueButton();
        authPage.enterCode(INVALID_CODE);
        String errorMessage = authPage.getErrorCodeMessage();
        Assert.assertNotNull(errorMessage, "Error message should be displayed.");
        Assert.assertTrue(errorMessage.contains("Введен неверный код"), "Error message should indicate invalid code."); // Обновленная проверка текста ошибки
    }

    @Test(description = "3. Введен неверный телефон")
    public void testLoginWithInvalidPhone() {
        authPage.openAuthPage();
        authPage.enterPhone(INVALID_PHONE);
        authPage.clickContinueButton();
        String errorMessage = authPage.getErrorMessage();
        Assert.assertNotNull(errorMessage, "Error message should be displayed.");
        Assert.assertTrue(errorMessage.contains("Некорректный номер телефона"), "Error message should indicate invalid phone format.");
    }

    @Test(description = "4. Нажатие на кнопку Отправить еще раз")
    public void testResendCodeButtonClick() {
        authPage.openAuthPage();
        authPage.enterPhone(VALID_PHONE);
        authPage.clickContinueButton();
        WebElement resendButton = authPage.waitForElementVisible(authPage.resendCodeButtonXPath, 100);
        Assert.assertTrue(resendButton.isDisplayed() && resendButton.isEnabled(), "Кнопка 'Отправить еще раз' должна быть видна и активна после таймера.");
        authPage.clickResendCodeButton();
        Assert.assertTrue(authPage.waitForElementInvisible(authPage.resendCodeButtonXPath, 5), "Кнопка 'Отправить еще раз' должна исчезнуть или стать неактивной после клика.");
    }
}