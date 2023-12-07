package com.browserstack.utils;

import com.browserstack.local.Local;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Utility {
    private static JSONObject config;
    private static JSONParser parser = new JSONParser();
    private static String osCaps;
    private static JSONObject singleCapabilityJson;
    private static JSONArray environments;
    private static Local local;
    private static final String LOCATION_SCRIPT_FORMAT = "navigator.geolocation.getCurrentPosition = function(success){\n" +
            "    var position = { \"coords\":{\"latitude\":\"%s\",\"longitude\":\"%s\"}};\n" +
            "    success(position);\n" +
            "}";
    private static final String OFFER_LATITUDE = "19";
    private static final String OFFER_LONGITUDE = "72";

    private Utility(){}

    public static Map<String,String> getLocalOptions(JSONObject config) {
        Map<String, String> localOptions = new HashMap<>();
        JSONObject localOptionsJson = (JSONObject) ((JSONObject) ((JSONObject) config.get("tests")).get("local")).get("local_binding_caps");
        for (Object o : localOptionsJson.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            if (localOptions.get(pair.getKey().toString()) == null) {
                localOptions.put(pair.getKey().toString(), pair.getValue().toString());
            }
        }
        return localOptions;
    }

    public static void setSessionStatus(WebDriver webDriver, String status, String reason) {
        JavascriptExecutor jse = (JavascriptExecutor)webDriver;
        jse.executeScript(String.format("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"%s\", \"reason\": \"%s\"}}",status,reason));
    }

    public static JSONObject getCombinedCapability(Map<String, String> envCapabilities, JSONObject config, JSONObject caps) {
        JSONObject capabilities = new JSONObject();
        Iterator it = envCapabilities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            capabilities.put(pair.getKey().toString(), pair.getValue().toString());
        }
        Map<String, String> commonCapabilities = (Map<String, String>) caps.get("common_caps");
        it = commonCapabilities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if(capabilities.get(pair.getKey().toString()) == null){
                capabilities.put(pair.getKey().toString(), pair.getValue().toString());
            }
        }
        JSONObject singleConfig = new JSONObject();
        singleConfig.put("user",config.get("user"));
        singleConfig.put("key",config.get("key"));
        singleConfig.put("capabilities",capabilities);
        if(caps.containsKey("application_endpoint")) {
            singleConfig.put("application_endpoint",caps.get("application_endpoint"));
        } else {
            singleConfig.put("application_endpoint",config.get("application_endpoint"));
        }
        return singleConfig;
    }

    public static JSONObject getConfig() throws ParseException, IOException {

        if (System.getenv("caps") != null) {
            config = (JSONObject) parser.parse(System.getenv("caps"));
        } else {
            config = (JSONObject) parser.parse(new FileReader("resources/conf/test_caps.json"));
            System.out.println("Read from caps.json = true");
        }
        return config;
    }

    public static String getOsCaps(){
        if (System.getProperty("caps-type").equalsIgnoreCase("single")) {
            osCaps = System.getProperty("caps-type");
            System.out.println(osCaps);
        } else if (System.getProperty("caps-type").equalsIgnoreCase("local")) {
            osCaps = System.getProperty("caps-type");
            System.out.println(osCaps);
        } else if (System.getProperty("caps-type").equalsIgnoreCase("parallel")) {
            osCaps = System.getProperty("caps-type");
            System.out.println(osCaps);
        }
        return osCaps;
    }

    public static JSONObject getSingleCapabilityObject(JSONObject config, String capsType){
        //get capabilities object
        singleCapabilityJson = (JSONObject) ((JSONObject) config.get("tests")).get(capsType);
        environments = (JSONArray)singleCapabilityJson.get("env_caps");
        JSONObject capabilityObject = Utility.getCombinedCapability((Map<String, String>) environments.get(0),config,singleCapabilityJson);
        return capabilityObject;
    }

    public static DesiredCapabilities getFinalCaps(JSONObject capabilityObject) throws Exception {
        DesiredCapabilities caps = new DesiredCapabilities();
        Map<String, String> commonCapabilities = (Map<String, String>) capabilityObject.get("capabilities");
        Iterator it = commonCapabilities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if (caps.getCapability(pair.getKey().toString()) == null) {
                caps.setCapability(pair.getKey().toString(), pair.getValue().toString());
            }
        }

        if (caps.getCapability("browserstack.local") != null && caps.getCapability("browserstack.local").equals("true")) {
            String localIdentifier = RandomStringUtils.randomAlphabetic(8);
            caps.setCapability("browserstack.localIdentifier", localIdentifier);
            local = new Local();
            Map<String, String> options = Utility.getLocalOptions(config);
            System.out.println((String) capabilityObject.get("key"));
            options.put("key", (String) capabilityObject.get("key"));
            options.put("localIdentifier", localIdentifier);
            System.out.println("Local Start");
            local.start(options);
        }

        return caps;
    }

    public static String getRemoteUrl(JSONObject capabilityObject){
        String username = System.getenv("BROWSERSTACK_USERNAME");
        if (username == null) {
            username = (String) capabilityObject.get("user");
        }
        String accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
        if (accessKey == null) {
            accessKey = (String) capabilityObject.get("key");
        }

        return String.format("https://%s:%s@hub.browserstack.com/wd/hub", username, accessKey);
    }

    public static boolean isAscendingOrder(List<WebElement> priceWebElement, int length) {
        if (priceWebElement == null || length < 2)
            return true;
        if (Integer.parseInt(priceWebElement.get(length - 2).getText()) > Integer.parseInt(priceWebElement.get(length - 1).getText()))
            return false;
        return isAscendingOrder(priceWebElement, length - 1);
    }

    public static void mockGPS(WebDriver webDriver) {
        String locationScript = String.format(LOCATION_SCRIPT_FORMAT, OFFER_LATITUDE, OFFER_LONGITUDE);
        ((JavascriptExecutor) webDriver).executeScript(locationScript);
    }

}
