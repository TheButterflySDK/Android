package com.example.butterflyhost;

import org.junit.Test;
import com.butterfly.sdk.ButterflySdk;
import com.butterfly.sdk.utils.SdkLogger;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void checkDeepLinkHandling() {
        try {
            ButterflySdk.overrideLanguage("en");
        } catch (Exception e) {
            SdkLogger.Companion.error("tests", e);
        }
    }
}