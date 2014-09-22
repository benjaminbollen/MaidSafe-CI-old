package net.maidsafeci.plugins.maidsafeCI;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor

/**
 * Created by Benjamin Bollen on 22/09/14.
 */
public class maidsafeBranch extends AbstractDescribableImpl<maidsafeBranch> {

    private String branch;

    public String getBranch() { return branch; }

    public boolean equals(String s) {
        return branch.equals(s.trim());
    }

    @DataBoundConstructor
    public maidsafeBranch(String branch) {
        this.branch = branch.trim();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<maidsafeBranch> {
        @Override
        public String getDisplayName() {
            return "Branch";
        }
    }
}
