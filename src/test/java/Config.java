import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class Config {

    @Test
    public void getConf() throws Exception {
        InputStream inpt = new FileInputStream(new File("C:\\Users\\root\\IdeaProjects\\rest-selenium-grid-tests\\src\\test\\resources\\data_01.yaml"));
        Map         conf = (Map) (new Yaml()).load( inpt );
        System.out.println( conf );
        assertEquals( conf.get("input"),  227);
        assertEquals( conf.get("expect"), 333);
    }
}
