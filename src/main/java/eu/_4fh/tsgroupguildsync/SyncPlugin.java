package eu._4fh.tsgroupguildsync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

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
import eu._4fh.tsgroupguildsync.commands.ForceRefreshCommand;
import eu._4fh.tsgroupguildsync.commands.GetAuthUrlCommand;
import eu._4fh.tsgroupguildsync.sync.RestSync;
import eu._4fh.tsgroupguildsync.sync.SyncTask;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.BadRequestException;
import io.undertow.util.Headers;

public class SyncPlugin implements HandleBotEvents, HandleTS3Events, LoadConfiguration {

	private JTS3ServerMod_Interface mod = null;
	private JTS3ServerQuery query;

	private Logger log = new Logger();
	private List<AbstractCommand> commands = Collections
			.unmodifiableList(Arrays.asList(new GetAuthUrlCommand(), new ForceRefreshCommand()));
	private Config config;
	private SyncTask syncTask;
	private Undertow httpServer;

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
		syncTask = new SyncTask(this);
	}

	public void handleOnBotConnect() {
	}

	public void handleAfterCacheUpdate() {
	}

	public void activate() {
		assertInitialized();
		log.info("Activated");
		syncTask.start();
		httpServer = Undertow.builder().addHttpListener(config.httpServerPort(), config.httpServerAddress())
				.setHandler(new HttpHandlerImpl()).build();
		httpServer.start();
	}

	public void disable() {
		assertInitialized();
		httpServer.stop();
		httpServer = null;
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

	private class HttpHandlerImpl implements HttpHandler {
		@Override
		public void handleRequest(HttpServerExchange exchange) throws Exception {
			if (!exchange.getRequestPath().endsWith("/auth/finished")) {
				exchange.setStatusCode(404);
				exchange.getRequestHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send("NOT FOUND. Path must end with /auth/finished");
				return;
			}

			final Map<String, Deque<String>> queryParams = exchange.getQueryParameters();

			final Deque<String> paramError = queryParams.getOrDefault("error", new LinkedList<>());
			final Deque<String> paramErrorDesc = queryParams.getOrDefault("errorDescription", new LinkedList<>());
			if (paramError.size() > 0 || paramErrorDesc.size() > 0) {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send("Authentication failed.\n\nError: " + Util.join(paramError, " ")
						+ "\n\nError-Description: " + Util.join(paramErrorDesc, " "));
			}

			final Deque<String> paramRemoteId = queryParams.getOrDefault("remoteId", new LinkedList<>());
			if (paramRemoteId.size() != 1) {
				throw new BadRequestException("Parameter remoteId is either given more than once or not at all");
			}
			final long remoteId = Long.parseLong(paramRemoteId.getFirst());

			final boolean added = new RestSync(getQuery(), getLog(), getConfig()).syncSingle(remoteId);
			if (!added) {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send("Authentication successfull, "
						+ "but either you were already authenticated or according to the blizzard api you are not in the guild. "
						+ "Probably try again later.");
			} else {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send("Authentication successfull.");
			}
		}
	}
}
