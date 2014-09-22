package net.maidsafeci.plugins.maidsafeCI;

import com.google.common.annotations.VisibleForTesting;
import hudson.model.AbstractBuild;
import jenkins.model.Jenkins;
import org.kohsuke.github.*;
import org.kohsuke.github.GHEventPayload.IssueComment;
import org.kohsuke.github.GHEventPayload.PullRequest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Benjamin Bollen on 22/09/14.
 */

public class maidsafeRepository {
    private static final Logger logger = Logger.getLogger(maidsafeRepository.class.getName());
    private static final EnumSet<GHEvent> HOOK_EVENTS = EnumSet.of(GHEvent.ISSUE_COMMENT, GHEvent.PULL_REQUEST);

    private final String reponame;
    private final ConcurrentMap<Integer, maidsafePullRequest> pulls;

    private GHRepository ghRepository; // TODO: add dependent submodules
    private maidsafe helper;

    public maidsafeRepository(String user, String repository, maidsafe helper, ConcurrentMap<Integer, maidsafePullRequest> pulls) {
        // constructor gets called from maidsafe class
        this.reponame = user + "/" + repository;
        this.helper = helper;
        this.pulls = pulls;
    }

    public void init() {
        for (maidsafePullRequest pull : pulls.values()) {
            pull.init(helper, this);
        }
        // make the initial call to populate data structures
        initGHRepository();
    }

    private boolean initGHRepository() {
        GitHub github = null;
        try {
            github = helper.getHitHub().get();
            if (github.getRateLimit().remaining() == 0) {
                return false;
            }
        } catch (FileNotFoundException ex) {
            logger.log(Level.INFO, "Rate limit API not found.");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error while accessing rate limit API", ex);
            return false;
        }

        if (ghRepository == null) {
            try {
                ghRepository = github.getRepository(reponame);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Could not retrieve GitHub repository named " + reponame + " (Do you have properly set 'GitHub project' field in job configuration?)", ex);
                return false;
            }
        }
        return true;
    }

    public void check() {
        if (!initGHRepository()) return;

        List<GHPullRequest> openPulls;
        try {
            openPulls = ghRepository.getPullRequests(GHIssueState.OPEN);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not retrieve open pull requests.", ex);
        }

        Set<Integer> closedPulls = new HashSet<Integer>(pulls.keySet());

        for (GHPullRequest pr : openPulls) {
            if (pr.getHead() == null) {
                try {
                    pr = ghRepository.getPullRequest(pr.getNumber());
                } catch(IOException ex){
                    logger.log(Level.SEVERE, "Could not retrieve pr " + pr.getNumber(), ex);
                    return;
                }
            }

            check(pr);
            closedPulls.remove(pr.getNumber());
        }

        // remove closed pulls so we don't check them again
        // TODO: change behaviour of open and active pulls
        for (Integer id : closedPulls) {
            pulls.remove(id);
        }
    }

    private void check(GHPullRequest pr) {

    }
}
