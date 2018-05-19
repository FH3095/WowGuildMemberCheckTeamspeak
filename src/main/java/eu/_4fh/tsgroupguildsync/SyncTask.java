package eu._4fh.tsgroupguildsync;

import java.net.MalformedURLException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.Vector;

import javax.annotation.Nonnull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import eu._4fh.tsgroupguildsync.data.RemoteCommand;
import eu._4fh.tsgroupguildsync.data.WowCharacter;
import eu._4fh.tsgroupguildsync.rest.RestHelper;

public class SyncTask {
	private final class SyncTimerTask extends TimerTask {
		private int numRuns = 0;

		@Override
		public void run() {
			boolean doFullSync = false;
			numRuns++;

			if (numRuns % plugin.getConfig().getFullCheckInterval() == 0) {
				doFullSync = true;
			}

			startSync(doFullSync);
		}
	}

	private final @Nonnull SyncPlugin plugin;

	public SyncTask(final @Nonnull SyncPlugin plugin) {
		this.plugin = plugin;
	}

	public void start() {
		plugin.getMod().addBotTimer(new SyncTimerTask(), plugin.getConfig().getCheckInterval() * 60L * 1000L,
				plugin.getConfig().getCheckInterval() * 60L * 1000L);
	}

	public synchronized String startSync(boolean doFullSync) {
		try {

			Client client = plugin.getRestHelper().getIgnoreSslClient();

			RestSyncTask restTask = new RestSyncTask(client);
			restTask.changesSync();
			if (doFullSync) {
				restTask.fullSync();
			}
			restTask.doChanges();
		} catch (TS3ServerQueryException | MalformedURLException | KeyManagementException | NoSuchAlgorithmException
				| WebApplicationException e) {
			plugin.getLog().error("Cant sync server group with battle net", e);
			return "Cant sync: " + e.getMessage();
		}
		return null;
	}

	private class RestSyncTask {
		private final @Nonnull Set<Integer> currentGroupMembers = new HashSet<>();
		private final @Nonnull List<Integer> toAdd = new ArrayList<>();
		private final @Nonnull List<Integer> toDel = new ArrayList<>();
		private final @Nonnull RestHelper restHelper = plugin.getRestHelper();
		private final @Nonnull Client client;

		public RestSyncTask(final @Nonnull Client client) throws TS3ServerQueryException {
			this.client = client;

			List<HashMap<String, String>> mainGroupClients;
			mainGroupClients = plugin.getQuery().getList(JTS3ServerQuery.LISTMODE_SERVERGROUPCLIENTLIST,
					"sgid=" + plugin.getConfig().getMainGroup());
			for (Map<String, String> mainGroupClient : mainGroupClients) {
				currentGroupMembers.add(Integer.parseInt(mainGroupClient.get("cldbid")));
			}
		}

		private void checkAccount(final @Nonnull int accId) {
			URI getCharsUri = restHelper.createUri("chars", "get").queryParam("remoteAccountId", accId).build();
			List<WowCharacter> chars = client.target(getCharsUri).request(MediaType.APPLICATION_JSON)
					.get(new GenericType<List<WowCharacter>>() {
					});
			if (chars.size() > 0 && !currentGroupMembers.contains(accId)) {
				toAdd.add(accId);
			} else if (chars.size() <= 0 && currentGroupMembers.contains(accId)) {
				toDel.add(accId);
			}
		}

		public void changesSync() throws MalformedURLException {
			URI getChangesUri = restHelper.createUri("changes", "get").build();
			List<RemoteCommand> commands = client.target(getChangesUri).request(MediaType.APPLICATION_JSON)
					.get(new GenericType<List<RemoteCommand>>() {
					});
			long maxId = 0;
			for (RemoteCommand command : commands) {
				int accId = (int) command.getRemoteAccountId();
				checkAccount(accId);
				if (maxId < command.getId()) {
					maxId = command.getId();
				}
			}
			URI resetChangesUri = restHelper.createUri("changes", "reset").queryParam("lastId", maxId).build();
			client.target(resetChangesUri).request().get();

			plugin.getLog().info("ChangesSync: Adding users " + Util.join(toAdd, ", ") + " ; Removing users "
					+ Util.join(toDel, ", "));
		}

		public void fullSync() throws TS3ServerQueryException {
			List<HashMap<String, String>> mainGroupClients;
			mainGroupClients = plugin.getQuery().getList(JTS3ServerQuery.LISTMODE_SERVERGROUPCLIENTLIST,
					"sgid=" + plugin.getConfig().getMainGroup());
			List<Integer> mainGroupClientIds = new ArrayList<>();
			for (Map<String, String> client : mainGroupClients) {
				mainGroupClientIds.add(Integer.parseInt(client.get("cldbid")));
			}

			for (int accId : mainGroupClientIds) {
				if (!toDel.contains(accId)) {
					checkAccount(accId);
				}
			}

			plugin.getLog().info("FullSync: Removing users " + Util.join(toDel, ", "));
		}

		public void doChanges() throws TS3ServerQueryException {
			for (int userId : toAdd) {
				doChange(userId, false);
			}
			for (int userId : toDel) {
				doChange(userId, true);
			}
		}

		private void doChange(final @Nonnull int userId, final @Nonnull boolean remove) throws TS3ServerQueryException {
			final Config config = plugin.getConfig();

			doUserGroupChange(config.getMainGroup(), userId, remove, true);

			final List<Integer> addToGroups = remove ? config.getOnLeaveAddTo() : config.getOnJoinAddTo();
			final List<Integer> removeFromGroups = remove ? config.getOnLeaveRemoveFrom()
					: config.getOnJoinRemoveFrom();
			for (int groupId : addToGroups) {
				doUserGroupChange(groupId, userId, false, false);
			}
			if (removeFromGroups.contains(-1)) {
				Vector<HashMap<String, String>> groups = plugin.getQuery()
						.getList(JTS3ServerQuery.LISTMODE_SERVERGROUPSBYCLIENTID, "cldbid=" + userId);
				for (HashMap<String, String> group : groups) {
					doUserGroupChange(Integer.parseInt(group.get("sgid")), userId, true, false);
				}
			} else {
				for (int groupId : removeFromGroups) {
					doUserGroupChange(groupId, userId, true, false);
				}
			}
		}

		private void doUserGroupChange(final @Nonnull int groupId, final @Nonnull int userId,
				final @Nonnull boolean remove, final @Nonnull boolean checkForError) throws TS3ServerQueryException {
			final StringBuilder command = new StringBuilder();
			if (remove) {
				command.append("servergroupdelclient");
			} else {
				command.append("servergroupaddclient");
			}
			command.append(" sgid=").append(groupId).append(" cldbid=").append(userId);

			Map<String, String> result = plugin.getQuery().doCommand(command.toString());
			if (!result.get("id").equals("0")) {
				if (checkForError) {
					throw new TS3ServerQueryException(
							"doUserGroupChange(" + groupId + ", " + userId + ", " + Boolean.toString(remove),
							result.get("id"), result.get("msg"), result.get("extra_msg"), result.get("failed_permid"));
				} else {
					plugin.getLog().info("Cant change user group for " + userId + " from group " + groupId + " remove "
							+ Boolean.toString(remove) + " because " + String.valueOf(result.get("id")) + " "
							+ String.valueOf(result.get("msg")) + " ; " + String.valueOf(result.get("extra_msg"))
							+ " ; " + String.valueOf(result.get("failed_permid")));
				}
			}
		}
	}
}
