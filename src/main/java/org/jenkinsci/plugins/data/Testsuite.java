package org.jenkinsci.plugins.data;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "testsuite")
public class Testsuite implements Serializable {

    private final static long serialVersionUID = -1L;
    // errors="0" failures="0" hostname="spina.stsci.edu" name="edu.stsci.CoSI.test.DependencySupressingConstraintJUnitTest" tests="6" time="0.14" timestamp="2012-02-27T18:08:03"

    @XmlAttribute(name = "errors")
    protected int errors;
    @XmlAttribute(name = "failures")
    protected int failures;
    @XmlAttribute(name = "skipped")
    protected int skipped;
    @XmlAttribute(name = "hostname")
    protected String hostname;
    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "tests")
    protected int tests;
    @XmlAttribute(name = "time")
    protected double time;
    @XmlAttribute(name = "timestamp")
    protected String timestamp;

    @XmlElement(name = "testcase")
    protected List<Testcase> testcases;
    @XmlElement(name = "system-out")
    protected String systemOut;
    @XmlElement(name = "system-err")
    protected String systemErr;


    public List<Testcase> getTestcases() {
        if (testcases == null) {
            testcases = new ArrayList<Testcase>();
        }

        return this.testcases;
    }


    public String getSystemOut() {
        return systemOut;
    }

    public void setSystemOut(String systemOut) {
        this.systemOut = systemOut;
    }

    public String getSystemErr() {
        return systemErr;
    }

    public void setSystemErr(String systemErr) {
        this.systemErr = systemErr;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTests() {
        return tests;
    }

    public void setTests(int tests) {
        this.tests = tests;
    }

    public double getTime() {
        return time;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
