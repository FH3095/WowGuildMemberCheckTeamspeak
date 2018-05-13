package eu._4fh.tsgroupguildsync.commands;

import java.util.List;

import javax.annotation.Nonnull;

import eu._4fh.tsgroupguildsync.SyncPlugin;

public interface AbstractCommand {
	public @Nonnull String getCommandSyntax();

	public @Nonnull String getHelp();

	public @Nonnull String getCommandStart();

	public void executeCommand(final @Nonnull int senderId, final @Nonnull List<String> commandAndParameters,
			final @Nonnull SyncPlugin plugin);
}
