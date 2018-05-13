package eu._4fh.tsgroupguildsync.data;

import java.util.Objects;

import javax.xml.bind.annotation.XmlRootElement;

import edu.umd.cs.findbugs.annotations.NonNull;

@XmlRootElement
public class RemoteCommand {
	public static enum Commands {
		ACC_UPDATE;
	}

	private long id;
	private long remoteAccountId;
	private @NonNull Commands command;

	public RemoteCommand(long id, long remoteAccountId, @NonNull Commands command) {
		Objects.requireNonNull(command);
		this.id = id;
		this.remoteAccountId = remoteAccountId;
		this.command = command;
	}

	// For JAX-RS
	@SuppressWarnings("unused")
	private RemoteCommand() {
		id = -1;
		remoteAccountId = -1;
		command = Commands.ACC_UPDATE;
	}

	public long getId() {
		return id;
	}

	public long getRemoteAccountId() {
		return remoteAccountId;
	}

	public @NonNull Commands getCommand() {
		return command;
	}

	@Override
	public String toString() {
		return String.valueOf(id) + " -> " + command.toString() + " for " + String.valueOf(remoteAccountId);
	}
}
