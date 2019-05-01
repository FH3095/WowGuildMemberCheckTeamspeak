package eu._4fh.tsgroupguildsync.commands;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.UriBuilder;

import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;
import eu._4fh.tsgroupguildsync.SyncPlugin;
import eu._4fh.tsgroupguildsync.data.WowCharacter;

public class AddCommand implements AbstractCommand {

	final @Nonnull Pattern serverNamePattern = Pattern.compile("^[\\p{L}]+$",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	@Override
	public String getCommandSyntax() {
		return "Add <TS-Account-ID> <Charname> <Server>";
	}

	@Override
	public String getHelp() {
		return "Adds a TS-user to the webservice";
	}

	@Override
	public String getCommandStart() {
		return "Add";
	}

	@Override
	public void executeCommand(int senderId, List<String> commandAndParameters, SyncPlugin plugin) {
		final @Nonnull JTS3ServerMod_Interface mod = plugin.getMod();

		if (commandAndParameters.size() == 3) {
			final @Nonnull String[] charnameAndServer = commandAndParameters.get(2).split("-", 2);
			if (charnameAndServer.length == 2) {
				commandAndParameters.set(2, charnameAndServer[0]);
				commandAndParameters.add(3, charnameAndServer[1]);
			}
		}
		if (commandAndParameters.size() < 4) {
			mod.sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId, "Invalid command syntax");
			return;
		}

		final int userDbId;
		try {
			userDbId = Integer.parseInt(commandAndParameters.get(1));
		} catch (NumberFormatException e) {
			mod.sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId, "User-DB-Id is not a number");
			return;
		}
		if (!serverNamePattern.matcher(commandAndParameters.get(3)).matches()) {
			mod.sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId,
					"Server-Name is invalid. Only letters are allowed (no whitespaces!)");
			return;
		}

		final @Nonnull WowCharacter character = new WowCharacter(commandAndParameters.get(2),
				commandAndParameters.get(3), Short.MAX_VALUE, Calendar.getInstance());

		UriBuilder wsUri = plugin.getRestHelper().createUri("chars", "add").queryParam("remoteAccountId", userDbId);
		try {
			final @Nonnull Client client = plugin.getRestHelper().getIgnoreSslClient();
			try (Response resp = client.target(wsUri.build()).request().post(Entity.json(character))) {
				if (!Family.SUCCESSFUL.equals(resp.getStatusInfo().getFamily())) {
					mod.sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId, "Cant add character: "
							+ resp.getStatusInfo().toString() + ": " + resp.readEntity(String.class));
					return;
				}
			}
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			mod.sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId,
					"Cant add character: " + e.getMessage());
			return;
		}

		plugin.getSyncTask().startSync(false);
		plugin.getMod().sendMessageToClient(plugin.getConfig().getPrefix(), "chat", senderId,
				"Added character " + character.toString());
	}
}
