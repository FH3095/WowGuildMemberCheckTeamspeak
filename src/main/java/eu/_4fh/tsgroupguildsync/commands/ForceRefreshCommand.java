package eu._4fh.tsgroupguildsync.commands;

import java.util.List;

import javax.annotation.Nonnull;

import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import eu._4fh.tsgroupguildsync.SyncPlugin;
import eu._4fh.tsgroupguildsync.sync.RestSync;

public class ForceRefreshCommand implements AbstractCommand {
	@Override
	public @Nonnull String getCommandSyntax() {
		return "ForceCheck";
	}

	@Override
	public @Nonnull String getHelp() {
		return "Forces a refresh of the guild members with the webservice.";
	}

	@Override
	public @Nonnull String getCommandStart() {
		return "ForceCheck";
	}

	@Override
	public void executeCommand(final @Nonnull int senderId, final boolean isAdmin,
			final @Nonnull List<String> commandAndParameters, final @Nonnull SyncPlugin plugin) {
		try {
			if (!isAdmin) {
				plugin.getMod().sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId,
						"You are not an officer. Access denied.");
				return;
			}

			new RestSync(plugin.getQuery(), plugin.getLog(), plugin.getConfig()).syncAll();
		} catch (TS3ServerQueryException e) {
			plugin.getMod().sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId,
					"Cant sync: " + e.getMessage());
			return;
		}
		plugin.getMod().sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId, "Sync finished");
	}
}
