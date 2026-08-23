package top.midream.ddoor.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Optional Vault economy bridge; inert when Vault or an economy plugin is absent. */
public class VaultHook {

    private Economy economy;

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        return economy == null ? String.valueOf(amount) : economy.format(amount);
    }
}
