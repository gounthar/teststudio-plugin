package org.jenkinsci.plugins.testsudio.data;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "failure",
        "error",
        "skipped"
})
public class Testcase
        implements Serializable {

    private final static long serialVersionUID = -1L;
    protected Testcase.Failure failure;
    protected Testcase.Error error;

    @XmlElement
    protected String skipped;

    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "classname")
    protected String classname;
    @XmlAttribute(name = "time")
    protected double time;

    public Testcase.Failure getFailure() {
        return failure;
    }

    public void setFailure(Testcase.Failure value) {
        this.failure = value;
    }

    public Testcase.Error getError() {
        return error;
    }

    public void setError(Testcase.Error value) {
        this.error = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String value) {
        this.classname = value;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double value) {
        this.time = value;
    }

    public String getSkipped() {
        return skipped;
    }

    public void setSkipped() {
        this.skipped = "";
    }
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Error
            implements Serializable {


        private final static long serialVersionUID = -1L;
        @XmlValue
        protected String value;
        @XmlAttribute(name = "message")
        protected String message;
        @XmlAttribute(name = "type")
        protected String type;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String value) {
            this.message = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String value) {
            this.type = value;
        }

    }
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Failure
            implements Serializable {


        private final static long serialVersionUID = -1L;
        @XmlValue
        protected String value;
        @XmlAttribute(name = "message")
        protected String message;
        @XmlAttribute(name = "type")
        protected String type;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String value) {
            this.message = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String value) {
            this.type = value;
        }
    }
}
