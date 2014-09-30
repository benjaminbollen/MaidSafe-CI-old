package net.maidsafeci.plugins.maidsafeCI;

import antlr.ANTLRException;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.model.*;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.RevisionParameterAction;
import hudson.plugins.git.util.BuildData;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.github.GHAuthorization;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by Benjamin Bollen on 22/09/14.
 * Forked from ghprb
 */

public class maidsafeTrigger extends Trigger<AbstractProject<?, ?>> {

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    private static final Logger logger = Logger.getLogger(maidsafeTrigger.class.getName());
    private final String triggerPhrase;
    private final Boolean onlyTriggerPhrase;
    private Boolean autoCloseFailedPullRequests;
    private List<maidsafeBranch> whiteListTargetBranches;
    private transient maidsafe helper;
    private String project;

    @DataBoundConstructor
    public maidsafeTrigger(String triggerPhrase,
                           Boolean onlyTriggerPhrase,
                           Boolean autoCloseFailedPullRequests,
                           List<maidsafeBranch> whiteListTargetBranches) throws ANTLRException {
        this.triggerPhrase = triggerPhrase;
        this.onlyTriggerPhrase = onlyTriggerPhrase;
        this.autoCloseFailedPullRequests = autoCloseFailedPullRequests;
        this.whiteListTargetBranches = whiteListTargetBranches;
    }

    public static maidsafeTrigger extractTrigger(AbstractProject project) {
        Trigger trigger = project.getTrigger(maidsafeTrigger.class);
        if (trigger == null || !(trigger instanceof maidsafeTrigger)) {
            return null;
        }
        return (maidsafeTrigger) trigger;
    }

    public static DescriptorImpl getDscp() { return DESCRIPTOR; }

    @Override
    public void start(AbstractProject<?, ?> project, boolean newInstance) {
        this.project = project.getName();
        if (project.getProperty(GithubProjectProperty.class) == null) {
            logger.log(Level.INFO, "GitHub project not set up, cannot start maidsafe trigger for job " + this.project);
            return;
        }
        try {
            helper = createMaidsafe(project);
        } catch (IllegalStateException ex) {
            logger.log(Level.SEVERE, "Can't start maidsafe trigger", ex);
            return;
        }

        logger.log(Level.INFO, "Starting the maidsafe trigger for the {0} job; newInstance is {1}",
                new String[]{this.project, String.valueOf(newInstance)});
        super.start(project, newInstance);
        helper.init();
    }

    maidsafe createMaidsafe(AbstractProject<?, ?> project) {
        return new maidsafe(project, this, getDescriptor().getPullRequests(project.getName()));
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "Stopping the ghprb trigger for project {0}", this.project);
        if (helper != null) {
            helper.stop();
            helper = null;
        }
        super.stop();
    }

    @Override
    public void run() {
        // only trigger on GitHub webhooks, not from cron or
        logger.log(Level.INFO, "Only running from GitHub webhook")
        return;
    }

    public QueueTaskFuture<?> startJob(maidsafeCause cause, maidsafeRepository repo) {
        ArrayList<ParameterValue> values = getDefaultParameters();
        final String commitSha = cause.isMerged() ? "origin/pr/" + cause.getPullID() + "/merge" : cause.getCommit();
        values.add(new StringParameterValue("sha1", commitSha));
        values.add(new StringParameterValue("maidsafeActualCommit", cause.getCommit()));
        final StringParameterValue pullIdPv = new StringParameterValue("maidsafePullId", String.valueOf(cause.getPullID())));
        values.add(pullIdPv);
        values.add(new StringParameterValue("maidsafeTargetBranch", String.valueOf(cause.getTargetBranch())));
        values.add(new StringParameterValue("maidsafeSourceBranch", String.valueOf(cause.getSourceBranch())));
        // if no email is given out, send to jenkins@maidsafe.net from notifier@maidsafe.net
        values.add(new StringParameterValue("maidsafePullAuthorEmail", cause.getAuthorEmail() != null ? cause.getAuthorEmail() : "jenkins@maidsafe.net"));
        values.add(new StringParameterValue("maidsafePullDescription", String.valueOf(cause.getShortDescription())));
        values.add(new StringParameterValue("maidsafePullTitle", String.valueOf(cause.getTitle()));
        values.add(new StringParameterValue("maidsafePullLink", String.valueOf(cause.getUrl()))));

        // add the previous pr BuildData as an action so that the correct change log is generated by the GitSCM plugin
        // note that this will be removed from the Actions list after the job is completed so that the old (and incorrect)
        // one isn't there
        return this.job.scheduleBuild2(job.getQuietPeriod(), cause, new ParametersAction(values), findPreviousBuildForPullId(pullIdPv), new RevisionParameterAction(commitSha));
    }

    /**
     * Find the previous BuildData for the given pull request number; this may return null
     */
    private BuildData findPreviousBuildForPullId(StringParameterValue pullIdPv) {
        // find the previous build for this particular pull request, it may not be the last build
        for (Run<?,?> r : job.getBuilds()) {
            ParametersAction pa = r.getAction(ParametersAction.class);
            if (pa != null) {
                for (ParameterValue pv : pa.getParameters()) {
                    if (pv.equals(pullIdPv)) {
                        for (BuildData bd : r.getAction(BuildData.class)) {
                            return bd;
                        }
                    }
                }
            }
        }
        return null;
    }



    public String getTriggerPhrase() {
        if (triggerPhrase == null) {
            return "";
        }
        return triggerPhrase;
    }

    public Boolean getOnlyTriggerPhrase() {
        return onlyTriggerPhrase != null && onlyTriggerPhrase;
    }

    public Boolean isAutoCloseFailedPullRequests() {
        if (autoCloseFailedPullRequests == null) {
            Boolean autoClose = getDescriptor().getAutoCloseFailedPullRequests();
            return (autoClose != null && autoClose);
        } else {
            return autoCloseFailedPullRequests;
        }
    }

    public List<maidsafeBranch> getWhiteListTargetBranches() {
        if (whiteListTargetBranches == null) {
            return new ArrayList<maidsafeBranch>();
        }
        return whiteListTargetBranches;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    @VisibleForTesting
    void setHelper(maidsafe helper) {
        this.helper = helper;
    }

    public maidsafeBuilds getBuilds() {
        if (helper == null) {
            logger.log(Level.SEVERE, "The maidsafe trigger for {0} wasn't properly started - helper is null", this.project);
        }
    }

    public static final class DescriptorImpl extends TriggerDescriptor {

    }
}
