package eu._4fh.tsgroupguildsync.commands;

import java.util.List;

import javax.annotation.Nonnull;

import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import eu._4fh.tsgroupguildsync.SyncPlugin;
import eu._4fh.tsgroupguildsync.rest.RestHelper;

public class AuthMeCommand implements AbstractCommand {
	@Override
	public @Nonnull String getCommandSyntax() {
		return "AuthMe [User-DB-Id]";
	}

	@Override
	public @Nonnull String getHelp() {
		return "Produces a URL to start an authentication process.";
	}

	@Override
	public @Nonnull String getCommandStart() {
		return "AuthMe";
	}

	@Override
	public void executeCommand(final @Nonnull int senderId, final @Nonnull List<String> commandAndParameters,
			final @Nonnull SyncPlugin plugin) {
		try {
			final RestHelper restHelper = new RestHelper(plugin.getConfig());
			final long senderDbId = Long.parseLong(
					plugin.getQuery().getInfo(JTS3ServerQuery.INFOMODE_CLIENTINFO, senderId).get("client_database_id"));

			if (commandAndParameters.size() > 2) {
				plugin.getMod().sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId,
						"Too many parameters");
				return;
			}

			final long userDbId;
			if (commandAndParameters.size() == 2) {
				if (!restHelper.isOfficer(senderDbId)) {
					plugin.getMod().sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId,
							"You are not an officer. Access denied.");
					return;
				} else {
					userDbId = Long.parseLong(commandAndParameters.get(1));
				}
			} else {
				userDbId = senderDbId;
			}

			plugin.getMod().sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId,
					"URL: [URL]" + restHelper.getAuthStartUrl(userDbId).toASCIIString() + "[/URL]");
		} catch (TS3ServerQueryException e) {
			plugin.getMod().sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId,
					"Error: " + e.getMessage());
		}
	}
}
