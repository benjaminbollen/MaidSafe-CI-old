package net.maidsafeci.plugins.maidsafeCI;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

/**
 * Created by Benjamin Bollen on 19/09/14.
 */
public class maidsafeGitHub {
    private static final Logger logger = Logger.getLogger(maidsafeGitHub.class.getName());
    private GitHub gh;

    private void connect() throws IOException {
        String accessToken = maidsafeTrigger.getDscp().getAccessToken
    }


}