import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.TestNG;
import org.testng.annotations.*;
import static org.testng.Assert.*;
import org.testng.TestListenerAdapter;

import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Tests
{
    public void main( String[] args )
    {
        TestNG              test = new TestNG();
        TestListenerAdapter lsnr = new TestListenerAdapter();
        test.setTestClasses(new Class[] { Tests.class });
        test.addListener( lsnr );
        test.run();
    }

    @Test( priority = 10 )
    public void RestAPI_correct_username_password() throws Exception
    {
        assertEquals(
            httpPost( System.getenv("USER"), System.getenv("PSWD"), 201 )
            .contains("access_token"),
            true );
    }

    @Test( priority = 20 )
    public void RestAPI_failed_username_password() throws Exception
    {
        assertEquals(
            httpPost( "USER", "PSWD", 401 )
            .contains("AUTHORIZATION_FAILURE"),
            true );
    }

    @Test( priority = 30 )
    public void RestAPI_access_token_has_three_parts_separated_by_period() throws Exception
    {
        String      jstr = httpPost( System.getenv("USER"), System.getenv("PSWD"), 201 );
        JSONObject  json = (JSONObject)(new JSONParser()).parse( jstr );
        String      item = json.get("access_token").toString();
        Pattern     ptrn = Pattern.compile(".+\\..+\\..+");     // '*'>=0 '+'>=1 '{3,}'>=3
        Matcher     mtch = ptrn.matcher( item );
        assertEquals( mtch.matches(), true );
    }

    @Test( priority = 40 )
    public void Selenium_Chrome_login_successfully() throws Exception
    {
        WebDriver driver = webLogin( DesiredCapabilities.chrome(), System.getenv("USER"), System.getenv("PSWD"), true );
        String    header = driver.findElement(By.cssSelector(".active")).getText();
        assertEquals( header, "Dashboard" );
        driver.quit();
    }

    @Test( priority = 50 )
    public void Selenium_Chrome_login_unsuccessfully() throws Exception
    {
        WebDriver driver = webLogin( DesiredCapabilities.chrome(), "user@host.domain.com", "1qaz2wsx", false );
        String    errmsg = driver.findElement(By.cssSelector(".alert > p:nth-child(1)")).getText();
        assertEquals( errmsg, "Incorrect email or password." );
        System.out.println("\tError: "+ errmsg );
        driver.quit();
    }

    @Test( priority = 60 )
    public void Selenium_Chrome_after_successful_login_the_user_is_taken_to_Dashboard() throws Exception
    {
        WebDriver driver = webLogin( DesiredCapabilities.chrome(), System.getenv("USER"), System.getenv("PSWD"), true );
        String    header = driver.findElement(By.cssSelector(".active")).getText();
        assertEquals( header, "Dashboard" );
        driver.quit();
    }

    @Test( priority = 70 )
    public void Selenium_Explorer_login_successfully() throws Exception
    {
        WebDriver driver = webLogin( DesiredCapabilities.internetExplorer(), System.getenv("USER"), System.getenv("PSWD"), true );
        String    header = driver.findElement(By.cssSelector(".active")).getText();
        assertEquals( header, "Dashboard" );
        driver.quit();
    }

    @Test( priority = 80 )
    public void Selenium_Explorer_login_unsuccessfully() throws Exception
    {
        WebDriver driver = webLogin( DesiredCapabilities.internetExplorer(), "user@host.domain.com", "1qaz2wsx", false );
        String    errmsg = driver.findElement(By.cssSelector(".alert > p:nth-child(1)")).getText();
        assertEquals( errmsg, "Incorrect email or password." );
        System.out.println("\tError: "+ errmsg );
        driver.quit();
    }

    @Test( priority = 90 )
    public void Selenium_Explorer_after_successful_login_the_user_is_taken_to_Dashboard() throws Exception
    {
        WebDriver driver = webLogin( DesiredCapabilities.internetExplorer(), System.getenv("USER"), System.getenv("PSWD"), true );
        String    header = driver.findElement(By.cssSelector(".active")).getText();
        assertEquals( header, "Dashboard" );
        driver.quit();
    }

    //  Utilities  -----------------------------------------------------------------
    private String httpPost( String user, String password, int expected ) throws Exception
    {
        int             code;
        String          addr = System.getenv("AURL"), body, jstr;
        HttpPost        post;
        HttpResponse    resp;

        body = "{\"userName\":\""+ user +"\",\"password\":\""+ password +"\"}";
        post = new HttpPost( addr );
        post . setHeader("Content-Type", "application/json");
        post . setEntity(new StringEntity( body ));

        resp = HttpClientBuilder.create().build().execute( post );
        code = resp.getStatusLine().getStatusCode();
        assertEquals( code, expected );

        jstr = EntityUtils.toString( resp.getEntity() );
        assertEquals( jstr.contains("}"), true );
        return jstr;
    }

    private WebDriver webLogin( DesiredCapabilities browser, String user, String password, boolean epicenter ) throws Exception
    {
        WebDriver       driver = new RemoteWebDriver( new URL( System.getenv("HURL") ), browser );
        WebDriverWait   wait   = new WebDriverWait( driver, 60 );

        driver.manage().window().setSize( new Dimension(800, 600 ) );
        driver.manage().window().setPosition( new Point(32, 64 ) );

        driver.get( System.getenv("SURL") +"/sign-in" );
        wait  .until(ExpectedConditions.visibilityOfElementLocated(By.name("email")));
        driver.findElement(By.name("email"))   .sendKeys( user );
        driver.findElement(By.name("password")).sendKeys( password );
        driver.findElement(By.xpath("//button[@type='submit']")).click();
        if( epicenter )
            wait.until( new ExpectedCondition<Boolean>() {
                public Boolean apply( WebDriver wd ) {
                    return wd.findElement(By.cssSelector(".active")).getText().equals("Dashboard");
                }
            } );
        return driver;
    }
}
