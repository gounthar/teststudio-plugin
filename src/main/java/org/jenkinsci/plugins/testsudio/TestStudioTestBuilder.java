package org.jenkinsci.plugins.testsudio;

import hudson.model.*;
import hudson.remoting.Callable;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jenkins.tasks.SimpleBuildStep;

import static org.jenkinsci.plugins.testsudio.Constants.AIILIST;
import static org.jenkinsci.plugins.testsudio.Constants.TSTEST;

import static org.jenkinsci.plugins.testsudio.Utils.isEmpty;

@SuppressWarnings("unused")
public class TestStudioTestBuilder extends Builder implements SimpleBuildStep, Serializable {

    private static final long serialVersionUID = 154878531464894L;

    private String artOfTestRunnerPath;
    private String testPath;
    private String settingsPath;
    private TestType testType = TestType.SINGLE_TEST;
    private String outputFileName;
    private String dateFormat;
    private String projectRoot;
    private String outputPath;

    private boolean testAsUnit;


    @DataBoundConstructor
    public TestStudioTestBuilder(String artOfTestRunnerPath,
                       String projectRoot,
                       String testPath,
                       boolean testAsUnit,
                       String outputPath,
                       String settingsPath,
                       String dateFormat) {
        this.artOfTestRunnerPath = artOfTestRunnerPath;
        this.testPath = testPath;
        this.settingsPath = settingsPath;
        if (testPath != null && testPath.endsWith(AIILIST)) {
            testType = TestType.TEST_LIST;
        }
        this.testAsUnit = testAsUnit;
        this.dateFormat = dateFormat;
        this.projectRoot = projectRoot;
        this.outputPath = outputPath;

    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {

        MyCallable task = new MyCallable(String.valueOf(workspace), Constants.TEST_STUDIO_RESULTS_DIR,
                                this.artOfTestRunnerPath, this.testAsUnit, this.settingsPath, this.projectRoot);


        String result = launcher.getChannel().call(task);
        listener.getLogger().println(result);
        if (result != null && result.contains("Overall Result:")){
            run.setResult(Result.SUCCESS);
        } else {
            run.setResult(Result.FAILURE);
        }
    }

    class MyCallable implements Callable<String, IOException> {
        private static final long serialVersionUID = 95483212356481L;

        private String workspace;
        private String resultsDir;
        private String artOfTestRunnerPath;
        private boolean testAsUnit;
        private String settings;
        private String projectRoot;

        public MyCallable(String workspace, String resultsDir, String artOfTestRunnerPath, boolean testAsUnit, String settings, String projectRoot){
            this.workspace = workspace;
            this.resultsDir = resultsDir;
            this.testAsUnit = testAsUnit;
            this.artOfTestRunnerPath = artOfTestRunnerPath;
            this.settings = settings;
            this.projectRoot = projectRoot;
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {

        }

        @Override
        public String call() throws IOException {

            String outputFileName = "TestStudioResults-" + System.currentTimeMillis();

            String output = "";
            if (this.testAsUnit) {
                outputFileName += ".junit";
            } else {
                outputFileName += ".junitstep";
            }

            String command = buildCommand(this.workspace, outputFileName, this.resultsDir, this.settings, this.projectRoot);
            output +="Command: \n" + command +"\n";

            String fullOutputName = this.workspace + "\\" + this.resultsDir + "\\" + outputFileName + ".xml";

            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(command);

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            output += "\nCommand output:\n";
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                output += s + "\n";
            }

            // read any errors from the attempted command
            output +="\nSTD Error output (if any):\n";
            while ((s = stdError.readLine()) != null) {
                output += s + "\n";
            }
            try {
                proc.waitFor();
            } catch (InterruptedException e) {
            }
            output +="\nCommand exit code: " + proc.exitValue() +" \n";
            return output;
        }

        private void prepareWorkspace(String workspace) {
            File index = new File(this.workspace + "\\" + this.resultsDir);
            if (!index.exists()) {
                index.mkdir();
            } else {
                String[] entries = index.list();
                for (String s : entries) {
                    File currentFile = new File(index.getPath(), s);
                    currentFile.delete();
                }
                if (!index.exists()) {
                    index.mkdir();
                }
            }
        }

        private boolean fileExists(String file){
            File f = new File(file);
            if(f.exists() && !f.isDirectory()) {
                return true;
            }
            return false;
        }

        private String buildCommand(String workspace, String outputFileName, String resultDir, String settingsPath, String projectRoot) {
            StringBuilder sb = new StringBuilder();
            sb.append("\"");
            sb.append(this.normalizeExecutable(artOfTestRunnerPath));
            sb.append("\"");
            sb.append(" ");
            sb.append(testType.toString());
            sb.append("=\"");
            sb.append(testPath);
            sb.append("\"");
            if (!isEmpty(settingsPath)) {
                sb.append(" ");
                sb.append("settings=\"");
                sb.append(normalizePath(workspace, settingsPath));
                sb.append("\"");
            }

            if (!isEmpty(projectRoot)) {
                sb.append(" ");
                sb.append("root=\"");
                sb.append(normalizePath(workspace, projectRoot));
                sb.append("\"");
            }

            sb.append(" ");
            sb.append("out=\"");
            sb.append(normalizePath(workspace, resultDir));
            sb.append("\"");
            sb.append(" ");
            sb.append("result=\"");
            sb.append(outputFileName);
            sb.append("\"");
            if (this.testAsUnit) {
                sb.append(" junit");
            } else {
                sb.append(" junitstep");
            }

            return sb.toString();
        }

        private String normalizeExecutable(String artOfTestRunnerPath) {
            String pathToLowerCase = artOfTestRunnerPath.toLowerCase();
            if (pathToLowerCase.endsWith("\\")) {
                return artOfTestRunnerPath + "ArtOfTest.Runner.exe";
            } else if (!pathToLowerCase.endsWith("ArtOfTest.Runner.exe".toLowerCase())) {
                return artOfTestRunnerPath + "\\ArtOfTest.Runner.exe";
            }
            return artOfTestRunnerPath;
        }

        private String normalizePath(String workspace, String path){
            String result;
            Matcher m = Pattern.compile("^\\D:\\\\.*$").matcher(path);
            if (!m.find()) {
                if (path.startsWith("\\")) {
                    result = workspace + path;
                } else {
                    result = workspace + "\\" + path;
                }
            } else {
                result = path;
            }
            if (result.endsWith("\\")){
                result = result.substring(0, result.length() - 1);
            }
            return result;
        }
    }

    @SuppressWarnings("unused")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @SuppressWarnings("unused")
        public FormValidation doCheckArtOfTestRunnerPath(@QueryParameter String artOfTestRunnerPath) throws IOException, ServletException {
            if (artOfTestRunnerPath == null || artOfTestRunnerPath.length() == 0) {
                return FormValidation.error(org.jenkinsci.plugins.testsudio.Messages.TestStudioTestBuilder_DescriptorImpl_errors_zero_artOfTestRunnerPath());
            } else {
                File f = new File(artOfTestRunnerPath);
                if (!f.exists()) {
                    return FormValidation.error(org.jenkinsci.plugins.testsudio.Messages.TestStudioTestBuilder_DescriptorImpl_errors_notFound_artOfTestRunnerPath());
                }
            }
            return FormValidation.ok();
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckTestPath(@QueryParameter String testPath) throws IOException, ServletException {

            if (testPath == null || testPath.length() == 0) {
                return FormValidation.error(org.jenkinsci.plugins.testsudio.Messages.TestStudioTestBuilder_DescriptorImpl_errors_zero_testPath());
            } else {
                File f = new File(testPath);
                if (!f.exists()) {
                    return FormValidation.error(org.jenkinsci.plugins.testsudio.Messages.TestStudioTestBuilder_DescriptorImpl_errors_notFound_testPath());
                } else if (!testPath.endsWith(TSTEST) && !testPath.endsWith(AIILIST)) {
                    return FormValidation.error(org.jenkinsci.plugins.testsudio.Messages.TestStudioTestBuilder_DescriptorImpl_errors_extension_testPath());
                }
            }

            return FormValidation.ok();
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckSettingsPath(@QueryParameter String settingsPath) throws IOException, ServletException {

            if (!isEmpty(settingsPath)){
                File f = new File(settingsPath);
                System.out.println(f.getCanonicalPath());
                if (!f.exists()) {
                    return FormValidation.warning(org.jenkinsci.plugins.testsudio.Messages.TestStudioTestBuilder_DescriptorImpl_errors_notFound_settingsPath());
                }
            }

            return FormValidation.ok();
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckOutputPath(@QueryParameter String outputPath) throws IOException, ServletException {

            if (!isEmpty(outputPath)) {
                File f = new File(outputPath);
                if (!f.exists()) {
                    return FormValidation.warning(org.jenkinsci.plugins.testsudio.Messages.TestStudioTestBuilder_DescriptorImpl_errors_notFound_outputPath());
                }
            }

            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return org.jenkinsci.plugins.testsudio.Messages.TestStudioTestBuilder_DescriptorImpl_DisplayName();
        }

    }

    @SuppressWarnings("unused")
    public String getTestPath() {
        return testPath;
    }

    @SuppressWarnings("unused")
    public void setTestPath(String testPath) {
        this.testPath = testPath;
    }

    @SuppressWarnings("unused")
    public String getArtOfTestRunnerPath() {
        return artOfTestRunnerPath;
    }

    @SuppressWarnings("unused")
    public String getSettingsPath() {
        return settingsPath;
    }

    @SuppressWarnings("unused")
    public void setSettingsPath(String settingsPath) {
        this.settingsPath = settingsPath;
    }

    public boolean isTestAsUnit() {
        return this.testAsUnit;
    }

    @DataBoundSetter
    public void setTestAsUnit(boolean testAsUnit) {
        this.testAsUnit = testAsUnit;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getProjectRoot() {
        return projectRoot;
    }

    @DataBoundSetter
    public void setProjectRoot(String projectRoot) {
        this.projectRoot = projectRoot;
    }

    public String getOutputPath() {
        return outputPath;
    }

    @DataBoundSetter
    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }
}



