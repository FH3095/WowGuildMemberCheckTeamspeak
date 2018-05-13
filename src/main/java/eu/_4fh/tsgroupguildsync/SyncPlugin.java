package eu._4fh.tsgroupguildsync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import de.stefan1200.jts3servermod.BotConfigurationException;
import de.stefan1200.jts3servermod.interfaces.HandleBotEvents;
import de.stefan1200.jts3servermod.interfaces.HandleTS3Events;
import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;
import de.stefan1200.jts3servermod.interfaces.LoadConfiguration;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.util.ArrangedPropertiesWriter;
import edu.umd.cs.findbugs.annotations.NonNull;
import eu._4fh.tsgroupguildsync.commands.AbstractCommand;
import eu._4fh.tsgroupguildsync.commands.AddCommand;
import eu._4fh.tsgroupguildsync.commands.ForceRefreshCommand;
import eu._4fh.tsgroupguildsync.commands.GetAuthUrlCommand;
import eu._4fh.tsgroupguildsync.rest.RestHelper;

public class SyncPlugin implements HandleBotEvents, HandleTS3Events, LoadConfiguration {

	private JTS3ServerMod_Interface mod = null;
	private JTS3ServerQuery query;

	private Logger log = new Logger();
	private List<AbstractCommand> commands = Collections
			.unmodifiableList(Arrays.asList(new GetAuthUrlCommand(), new AddCommand(), new ForceRefreshCommand()));
	private Config config;
	private OfficersCache officers;
	private SyncTask syncTask;
	private RestHelper restHelper;

	public @NonNull Logger getLog() {
		return log;
	}

	private void assertInitialized() {
		if (mod == null) {
			throw new IllegalStateException("Called before initClass called");
		}
	}

	public static void main(String[] args) {
		System.out.println("Dont start me directly");
	}

	public void initClass(JTS3ServerMod_Interface modClass, JTS3ServerQuery queryLib, String prefix) {
		log.init(modClass, prefix);
		mod = modClass;
		this.query = queryLib;
		config = new Config(this, prefix);
		restHelper = new RestHelper(config);
		officers = new OfficersCache(this);
		syncTask = new SyncTask(this);
	}

	public void handleOnBotConnect() {
	}

	public void handleAfterCacheUpdate() {
	}

	public void activate() {
		assertInitialized();
		log.info("Activated");
		officers.start();
		syncTask.start();
	}

	public void disable() {
		assertInitialized();
		log.info("Disabled");
	}

	public void unload() {
	}

	public boolean multipleInstances() {
		return false;
	}

	public int getAPIBuild() {
		return 4;
	}

	public String getCopyright() {
		return "4fh.eu";
	}

	// Chat events

	public String[] botChatCommandList(HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin) {
		assertInitialized();
		int senderDbId = mod.getClientDBID(eventInfo.get("invokeruid"));
		if (!officers.getOfficersDbId().contains(senderDbId)) {
			return new String[] {};
		}
		List<String> helpList = new ArrayList<>();
		for (AbstractCommand command : commands) {
			helpList.add(command.getCommandSyntax());
		}
		return helpList.toArray(new String[0]);
	}

	public String botChatCommandHelp(final String strCommand) {
		StringBuilder help = new StringBuilder();
		for (AbstractCommand command : commands) {
			help.append(command.getCommandSyntax()).append(": ");
			help.append(command.getHelp()).append('\n');
		}
		return help.toString();
	}

	public boolean handleChatCommands(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin,
			boolean isAdmin) {
		assertInitialized();
		log.info("HandleChatCommand " + msg);

		int senderDbId = mod.getClientDBID(eventInfo.get("invokeruid"));
		if (!officers.getOfficersDbId().contains(senderDbId)) {
			return false;
		}

		List<String> commandSplitted = new ArrayList<>(Arrays.asList(msg.split(" ")));
		for (ListIterator<String> it = commandSplitted.listIterator(); it.hasNext();) {
			String part = it.next();
			if (part.isEmpty()) {
				it.remove();
			}
		}
		if (commandSplitted.isEmpty()) {
			return false;
		}

		AbstractCommand command = findCommand(commandSplitted.get(0));
		if (command == null) {
			return false;
		}
		command.executeCommand(Integer.parseInt(eventInfo.get("invokerid")), commandSplitted, this);
		return true;
	}

	private @CheckForNull AbstractCommand findCommand(@Nonnull String msg) {
		msg = msg.trim().toLowerCase();

		for (AbstractCommand command : commands) {
			final @Nonnull String strCommand = command.getCommandStart().toLowerCase();
			if (msg.equals(strCommand)) {
				return command;
			}
		}
		return null;
	}

	public void handleTS3Events(String eventType, HashMap<String, String> eventInfo) {
		// Nothing to do
	}

	// Config events

	public void initConfig(ArrangedPropertiesWriter config) {
		assertInitialized();
		this.config.initConfig(config);
	}

	public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode)
			throws BotConfigurationException, NumberFormatException {
		assertInitialized();
		return this.config.loadConfig(config, slowMode);
	}

	public void setListModes(BitSet listOptions) {
		assertInitialized();
		config.setListModes(listOptions);
	}

	public @Nonnull JTS3ServerMod_Interface getMod() {
		if (mod == null) {
			throw new RuntimeException("Cant get JTS3ServerMod yet");
		}
		return mod;
	}

	public @Nonnull JTS3ServerQuery getQuery() {
		if (query == null) {
			throw new RuntimeException("Cant get JTS3ServerQuery yet");
		}
		return query;
	}

	public @Nonnull Config getConfig() {
		if (config == null) {
			throw new RuntimeException("Cant get config yet");
		}
		return config;
	}

	public @Nonnull SyncTask getSyncTask() {
		if (syncTask == null) {
			throw new RuntimeException("Cant get syncTask yet");
		}
		return syncTask;
	}

	public @Nonnull RestHelper getRestHelper() {
		if (restHelper == null) {
			throw new RuntimeException("Cant get restHelper yet");
		}
		return restHelper;
	}
}
