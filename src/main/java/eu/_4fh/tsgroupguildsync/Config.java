package eu._4fh.tsgroupguildsync;

import java.net.URI;
import java.security.Key;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.crypto.spec.SecretKeySpec;

import de.stefan1200.jts3servermod.BotConfigurationException;
import de.stefan1200.jts3servermod.interfaces.LoadConfiguration;
import de.stefan1200.util.ArrangedPropertiesWriter;
import edu.umd.cs.findbugs.annotations.NonNull;

public class Config implements LoadConfiguration {
	private static enum ConfigKeys {
		WEBSERVICE_URL("webserviceUrl", "URL to the webservice, for example https://my.server.com/guildsync/."),

		WEBSERVICE_MAC_KEY("webserviceMacKey", "MAC key for the webservice."),

		WEBSERVICE_SYSTEM_NAME("webserviceSystemName", "System name for the webservice.", "Voice"),

		WEBSERVICE_AFTER_AUTH_REDIRECT_TO("webserviceAfterAuthRedirectTo",
				"URL to redirect the user to after he is authed."),

		WEBSERVICE_CHECK_INTERVAL("checkInterval",
				"Interval to check the webservice (in minutes). Use 0 to disable the plugin.", "240"),

		TS_MAIN_GROUP("mainGroup", "The main TS user group for the guild as id number.\n"
				+ "If someone is not in this group but is in the guild according to the webservice he will be added to this group and onJoin* will be triggered.\n"
				+ "If someone is in this group but not in the guild according to the webservice he will be removed from this group and onLeave will be triggered."),

		TS_ON_JOIN_ADD_TO_GROUPS("onJoin_AddToGroups",
				"When a user was added to the guild, add this user the this groups. Give group id numbers comma seperated. Optional.",
				null, true),

		TS_ON_JOIN_REMOVE_FROM_GROUPS("onJoin_RemoveFromGroups",
				"Same as onJoin_AddToGroups but removes user groups.  Optional.", null, true),

		TS_ON_LEAVE_ADD_TO_GROUPS("onLeave_AddToGroups",
				"Same as onJoin_AddToGroups but assigns groups when user is removed from the guild instead of added.  Optional.",
				null, true),

		TS_ON_LEAVE_REMOVE_FROM_GROUPS("onLeave_RemoveFromGroups",
				"Same as onLeave_AddToGroups but removes user groups. Optional.", null, true),

		TS_AUTH_MESSAGE_GROUPS("authMessage_groups",
				"This groups will receive a message with a link to authenticate on joining the ts server", null, true),

		TS_AUTH_MESSAGE_TEXT("authMessage_text", "This message will be send to authMessage_groups", null, true),

		HTTP_SERVER_ADDRESS("HttpServerAddress", "Address for the embedded http server", "localhost", false),

		HTTP_SERVER_PORT("HttpServerPort", "Port for the embedded http server");

		public final @Nonnull String key;
		public final @Nonnull String help;
		public final @CheckForNull String defaultValue;
		public final @Nonnull boolean optional;

		private ConfigKeys(final @Nonnull String configKey, final @Nonnull String configHelp) {
			this(configKey, configHelp, null);
		}

		private ConfigKeys(final @Nonnull String configKey, final @Nonnull String configHelp,
				final @CheckForNull String defaultValue) {
			this(configKey, configHelp, defaultValue, null);
		}

		private ConfigKeys(final @Nonnull String configKey, final @Nonnull String configHelp,
				final @CheckForNull String defaultValue, final @CheckForNull Boolean optional) {
			this.key = configKey;
			this.help = configHelp;
			this.defaultValue = defaultValue;
			this.optional = optional == null ? false : optional.booleanValue();
		}
	};

	private final @NonNull SyncPlugin plugin;
	private final @NonNull String prefix;
	private URI webserviceUrl;
	private byte[] webserviceMacKey;
	private String webserviceSystemName;
	private String redirectAfterAuthTo;
	private int checkInterval;
	private int mainGroup;
	private @NonNull Set<Long> onJoinAddTo;
	private @NonNull Set<Long> onJoinRemoveFrom;
	private @NonNull Set<Long> onLeaveAddTo;
	private @NonNull Set<Long> onLeaveRemoveFrom;
	private @Nonnull Set<Long> authMsgGroups;
	private @CheckForNull String authMsgText;
	private String httpServerAddress;
	private int httpServerPort;

	public Config(final @NonNull SyncPlugin plugin, final @NonNull String prefix) {
		this.plugin = plugin;
		this.prefix = prefix;
		onJoinAddTo = new HashSet<>();
		onJoinRemoveFrom = new HashSet<>();
		onLeaveAddTo = new HashSet<>();
		onLeaveRemoveFrom = new HashSet<>();
		authMsgGroups = new HashSet<>();
	}

	@Override
	public void initConfig(ArrangedPropertiesWriter config) {
		for (ConfigKeys key : ConfigKeys.values()) {
			if (key.defaultValue == null) {
				config.addKey(prefix + "_" + key.key, key.help);
			} else {
				config.addKey(prefix + "_" + key.key, key.help, key.defaultValue);
			}
		}
	}

	private Integer readConfigInt(final ArrangedPropertiesWriter config, final @Nonnull ConfigKeys key,
			final int minValue) throws BotConfigurationException {
		final String strValue = readConfigStr(config, key);
		if (strValue == null) {
			return null;
		}
		final @Nonnull int intValue = Integer.parseInt(strValue);
		if (intValue < minValue) {
			throw new BotConfigurationException(prefix + "_" + key.key + " cant be less than " + minValue);
		}
		return intValue;
	}

