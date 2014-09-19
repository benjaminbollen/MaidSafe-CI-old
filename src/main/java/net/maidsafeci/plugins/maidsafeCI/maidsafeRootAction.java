package net.maidsafeci.plugins.maidsafeCI

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.UnprotectedRootAction;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by ben on 19/09/14.
 */

@Extension
public class maidsafeRootAction implements UnprotectedRootAction {
    static final String URL = "maidsafehook"; // configure webhook
    private static final Logger logger = Logger.getLogger(maidsafeRootAction.class.getName());

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getURLName() {
        return URL;
    }

    public void doIndex(StaplerRequest req, StaplerResponse resp) {
        String event = req.getHeader("X-GitHub-Event");
        String payload = req.getParameter("payload");
        if (payload == null) {
            Logger.log(Level.SEVERE, "Request doesn't contain payload.");
            return;
        }


    }


}
