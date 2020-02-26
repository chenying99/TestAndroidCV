package com.yeyupiaoling.testclassification;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        Map<String, String> map = new HashMap<>();
        map.put("邓超", "孙俪");
        map.put("李晨", "范冰冰");
        System.out.println(map.size());
        map.put("刘德华", "柳岩");
        System.out.println(map.size());
        for (String f:map.keySet()             ) {
            System.out.println(map.get(f));
        }
    }
}