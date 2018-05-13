package eu._4fh.tsgroupguildsync.commands;

import java.util.List;

import javax.annotation.Nonnull;
import javax.ws.rs.core.UriBuilder;

import eu._4fh.tsgroupguildsync.Config;
import eu._4fh.tsgroupguildsync.SyncPlugin;

public class GetAuthUrlCommand implements AbstractCommand {
	@Override
	public @Nonnull String getCommandSyntax() {
		return "GetAuthUrl <User-DB-Id>";
	}

	@Override
	public @Nonnull String getHelp() {
		return "Produces a URL to start an authentication process.";
	}

	@Override
	public @Nonnull String getCommandStart() {
		return "GetAuthUrl";
	}

	@Override
	public void executeCommand(final @Nonnull int senderId, final @Nonnull List<String> commandAndParameters,
			final @Nonnull SyncPlugin plugin) {
		if (commandAndParameters.size() < 2) {
			plugin.getMod().sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId, "Missing User-Db-Id");
			return;
		}
		String userDbIdStr = commandAndParameters.get(1);
		int userDbId;
		try {
			userDbId = Integer.parseInt(userDbIdStr);
		} catch (NumberFormatException e) {
			plugin.getMod().sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId,
					"User-DB-Id is not a number");
			return;
		}
		final Config config = plugin.getConfig();
		UriBuilder uri = UriBuilder.fromUri(config.getWebserviceUrl()).path("rest/auth/start")
				.queryParam("guildId", config.getGuildId()).queryParam("systemName", config.getWebserviceSystemName())
				.queryParam("remoteAccountId", userDbId)
				.queryParam("redirectTo", config.getWebserviceAfterAuthRedirectTo())
				.queryParam("mac", plugin.getRestHelper().calcMac(userDbId));
		plugin.getMod().sendMessageToClient(config.getPrefix(), "chat", senderId,
				"URL: [URL]" + uri.build().toASCIIString() + "[/URL]");
	}
}
