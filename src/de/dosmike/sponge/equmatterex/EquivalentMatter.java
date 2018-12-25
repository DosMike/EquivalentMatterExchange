package de.dosmike.sponge.equmatterex;

import com.google.inject.Inject;
import com.sun.xml.internal.ws.encoding.soap.DeserializationException;
import de.dosmike.sponge.equmatterex.customNBT.CustomNBT;
import de.dosmike.sponge.equmatterex.emcDevices.DeviceRegistry;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.SpongeExecutorService;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Plugin(id="equivalentmatter", name="Equivalent Matter Exchange", version="0.1", authors={"DosMike"})
public class EquivalentMatter {
	
	public static void main(String[] args) { System.err.println("This plugin can not be run as executable!"); }
	
	static EquivalentMatter instance;
	public static EquivalentMatter getInstance() { return instance; }

	public PluginContainer getContainer() { return Sponge.getPluginManager().fromInstance(this).get(); }

	private SpongeExecutorService asyncExecutor, syncExecutor;
	public static SpongeExecutorService getAsyncExecutor() {
		return instance.asyncExecutor;
	}
	public static SpongeExecutorService getSsyncExecutor() {
		return instance.syncExecutor;
	}

	@Inject
	private Logger logger;
	private static final Object logMutex = new Object();
	public static void l(String format, Object... args) {
		synchronized (logMutex) {
			instance.logger.info(String.format(format, args));
		}
	}
	public static void w(String format, Object... args) {
		synchronized (logMutex) {
			instance.logger.warn(String.format(format, args));
		}
	}

	@Inject
	@ConfigDir(sharedRoot = false)
	private Path privateConfigDir;

	/// --- === Main Plugin stuff === --- \\\

	@Listener
	public void onServerPreInit(GamePreInitializationEvent event) {
		instance = this;
		
		Sponge.getEventManager().registerListeners(this, new EventListeners());
		Sponge.getEventManager().registerListeners(this, new CustomNBT());
	}
	@Listener
	public void onServerInit(GameInitializationEvent event) {
		Calculator.resetAndDefaults();
	}
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		asyncExecutor = Sponge.getScheduler().createAsyncExecutor(this);
		syncExecutor = Sponge.getScheduler().createSyncExecutor(this);
		if (!loadConfigs()) {
			l("Could not load config, rebuilding EMC values. (This could take some time!");
			invokeAsyncCalculation()
					.whenCompleteAsync((v,e)->{
						if (e != null) {
							w("EMC Calculation failed!");
							e.printStackTrace();
						} else {
							l("EMC Calculation finished!");
						}
					});
		}
		CommandRegistra.registerCommands();
		syncExecutor.scheduleAtFixedRate(DeviceRegistry::deviceTick
				, 1000, 50, TimeUnit.MILLISECONDS);
		l("Equivalent Matter Exchange started!");
	}
	/**
	 * @return success
	 */
	public boolean loadConfigs() {
		try {
			ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder()
					.setPath(getConfigDir().resolve("emcValues.conf"))
					.build();
			ConfigurationNode root = loader.load();
			if (root.isVirtual()) return false;
			root.getNode("presets").getChildrenMap().forEach((key, value)->{
				ItemType type = Sponge.getRegistry().getType(ItemType.class, key.toString()).orElseThrow(()->new DeserializationException("No ItemType "+key.toString()+" was recognised!"));
				BigInteger emc = new BigInteger(value.getString(), 10);
				Calculator.setFixCost(type, emc);
			});
			root.getNode("calculated").getChildrenMap().forEach((key, value)->{
				ItemType type = Sponge.getRegistry().getType(ItemType.class, key.toString()).orElseThrow(()->new DeserializationException("No ItemType "+key.toString()+" was recognised!"));
				BigInteger emc = new BigInteger(value.getString(), 10);
				Calculator.setTemporaryCost(type, emc);
			});
			Calculator.setVerbosity(root.getNode("calulatorVerbosity").getInt(3));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public Path getConfigDir() {
		{
			File folder = privateConfigDir.toFile();
			if (!folder.exists()) folder.mkdirs();
		}
		return privateConfigDir;
	}

	public CompletableFuture<?> invokeAsyncCalculation() {
		CompletableFuture<?> cf = new CompletableFuture<>();
		asyncExecutor.submit(()->{
			try {
				Calculator.calculate();
				cf.complete(null);
			} catch (Exception e) {
				Calculator.resetState();
				cf.completeExceptionally(e);
			}
		});
		return cf;
	}

}