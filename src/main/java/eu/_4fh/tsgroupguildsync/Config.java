package eu._4fh.tsgroupguildsync;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import de.stefan1200.jts3servermod.BotConfigurationException;
import de.stefan1200.jts3servermod.interfaces.LoadConfiguration;
import de.stefan1200.util.ArrangedPropertiesWriter;
import edu.umd.cs.findbugs.annotations.NonNull;

public class Config implements LoadConfiguration {
	private static enum ConfigKeys {
		WEBSERVICE_URL("webserviceUrl", "URL to the webservice, for example https://my.server.com/guildsync/."),

		WEBSERVICE_API_KEY("webserviceApiKey", "API key for the webservice."),

		WEBSERVICE_MAC_KEY("webserviceMacKey", "MAC key for the webservice."),

		WEBSERVICE_SYSTEM_NAME("webserviceSystemName", "System name for the webservice.", "Voice"),

		WEBSERVICE_AFTER_AUTH_REDIRECT_TO("webserviceAfterAuthRedirectTo",
				"URL to redirect the user to after he is authed."),

		WEBSERVICE_CHECK_INTERVAL("checkInterval",
				"Interval to check the webservice (in minutes). Use 0 to disable the plugin.", "60"),

		WEBSERVICE_FULL_CHECK_INTERVAL("fullCheckInterval",
				"Interval to check group completly instead of only changed. Has to be a multiple of checkInterval.",
				String.valueOf(60 * 12)),

		WEBSERVICE_GUILD_ID("guildId", "Id of the guild on the webservice-side."),

		TS_OFFICER_GROUP("officerGroup", "The guild officer user group as id number."),

		TS_MAIN_GROUP("mainGroup", "The main TS user group for the guild as id number.\n"
				+ "If someone is not in this group but is in the guild according to the webservice he will be added to this group and onAdd* will be triggered.\n"
				+ "If someone is in this group but not in the guild according to the webservice he will be removed from this group and onRemove will be triggered."),

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

		TS_LOG_CHECK_INTERVAL("tsLog_checkInterval",
				"How often to check the server log for new group assignments in minutes.", "1", false),

		TS_LOG_LINE_FORMAT("tsLog_lineFormat", "Pattern used to parse server group add from teamspeak log.",
				"(?<date>\\S+ \\S+)\\.\\d+\\|.*client \\(id:(?<tuser>\\d+)\\) was added to servergroup '.*'\\(id:(?<tgroup>\\d+)\\) by client '.*'\\(id:(?<suser>\\d+)\\)",
				false),

		TS_LOG_DATE_PATTERN("tsLog_datePattern",
				"Pattern used to parse date from teamspeak log. Needs to be the format of the teamspeak logs.",
				"yyyy-MM-dd HH:mm:ss", false);

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
	private String webserviceApiKey;
	private byte[] webserviceMacKey;
	private String webserviceSystemName;
	private String redirectAfterAuthTo;
	private int checkInterval;
	private int fullCheckInterval;
	private int guildId;
	private int mainGroup;
	private int officerGroup;
	private @NonNull List<Integer> onJoinAddTo;
	private @NonNull List<Integer> onJoinRemoveFrom;
	private @NonNull List<Integer> onLeaveAddTo;
	private @NonNull List<Integer> onLeaveRemoveFrom;
	private long tsLogCheckInterval;
	private SimpleDateFormat tsLogDateFormat;
	private Pattern tsLogServerGroupAddPattern;

