package com.github.seregamorph.maven.test.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.TreeMap;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;

class HashUtilsTest {

    @Test
    public void shouldHashDirectory() {
        var failsafeReportsFile = new File(getClass().getClassLoader().getResource("failsafe-reports").getFile());
        var resourcesFile = failsafeReportsFile.getParentFile();
        var actual = HashUtils.hashDirectory(resourcesFile);

        assertTrue(actual.containsKey("com/github/seregamorph/maven/test/common/TestSuiteReportTest.class"));
        assertTrue(actual.containsKey("com/github/seregamorph/maven/test/core/HashUtilsTest.class"));
        assertEquals("223cf05e7926df6abfbf0c77fa75d736", actual.get("failsafe-reports/TEST-com.github.seregamorph.testsmartcontext.demo.SampleIT.xml"));
        assertEquals("6e12494faa3b9fa98c9df3ea901e6a63", actual.get("surefire-reports/TEST-com.github.seregamorph.testsmartcontext.demo.Unit1Test.xml"));
    }

    @Test
    public void shouldHashZip() {
        var javaxInjectJar = new File(Inject.class.getProtectionDomain().getCodeSource().getLocation().getFile());

        var map = new TreeMap<>();
        map.put("javax/inject/Inject.class", "1e1338c2ccb7ff592658cbaa55b4c005");
        map.put("javax/inject/Named.class", "c038dca606103aedca493d81b58916e5");
        map.put("javax/inject/Provider.class", "67ef8a21afeaa07d12ef0a7ee9765b57");
        map.put("javax/inject/Qualifier.class", "a5b485879243fcf0eb1fd727a455ac7f");
        map.put("javax/inject/Scope.class", "352aa61d0839b660a383dc221f3d16c2");
        map.put("javax/inject/Singleton.class", "207d735ffb50956af12cb1b955af1927");
        var actual = HashUtils.hashZipFile(javaxInjectJar);
        assertEquals(map, actual);
    }
}
