package net.maidsafeci.plugins.maidsafeCI;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.google.common.base.Preconditions;
import hudson.model.AbstractProject;
import jenkins.model.Jenkins;
import org.kohsuke.github.GHUser;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.*;

/**
 * Created by Benjamin Bollen on 22/09/14.
 */

public class maidsafe {
    private static final Logger logger = Logger.getLogger(maidsafe.class.getName());
    private static final Pattern githubUserRepoPattern = Pattern.compile("^(http[s]?://[^/]*)/([^/]*)/([^/]*).*");

    private final String triggerPhrase;
    private final maidsafeTrigger trigger;
    private final AbstractProject<?, ?> project;
    private final Pattern retestPhrasePattern;
    private final Pattern whitelistPhrasePattern;
    private final Pattern oktotestPhrasePattern;
    private maidsafeRepository repository;
    private maidsafeBuilds builds;

    public maidsafe(AbstractProject<?, ?> project, maidsafeTrigger trigger, ConcurrentMap<Integer, maidsafePullRequest> pulls) {
        this.project = project;

        final GithubProjectProperty ghpp = project.getProperty(GithubProjectProperty.class);
        if (ghpp == null || ghpp.getProjectUrl() == null) {
            throw new IllegalStateException("A GitHub project URL is required.");
        }
        String baseUrl = ghpp.getProjectUrl().baseUrl();
        Matcher m = githubUserRepoPattern.matcher(baseUrl);
        if (!m.matches()) {
            throw new IllegalStateException(String.format("Invalid GitHub project url: %s", baseUrl));
        }
        final String user = m.group(2);
        final String repo = m.group(3);

        this.trigger = trigger;
        this.triggerPhrase = trigger.getTriggerPhrase();

        retestPhrasePattern = Pattern.compile(trigger.getDescriptor().getRetestPhrase());
        whitelistPhrasePattern = Pattern.compile(trigger.getDescriptor().getWhitelistPhrase());
        oktotestPhrasePattern = Pattern.compile(trigger.getDescriptor().getOkToTestPhrase());

        this.repository = new maidsafeRepository(user, repo, this, pulls)


    }
}
