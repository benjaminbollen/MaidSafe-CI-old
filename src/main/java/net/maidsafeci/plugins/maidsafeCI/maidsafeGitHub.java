package net.maidsafeci.plugins.maidsafeCI;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

import static org.kohsuke.github.GitHub.connect;

/**
 * Created by Benjamin Bollen on 19/09/14.
 */
public class maidsafeGitHub {
    private static final Logger logger = Logger.getLogger(maidsafeGitHub.class.getName());
    private GitHub gh;

    private void connect() throws IOException {
        String accessToken = maidsafeTrigger.getDscp().getAccessToken();
        String serverAPIUrl = maidsafeTrigger.getDscp().getServerAPIUrl();
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                gh = GitHub.connectUsingOAuth(serverAPIUrl, accessToken);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Can''t connect to {0} using oauth", serverAPIUrl);
                throw ex;
            }
        } else {
            gh = GitHub.connect(maidsafeTrigger.getDscp().getUsername(), null, maidsafeTrigger.getDscp().getPassword());
        }
    }

    public GitHub get() throws IOException {
        if (gh == null) {
            connect();
        }
        return gh;
    }

    public boolean isUserMemberOfOrganization(String organization, GHUser member) {
        boolean orgHasMember = false;
        try {
            GHOrganization org = get().getOrganization(organization);
            orgHasMember = org.hasMember(member);
            logger.log(Level.FINE, "org.hasMember(member)? user:{0} org: {1} == {2}",
                    new Object[]{member.getLogin(), organization, orgHasMember ? "yes" : "no"});
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
        return orgHasMember;
    }


}