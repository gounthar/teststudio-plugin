package org.jenkinsci.plugins.data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@XmlRootElement(name = "testsuites")
public class Testsuites implements Serializable {

    private final static long serialVersionUID = -1L;
    @XmlElement(name = "testsuite")
    protected List<Testsuite> testsuites;

    public List<Testsuite> getTestsuites() {
        if (testsuites == null) {
            testsuites = new ArrayList<Testsuite>();
        }

        return this.testsuites;
    }
}
