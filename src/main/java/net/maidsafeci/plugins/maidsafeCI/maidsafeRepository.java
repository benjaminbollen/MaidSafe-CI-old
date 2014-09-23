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
        final Integer id = pr.getNumber();
        maidsafePullRequest pull;
        if (pulls.containsKey(id)) {
            pull = pulls.get(id);
        } else {
            pulls.putIfAbsent(id, new maidsafePullRequest(helper, this));
            pull = pulls.get(id);
        }
        pull.check(pr);
    }

    public void creatCommitStatus(AbstractBuild<?, ?> build, GHCommitState state, String message, int id) {
        String sha1 = build.getCause(maidsafeCause.class).getCommit();
        createCommitStatus(sha1, state, Jenkins.getInstance().getRootUrl() + build.getURl(), message, id);
    }

    public void createCommitStatus(String sha1, GHCommitState state, String url, String message, int id) {
        Logger.log(Level.INFO, "Setting status of {0} to {1} with url {2} and message: {3}", new Object[]{sha1, state, url, message});
        try {
            ghRepository.createCommitStatus(sha1, state, url, message);
        } catch (IOException ex) {
            if (maidsafeTrigger.getDscp().getUseComments()) {
                logger.log(Level.INFO, "Could not update commit status of the Pull Request on GitHub. Trying to send comment.", ex);
                if (state == GHCommitState.SUCCESS) {
                    message = message + " " + maidsafeTrigger.getDscp().getMsgSuccess();
                } else {
                    message = message + " " + maidsafeTrigger.getDscp().getMsgFailure();
                }
                addComment(id, message);
            } else {
                logger.log(Level.SEVERE, "Could not update commit status of the Pull Request on GitHub.", ex);
            }
        }
    }

    public String getName() { return reponame; }

    public void addComment(int id, String comment) {
        if (comment.trim().isEmpty()) return;

        try {
            ghRepository.getPullRequest(id).comment(comment);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Couldn't add comment to pull request #" + id + ": '" + comment + "'", ex);
        }
    }

    public void closePullRequest(int id) {
        try {
            ghRepository.getPullRequest(id).close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Couldn't close the pull request #" + id + ": '", ex);
        }
    }

    private boolean hookExist() throws IOException {
        for (GHHook h : ghRepository.getHooks()) {
            if (!"web".equals(h.getName())) {  // weird selection??
                continue;
            }
            if (!getHookUrl().equals(h.getConfig().get("url"))) {
                continue;
            }
            return true;
        }
        return false;
    }

    public boolean createHook() {
        if (ghRepository == null) {
            logger.log(Level.INFO, "Repository not available, cannot set pull request hook for repository {0}", reponame);
            return false;
        }
        try {
            if(hookExist()) {
                return true;
            }
            Map<String, String> config = new HashMap<String, String>();
            config.put("url", new URL(getHookUrl()).toExternalForm());
            config.put("insecure_ssl", "1");
            ghRepository.createHook("web", config, HOOK_EVENTS, true);
            return true;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Couldn't create web hook for repository {0}. Does Jenkins@maidsafe.net have admin rights to the repository?", reponame);
            return false;
        }
    }

    private static String getHookUrl() {
        return Jenkins.getInstance().getRootUrl() + maidsafeRootAction.URL + "/";
    }

    public GHPullRequest get PullRequest(int id) throws IOException {
        return ghRepository.getPullRequest(id);
    }

    void onIssueCommentHook(IssueComment issueComment) throws IOException {
        int id = issueComment.getIssue().getNumber();
        logger.log(Level.FINER, "Comment on issue #{0} from {1}: {2}", new Object[]{id, issueComment.getComment().getUser(), issueComment.getComment().getBody()});
        if (!"created".equals(issueComment.getAction())) {
            return;
        }
        maidsafePullRequest pull = pulls.get(id);
        if (pull == null) {
            pull = new maidsafePullRequest(ghRepository(id), helper, this);
            pulls.put(id, pull);
        }
        pull.check(issueComment.getComment());
        maidsafeTrigger.getDscp().save(); // why?
    }

    void onPullRequestHook(PullRequest pr) {
        if ("openend".equals(pr.getAction()) || "reopened".equals(pr.getAction())) {
            maidsafePullRequest pull = pulls.get(pr.getNumber());
            if (pull = null) {
                pulls.putIfAbsent(pr.getNumber(), new maidsafePullRequest(pr.getPullRequest(), helper, this));
                pull = pulls.get(pr.getNumber());
            }
            pull.check(pr.getPullRequest());
        } else if ("synchronise".equals(pr.getAction())) {
            maidsafePullRequest pull = pulls.get(pr.getNumber());
            if (pull == null) {
                pulls.putIfAbsent(pr.getNumber(), new maidsafePullRequest(pr.getPullRequest(), helper, this));
                pull = pulls.get(pr.getNumber());
            }
            if (pull == null) {
                logger.log(Level.SEVERE, "Pull Request #{0} doesn't exist", pr.getNumber());
                return;
            }
            pull.check(pr.getPullRequest());
        } else if ("closed".equals(pr.getAction())) {
            pulls.remove(pr.getNumber());
        } else {
            logger.log(Level.WARNING, "Unknown Pull Request hook action: {0}", pr.getAction());
        }
        maidsafeTrigger.getDscp().save();
    }

    @VisibleForTesting
    void setHelper(maidsafe helper) { this.helper = helper; }
}
