package hudson.plugins.emailext.plugins;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.content.FailedTestsContent;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.util.StreamTaskListener;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;
import java.net.URL;

public class OnlyRegressionsTest extends HudsonTestCase {

    public void testOnlyRegressionsAreShown() throws Exception {
        FreeStyleProject project = createFreeStyleProject("onlyRegressions");
        project.getPublishersList().add(new JUnitResultArchiver("target/testreports/*.xml", true, null));

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener) throws InterruptedException, IOException {
                final URL failedTestReport = OnlyRegressionsTest.class.getClassLoader().getResource("hudson/plugins/emailext/testreports/failed_test.xml");
                FilePath workspace = abstractBuild.getWorkspace();

                FilePath testDir = workspace.child("target").child("testreports");
                testDir.mkdirs();
                FilePath reportFile = testDir.child("failed_test.xml");
                reportFile.copyFrom(failedTestReport);

                return true;
            }
        });
        TaskListener listener = new StreamTaskListener(System.out);
        project.scheduleBuild2(0).get();
        FailedTestsContent failedTestsContent = new FailedTestsContent();
        failedTestsContent.onlyRegressions = true;
        String content = failedTestsContent.evaluate(project.getLastBuild(), listener, FailedTestsContent.MACRO_NAME);
        assertTrue("The failing test should be reported the first time it fails", content.contains("hudson.plugins.emailext"));

        project.scheduleBuild2(0).get();
        content = failedTestsContent.evaluate(project.getLastBuild(), listener, FailedTestsContent.MACRO_NAME);
        assertFalse("The failing test should not be reported the second time it fails", content.contains("hudson.plugins.emailext"));
        assertTrue("The content should state that there are other failing tests still", content.contains("and 1 other failed test"));
    }
}
