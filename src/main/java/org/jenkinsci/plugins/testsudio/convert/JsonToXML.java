package org.jenkinsci.plugins.testsudio.convert;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import org.jenkinsci.plugins.testsudio.data.Testcase;
import org.jenkinsci.plugins.testsudio.data.Testsuite;
import org.jenkinsci.plugins.testsudio.data.Testsuites;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.jenkinsci.plugins.testsudio.Utils.isEmpty;

public class JsonToXML {

    enum Result {
        Pass,
        Fail,
        NotRun
    }

    private static final String TEST_NAME = "TestName";
    private static final String RESULT = "Result";
    private static final String VALUE = "__value";
    private static final String PASSED_COUNT = "PassedCount";
    private static final String TEST_RESULTS = "TestResults";
    private static final String FAILED_COUNT = "FailedCount";
    private static final String NOT_RUN_COUNT = "NotRunCount";
    private static final String START_TIME = "StartTime";
    private static final String END_TIME = "EndTime";
    private static final String NAME = "Name";
    private static final String MACHINE = "Machine";
    private static final String NETWORK_NAME = "NetworkName";
    private static final String MESSAGE = "Message";
    public static String dateFormat = "yyyy-mm-dd'T'HH:mm:ss";


    public static String convert(String inputFile, String outputFile, String dateFormat, boolean testToJUnit) {
        String result = null;
        if (!isEmpty(dateFormat)) {
            JsonToXML.dateFormat = dateFormat;
        }
        try {
            if (testToJUnit) {
                testToJUnitTest(inputFile, outputFile);
            } else {
                stepToJunitTest(inputFile, outputFile);
            }
        } catch (Exception e) {
            result = e.getMessage();
        }
        return result;
    }

