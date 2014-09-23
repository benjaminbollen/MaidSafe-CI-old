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

    public static maidsafeTrigger extractTrigger(AbstractProject proj) {
        Trigger trigger = proj.getTrigger(maidsafeTrigger.class);
        if (trigger == null || !(trigger instanceof maidsafeTrigger)) {
            return null;
        }
        return (maidsafeTrigger) trigger;
    }

    public static DescriptorImpl getDscp() { return DESCRIPTOR; }

    @Override

}
