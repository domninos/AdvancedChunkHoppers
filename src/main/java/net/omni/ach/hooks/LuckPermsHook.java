package net.omni.ach.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.managers.ChunkHopperManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckPermsHook {

    private final AdvancedChunkHoppers plugin;
    private EventSubscription<UserDataRecalculateEvent> subscription;

    public LuckPermsHook(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    public void init() {
        RegisteredServiceProvider<LuckPerms> provider =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);

        if (provider == null) {
            this.subscription = null;
            return;
        }

        LuckPerms api = provider.getProvider();

        this.subscription = api.getEventBus().subscribe(
                plugin,
                UserDataRecalculateEvent.class,
                event -> {
                    User user = event.getUser();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        ChunkHopperManager manager =
                                plugin.getChunkHopperManager();

                        manager.getChunkHoppers().forEach((id, chunkHopper) -> {
                            if (chunkHopper != null
                                    && chunkHopper.getOwnerUUID()
                                    .equals(user.getUniqueId())) {

                                manager.recalculateLimit(chunkHopper);
                            }
                        });
                    });
                }
        );
    }

    public void unregister() {
        if (isEnabled())
            subscription.close();
    }

    public boolean isEnabled() {
        return this.subscription != null;
    }
}