	private String readConfigStr(final ArrangedPropertiesWriter config, final @Nonnull ConfigKeys key)
			throws BotConfigurationException {
		final @CheckForNull String strValue = config.getValue(prefix + "_" + key.key);
		if (strValue == null || strValue.isEmpty()) {
			if (key.optional) {
				return null;
			} else {
				throw new BotConfigurationException("Missing " + prefix + "_" + key.key);
			}
		}
		return strValue;
	}

	@Override
	public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode)
			throws BotConfigurationException, NumberFormatException {
		checkInterval = readConfigInt(config, ConfigKeys.WEBSERVICE_CHECK_INTERVAL, 0);
		if (checkInterval == 0) {
			plugin.getLog().info(prefix + "_checkInterval = 0, disabling plugin");
			return false;
		}

		webserviceUrl = URI.create(readConfigStr(config, ConfigKeys.WEBSERVICE_URL));
		redirectAfterAuthTo = readConfigStr(config, ConfigKeys.WEBSERVICE_AFTER_AUTH_REDIRECT_TO);

		webserviceMacKey = Base64.getDecoder().decode(readConfigStr(config, ConfigKeys.WEBSERVICE_MAC_KEY));
		webserviceSystemName = readConfigStr(config, ConfigKeys.WEBSERVICE_SYSTEM_NAME);

		mainGroup = readConfigInt(config, ConfigKeys.TS_MAIN_GROUP, 1);

		onJoinAddTo = new HashSet<>();
		onJoinRemoveFrom = new HashSet<>();
		onLeaveAddTo = new HashSet<>();
		onLeaveRemoveFrom = new HashSet<>();

		onJoinAddTo.addAll(splitStringToLongList(readConfigStr(config, ConfigKeys.TS_ON_JOIN_ADD_TO_GROUPS)));
		onJoinRemoveFrom
				.addAll(readLongListWithAllOption(readConfigStr(config, ConfigKeys.TS_ON_JOIN_REMOVE_FROM_GROUPS)));
		onLeaveAddTo.addAll(splitStringToLongList(readConfigStr(config, ConfigKeys.TS_ON_LEAVE_ADD_TO_GROUPS)));
		onLeaveRemoveFrom
				.addAll(readLongListWithAllOption(readConfigStr(config, ConfigKeys.TS_ON_LEAVE_REMOVE_FROM_GROUPS)));

		onJoinAddTo = Collections.unmodifiableSet(onJoinAddTo);
		onJoinRemoveFrom = Collections.unmodifiableSet(onJoinRemoveFrom);
		onLeaveAddTo = Collections.unmodifiableSet(onLeaveAddTo);
		onLeaveRemoveFrom = Collections.unmodifiableSet(onLeaveRemoveFrom);

		httpServerAddress = readConfigStr(config, ConfigKeys.HTTP_SERVER_ADDRESS);
		httpServerPort = readConfigInt(config, ConfigKeys.HTTP_SERVER_PORT, 1);

		authMsgGroups = new HashSet<>();
		authMsgGroups.addAll(splitStringToLongList(readConfigStr(config, ConfigKeys.TS_AUTH_MESSAGE_GROUPS)));
		authMsgGroups = Collections.unmodifiableSet(authMsgGroups);
		authMsgText = readConfigStr(config, ConfigKeys.TS_AUTH_MESSAGE_TEXT);

		return true;
	}

	private List<Long> readLongListWithAllOption(final @CheckForNull String str) {
		final String ALL_OPTION = "all";
		if (ALL_OPTION.equalsIgnoreCase(str)) {
			return Collections.singletonList(-1L);
		}
		return splitStringToLongList(str);
	}

	private List<Long> splitStringToLongList(@CheckForNull String str) {
		List<Long> result = new LinkedList<>();
		if (str == null) {
			return result;
		}
		str = str.replace(" ", "");
		for (String value : str.split(",")) {
			value = value.trim();
			if (value.isEmpty()) {
				continue;
			}

			result.add(Long.parseLong(value));
		}
		return result;
	}

	@Override
	public void setListModes(BitSet listOptions) {
		// Nothing to do, default list options are enough
	}

	public final @Nonnull String getPrefix() {
		return prefix;
	}

	public final @Nonnull URI getWebserviceUrl() {
		return URI.create(webserviceUrl.toASCIIString());
	}

	public final @Nonnull int getCheckInterval() {
		return checkInterval;
	}

	public final @Nonnull int getMainGroup() {
		return mainGroup;
	}

	public final @Nonnull Set<Long> getOnJoinAddTo() {
		return onJoinAddTo;
	}

	public final @Nonnull Set<Long> getOnJoinRemoveFrom() {
		return onJoinRemoveFrom;
	}

	public final @Nonnull Set<Long> getOnLeaveAddTo() {
		return onLeaveAddTo;
	}

	public final @Nonnull Set<Long> getOnLeaveRemoveFrom() {
		return onLeaveRemoveFrom;
	}

	public final @Nonnull String getWebserviceSystemName() {
		return webserviceSystemName;
	}

	public final @Nonnull Key getWebserviceMacKey() {
		return new SecretKeySpec(webserviceMacKey, macAlgorithm());
	}

	public final @Nonnull String getWebserviceAfterAuthRedirectTo() {
		return redirectAfterAuthTo;
	}

	public String macAlgorithm() {
		return "HmacSHA256";
	}

	public String httpServerAddress() {
		return httpServerAddress;
	}

	public int httpServerPort() {
		return httpServerPort;
	}

	public @Nonnull Set<Long> getAuthMsgGroups() {
		return authMsgGroups;
	}

	public @CheckForNull String getAuthMsgText() {
		return authMsgText;
	}
}
