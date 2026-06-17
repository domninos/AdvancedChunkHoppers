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

    private final EventSubscription<UserDataRecalculateEvent> subscription;

    public LuckPermsHook(AdvancedChunkHoppers plugin) {
        RegisteredServiceProvider<LuckPerms> provider =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);

        if (provider == null) {
            subscription = null;
            return;
        }

        LuckPerms api = provider.getProvider();

        subscription = api.getEventBus().subscribe(
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
        if (subscription != null) {
            subscription.close();
        }
    }
}
