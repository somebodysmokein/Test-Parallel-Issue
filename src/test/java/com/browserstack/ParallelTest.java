package com.browserstack;

import io.cucumber.core.cli.Main;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.browserstack.utils.Utility;

import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelTest {
    public static ThreadLocal<JSONObject> threadLocalValue = new ThreadLocal<>();

    public static void main(String[] args) throws IOException, ParseException {
        JSONObject testConfigs;
        JSONObject testSelectedConfig;
        int threadCount = 5;
        JSONParser parser = new JSONParser();
        testConfigs = (JSONObject) parser.parse(new FileReader("src/test/resources/conf/caps.json"));
        testSelectedConfig = (JSONObject) ((JSONObject) testConfigs.get("tests")).get("parallel");
        JSONArray environments = (JSONArray) testSelectedConfig.get("env_caps");
        if (StringUtils.isNoneEmpty(System.getProperty("parallel-count")) && StringUtils.isNumeric(System.getProperty("parallel-count"))) {
            threadCount = Integer.parseInt(System.getProperty("parallel-count"));
        }

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        for (Object obj : environments) {
            JSONObject singleConfig = Utility.getCombinedCapability((Map<String, String>) obj, testConfigs, testSelectedConfig);
            System.out.println("Inside Parallel Tests");
            //System.out.println(singleConfig.toJSONString());
            Runnable task = new Task(singleConfig, threadLocalValue);
            pool.execute(task);
        }

        pool.shutdown();
    }

}

class Task implements Runnable {
    private JSONObject singleConfig;
    private ThreadLocal<JSONObject> threadLocalValue;

    public Task(JSONObject singleConfig, ThreadLocal<JSONObject> threadLocalValue) {
        this.singleConfig = singleConfig;
        this.threadLocalValue = threadLocalValue;
    }

    public void run() {
        System.setProperty("parallel", "true");
        threadLocalValue.set(singleConfig);
        //System.out.println(threadLocalValue.get());
        try {
            String[] argv = new String[]{"-g", "", "src/test/resources/features/"};
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            Main.run(argv, contextClassLoader);
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        } finally {
            threadLocalValue.remove();
        }
    }
}
