package net.omni.ach.hooks;

public class GangsPlusHook {

    private boolean enabled = false;

    public void init() {
        this.enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
