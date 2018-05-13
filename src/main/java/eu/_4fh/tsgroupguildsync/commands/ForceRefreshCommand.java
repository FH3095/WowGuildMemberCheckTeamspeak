package eu._4fh.tsgroupguildsync.commands;

import java.util.List;

import javax.annotation.Nonnull;

import eu._4fh.tsgroupguildsync.SyncPlugin;

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
	public void executeCommand(final @Nonnull int senderId, final @Nonnull List<String> commandAndParameters,
			final @Nonnull SyncPlugin plugin) {
		String result = plugin.getSyncTask().startSync(true);
		plugin.getMod().sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId,
				result == null ? "Check finished" : result);
	}
}
