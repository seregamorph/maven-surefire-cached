package com.github.seregamorph.maven.test.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestSuiteReportTest {

    @Test
    public void testFailsafeReports() {
        var testSuiteReport = TestSuiteReport.fromFile(getResourceFile(
            "failsafe-reports/TEST-com.github.seregamorph.testsmartcontext.demo.SampleIT.xml"));

        assertEquals("com.github.seregamorph.testsmartcontext.demo.SampleIT", testSuiteReport.name());
        assertEquals(new BigDecimal("0.155"), testSuiteReport.timeSeconds());
        assertEquals(1, testSuiteReport.tests());
        assertEquals(0, testSuiteReport.errors());
        assertEquals(0, testSuiteReport.failures());
        assertEquals(List.of(), testSuiteReport.testcaseFlakyErrors());
        assertEquals(List.of(), testSuiteReport.testcaseFlakyFailures());
        assertEquals(List.of(), testSuiteReport.testcaseErrors());
    }

    @Test
    public void testSurefireReports() {
        var testSuiteReport = TestSuiteReport.fromFile(getResourceFile(
            "surefire-reports/TEST-com.github.seregamorph.testsmartcontext.demo.Unit1Test.xml"));

        assertEquals("com.github.seregamorph.testsmartcontext.demo.Unit1Test", testSuiteReport.name());
        assertEquals(new BigDecimal("0.005"), testSuiteReport.timeSeconds());
        assertEquals(1, testSuiteReport.tests());
        assertEquals(0, testSuiteReport.errors());
        assertEquals(0, testSuiteReport.failures());
        assertEquals(List.of(), testSuiteReport.testcaseFlakyErrors());
        assertEquals(List.of(), testSuiteReport.testcaseFlakyFailures());
        assertEquals(List.of(), testSuiteReport.testcaseErrors());
    }

    @Test
    public void testSurefireFlakyReports() {
        var testSuiteReport = TestSuiteReport.fromFile(getResourceFile(
            "surefire-reports/TEST-com.github.seregamorph.testsmartcontext.demo.FlakyTest.xml"));

        assertEquals("com.github.seregamorph.testsmartcontext.demo.FlakyTest", testSuiteReport.name());
        assertEquals(new BigDecimal("0.005"), testSuiteReport.timeSeconds());
        assertEquals(1, testSuiteReport.tests());
        assertEquals(0, testSuiteReport.errors());
        assertEquals(0, testSuiteReport.failures());
        assertEquals("[TestcaseFailure{testcase=Testcase{classname='com.github.seregamorph.testsmartcontext.demo.FlakyTest', name='testGetSubscriptionId_MicroserviceEnabled'}, type='org.grpcmock.exception.GrpcMockException', message='failed to start gRPC Mock server'}]", testSuiteReport.testcaseFlakyErrors().toString());
        assertEquals(List.of(), testSuiteReport.testcaseFlakyFailures());
        assertEquals("[TestcaseFailure{testcase=Testcase{classname='com.github.seregamorph.testsmartcontext.demo.FlakyTest', name=''}, type='org.grpcmock.exception.GrpcMockException', message='failed to start gRPC Mock server'}]", testSuiteReport.testcaseErrors().toString());
    }

    @Test
    public void testSurefireFlakyFailureReports() {
        var testSuiteReport = TestSuiteReport.fromFile(getResourceFile(
            "surefire-reports/TEST-com.github.seregamorph.testsmartcontext.demo.FlakyFailureTest.xml.xml"));

        assertEquals("com.github.seregamorph.testsmartcontext.demo.FlakyFailureTest", testSuiteReport.name());
        assertEquals(new BigDecimal("0.005"), testSuiteReport.timeSeconds());
        assertEquals(1, testSuiteReport.tests());
        assertEquals(0, testSuiteReport.errors());
        assertEquals(0, testSuiteReport.failures());
        assertEquals(List.of(), testSuiteReport.testcaseFlakyErrors());
        assertEquals("[TestcaseFailure{testcase=Testcase{classname='projects.pt.server.architecture.transactional.TransactionalMethodsCallersArchTest', name='transactionalMethods_shouldNotSelfCall'}, type='java.lang.AssertionError', message='Architecture Violation [Priority: MEDIUM] - Rule 'methods that annotated with @Transactional should be public and should not be final and should not be static and should overrides only an interface method if any and should not has class constructor direct calls (only via DI), because this is required to manage transactions with PROXY advice mode' was violated (1 times):\n"
            + "Constructor Class <com.miro.domain.OrganizationDataDeleters$AllTablesOrganizationDataDeleter> is being called.\n"
            + "Constructor <com.miro.domain.OrganizationDataDeleters.<init>(java.util.List)> calls constructor <com.miro.domain.OrganizationDataDeleters$AllTablesOrganizationDataDeleter.<init>(java.util.List)> in (OrganizationDataDeleters.kt:16)'}]", testSuiteReport.testcaseFlakyFailures().toString());
        assertEquals(List.of(), testSuiteReport.testcaseErrors());
    }

    private static File getResourceFile(String name) {
        var resource = TestSuiteReportTest.class.getClassLoader().getResource(name);
        assertNotNull(resource, "Resource not found: " + name);
        return new File(resource.getFile());
    }
}