    private static void stepToJunitTest(String inputFile, String outputFile) throws FileNotFoundException, JAXBException {
        Testsuites allTests = new Testsuites();

        GsonBuilder builder = new GsonBuilder();
        JsonReader reader = new JsonReader(new FileReader(inputFile));
        LinkedTreeMap root = (LinkedTreeMap) builder.create().fromJson(reader, Object.class);
        LinkedTreeMap first_level = (LinkedTreeMap) root.get(VALUE);

        LinkedTreeMap machine = (LinkedTreeMap) ((LinkedTreeMap) first_level.get(MACHINE)).get(VALUE);
        String hostname = (String) machine.get(NETWORK_NAME);

        List testResults = (ArrayList) first_level.get(TEST_RESULTS);

        for (Object entry : testResults) {
            Testsuite junitTestSuite = new Testsuite();
            List<Testcase> testcases = junitTestSuite.getTestcases();
            junitTestSuite.setHostname(hostname);
            LinkedTreeMap testSuite = (LinkedTreeMap) ((LinkedTreeMap) entry).get(VALUE);
            junitTestSuite.setName((String) testSuite.get(TEST_NAME));

            int passedCount = ((Double) testSuite.get("TotalPassedSteps")).intValue();
            int executedCount = ((Double) testSuite.get("ExecutingTestStepCount")).intValue();
            int notRunCount = ((Double) testSuite.get("TotalNumberOfNotRunSteps")).intValue();
            int total = executedCount + notRunCount;

            junitTestSuite.setErrors(0);
            junitTestSuite.setFailures(executedCount - passedCount);
            junitTestSuite.setSkipped(notRunCount);
            junitTestSuite.setTests(total);

            Date testSuiteStartDate = null;
            Date testSuiteEndDate = null;
            try {
                testSuiteStartDate = getDate(((String) testSuite.get(START_TIME)), dateFormat);
                testSuiteEndDate = getDate(((String) testSuite.get(END_TIME)), dateFormat);
                junitTestSuite.setTime((testSuiteEndDate.getTime() - testSuiteStartDate.getTime()) / 1000.0);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            List testSteps = (ArrayList) testSuite.get("StepResults");

            for (Object step : testSteps) {
                // ArtOfTest.WebAii.Design.Execution.TestAsStepResult

                String testType = (String) ((LinkedTreeMap) step).get("__type");
                if (testType != null && testType.equals("ArtOfTest.WebAii.Design.Execution.TestAsStepResult")) {
                    LinkedTreeMap test = (LinkedTreeMap) ((LinkedTreeMap) step).get(VALUE);
                    if (test.get("TestResult") == null) {
                        testcases.add(addSkippedTest());
                    } else {
                        testcases.add(transformTestResult(test.get("TestResult")));
                    }
                } else {
                    testcases.add(transformStepResult(step));
                }
            }
            allTests.getTestsuites().add(junitTestSuite);
        }

        File file = new File(outputFile);
        JAXBContext jaxbContext = JAXBContext.newInstance(Testsuites.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(allTests, file);
    }

    private static void testToJUnitTest(String inputFile, String outputFile) throws FileNotFoundException, JAXBException {

        Testsuite testsuite = new Testsuite();
        List<Testcase> testcases = testsuite.getTestcases();

        GsonBuilder builder = new GsonBuilder();
        JsonReader reader = new JsonReader(new FileReader(inputFile));
        LinkedTreeMap root = builder.create().fromJson(reader, Object.class);
        LinkedTreeMap first_level = (LinkedTreeMap) root.get(VALUE);

        testsuite.setName((String) first_level.get(NAME));
        try {
            int passedCount = ((Double) first_level.get(PASSED_COUNT)).intValue();
            int failedCount = ((Double) first_level.get(FAILED_COUNT)).intValue();
            int notRunCount = ((Double) first_level.get(NOT_RUN_COUNT)).intValue();
            int total = passedCount + failedCount + notRunCount;

            testsuite.setErrors(0);
            testsuite.setFailures(failedCount);
            testsuite.setSkipped(notRunCount);
            testsuite.setTests(total);
        } catch (Exception e) {

        }

        Date startDate = null;
        Date endDate = null;
        try {
            startDate = getDate(((String) first_level.get(START_TIME)), dateFormat);
            endDate = getDate(((String) first_level.get(END_TIME)), dateFormat);
            testsuite.setTimestamp((String) first_level.get(START_TIME));
            testsuite.setTime((endDate.getTime() - startDate.getTime()) / 1000.0);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        List testResults = (ArrayList) first_level.get(TEST_RESULTS);
        for (Object entry : testResults) {
            testcases.add(transformTestResult(entry));
        }

        LinkedTreeMap machine = (LinkedTreeMap) ((LinkedTreeMap) first_level.get(MACHINE)).get(VALUE);
        String hostname = (String) machine.get(NETWORK_NAME);

        testsuite.setHostname(hostname);


        File file = new File(outputFile);
        JAXBContext jaxbContext = JAXBContext.newInstance(Testsuite.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        jaxbMarshaller.marshal(testsuite, file);
    }

    private static Testcase transformStepResult(Object entry) {
        Testcase junitTest = new Testcase();
        LinkedTreeMap test = (LinkedTreeMap) ((LinkedTreeMap) entry).get(VALUE);
        junitTest.setName((String) test.get("StepDescription"));

        Result result = Result.values()[((Double) test.get("ResultType")).intValue()];
        // Results
        switch (result) {
            case Pass:
                break;
            case Fail:
                String message = "";
                if ((test.get(MESSAGE) == null)) {
                    try {
                        message += ((LinkedTreeMap) ((LinkedTreeMap) test.get("Exception")).get(VALUE)).get("message").toString();
                    } catch (Exception e) {

                    }
                } else {
                    message += (test.get(MESSAGE));
                }
                Testcase.Failure failure = new Testcase.Failure();
                failure.setValue(message);
                junitTest.setFailure(failure);
                break;
            case NotRun:
                junitTest.setSkipped();
                break;
        }

        return junitTest;
    }

    private static Testcase transformTestResult(Object entry) {

        Testcase junitTest = new Testcase();
        LinkedTreeMap test = (LinkedTreeMap) ((LinkedTreeMap) entry).get(VALUE);
        junitTest.setName((String) test.get(TEST_NAME));

        Date testStartDate = null;
        Date testEndDate = null;
        try {
            testStartDate = getDate(((String) test.get(START_TIME)), dateFormat);
            testEndDate = getDate(((String) test.get(END_TIME)), dateFormat);
            junitTest.setTime((testEndDate.getTime() - testStartDate.getTime()) / 1000.0);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Result result = Result.values()[((Double) test.get(RESULT)).intValue()];
        // Results
        switch (result) {
            case Pass:
                break;
            case Fail:
                String message = "";
                message += (test.get(MESSAGE));
                Testcase.Failure failure = new Testcase.Failure();
                failure.setValue(message);
                junitTest.setFailure(failure);
                break;
            case NotRun:
                junitTest.setSkipped();
                break;
        }
        return junitTest;

    }

    private static Date getDate(String date, String frmt) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(frmt, Locale.ENGLISH);

        return format.parse(date);
    }

    private static Testcase addSkippedTest() {
        Testcase junitTest = new Testcase();
        junitTest.setSkipped();
        return junitTest;
    }
}
