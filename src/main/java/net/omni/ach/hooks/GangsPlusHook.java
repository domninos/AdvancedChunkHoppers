package net.omni.ach.hooks;

import net.brcdev.gangs.GangsPlusApi;
import net.brcdev.gangs.gang.Gang;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public class GangsPlusHook {

    private boolean enabled = false;

    public void init() {
        this.enabled = true;
    }

    public boolean isGangMember(Player player, UUID ownerUUID) {
        if (!isEnabled())
            return false;

        if (!GangsPlusApi.isInGang(player))
            return false;

        Gang gang = GangsPlusApi.getPlayersGang(player);
        if (gang == null)
            return false;

        return gang.getAllMembers().stream()
                .filter(Objects::nonNull)
                .anyMatch(member -> member.getUniqueId().equals(ownerUUID));
    }

    public boolean isEnabled() {
        return enabled;
    }
}