	public Config(final @NonNull SyncPlugin plugin, final @NonNull String prefix) {
		this.plugin = plugin;
		this.prefix = prefix;
		onJoinAddTo = new ArrayList<>();
		onJoinRemoveFrom = new ArrayList<>();
		onLeaveAddTo = new ArrayList<>();
		onLeaveRemoveFrom = new ArrayList<>();
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

		final int fullCheckIntervalTime = readConfigInt(config, ConfigKeys.WEBSERVICE_FULL_CHECK_INTERVAL, 1);
		if (fullCheckIntervalTime % checkInterval != 0) {
			throw new BotConfigurationException(
					"fullCheckInterval has to be a multiple of checkInterval, but fullCheckInterval is "
							+ fullCheckIntervalTime + " and checkInterval is " + checkInterval);
		}
		fullCheckInterval = fullCheckIntervalTime / checkInterval;

		webserviceUrl = URI.create(readConfigStr(config, ConfigKeys.WEBSERVICE_URL));
		redirectAfterAuthTo = readConfigStr(config, ConfigKeys.WEBSERVICE_AFTER_AUTH_REDIRECT_TO);

		webserviceApiKey = readConfigStr(config, ConfigKeys.WEBSERVICE_API_KEY);
		webserviceMacKey = Base64.getDecoder().decode(readConfigStr(config, ConfigKeys.WEBSERVICE_MAC_KEY));
		webserviceSystemName = readConfigStr(config, ConfigKeys.WEBSERVICE_SYSTEM_NAME);

		guildId = readConfigInt(config, ConfigKeys.WEBSERVICE_GUILD_ID, 1);

		mainGroup = readConfigInt(config, ConfigKeys.TS_MAIN_GROUP, 1);
		officerGroup = readConfigInt(config, ConfigKeys.TS_OFFICER_GROUP, 1);

		onJoinAddTo = new ArrayList<>();
		onJoinRemoveFrom = new ArrayList<>();
		onLeaveAddTo = new ArrayList<>();
		onLeaveRemoveFrom = new ArrayList<>();

		onJoinAddTo.addAll(splitStringToIntList(readConfigStr(config, ConfigKeys.TS_ON_JOIN_ADD_TO_GROUPS)));
		onJoinRemoveFrom
				.addAll(readIntListWithAllOption(readConfigStr(config, ConfigKeys.TS_ON_JOIN_REMOVE_FROM_GROUPS)));
		onLeaveAddTo.addAll(splitStringToIntList(readConfigStr(config, ConfigKeys.TS_ON_LEAVE_ADD_TO_GROUPS)));
		onLeaveRemoveFrom
				.addAll(readIntListWithAllOption(readConfigStr(config, ConfigKeys.TS_ON_LEAVE_REMOVE_FROM_GROUPS)));

		onJoinAddTo = Collections.unmodifiableList(onJoinAddTo);
		onJoinRemoveFrom = Collections.unmodifiableList(onJoinRemoveFrom);
		onLeaveAddTo = Collections.unmodifiableList(onLeaveAddTo);
		onLeaveRemoveFrom = Collections.unmodifiableList(onLeaveRemoveFrom);

		tsLogCheckInterval = readConfigInt(config, ConfigKeys.TS_LOG_CHECK_INTERVAL, 1);
		tsLogDateFormat = new SimpleDateFormat(readConfigStr(config, ConfigKeys.TS_LOG_DATE_PATTERN));
		tsLogServerGroupAddPattern = Pattern.compile(readConfigStr(config, ConfigKeys.TS_LOG_LINE_FORMAT),
				Pattern.UNIX_LINES);

		return true;
	}

	private List<Integer> readIntListWithAllOption(final @CheckForNull String str) {
		final String ALL_OPTION = "all";
		if (ALL_OPTION.equalsIgnoreCase(str)) {
			return Collections.singletonList(-1);
		}
		return splitStringToIntList(str);
	}

	private List<Integer> splitStringToIntList(final @CheckForNull String str) {
		List<Integer> result = new LinkedList<>();
		if (str == null) {
			return result;
		}
		for (String value : str.split(",")) {
			value = value.trim();
			if (value.isEmpty()) {
				continue;
			}

			result.add(Integer.parseInt(value));
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

	public final @Nonnull int getFullCheckInterval() {
		return fullCheckInterval;
	}

	public final @Nonnull int getGuildId() {
		return guildId;
	}

	public final @Nonnull int getMainGroup() {
		return mainGroup;
	}

	public final @Nonnull int getOfficerGroup() {
		return officerGroup;
	}

	public final @Nonnull List<Integer> getOnJoinAddTo() {
		return onJoinAddTo;
	}

	public final @Nonnull List<Integer> getOnJoinRemoveFrom() {
		return onJoinRemoveFrom;
	}

	public final @Nonnull List<Integer> getOnLeaveAddTo() {
		return onLeaveAddTo;
	}

	public final @Nonnull List<Integer> getOnLeaveRemoveFrom() {
		return onLeaveRemoveFrom;
	}

	public final @Nonnull String getWebserviceApiKey() {
		return webserviceApiKey;
	}

	public final @Nonnull String getWebserviceSystemName() {
		return webserviceSystemName;
	}

	public final @Nonnull byte[] getWebserviceMacKey() {
		return webserviceMacKey.clone();
	}

	public final @Nonnull String getWebserviceAfterAuthRedirectTo() {
		return redirectAfterAuthTo;
	}

	public final long getTsLogCheckInterval() {
		return tsLogCheckInterval;
	}

	public final SimpleDateFormat getTsLogDateFormat() {
		return tsLogDateFormat;
	}

	public final Pattern getTsLogServerGroupAddPattern() {
		return tsLogServerGroupAddPattern;
	}
}
