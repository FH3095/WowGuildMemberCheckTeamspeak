package eu._4fh.tsgroupguildsync.sync;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import eu._4fh.tsgroupguildsync.Config;
import eu._4fh.tsgroupguildsync.Logger;
import eu._4fh.tsgroupguildsync.Util;
import eu._4fh.tsgroupguildsync.rest.RestHelper;

public class RestSync {
	private static final Object syncObject = new Object();

	private final @Nonnull Set<Long> currentGroupMembers;
	private final @Nonnull Set<Long> restMembers;
	private final @Nonnull Set<Long> toAdd;
	private final @Nonnull Set<Long> toDel;
	private final @Nonnull RestHelper restHelper;
	private final @Nonnull JTS3ServerQuery query;
	private final @Nonnull Logger log;
	private final @Nonnull Config config;

	public RestSync(final JTS3ServerQuery query, final Logger log, final @Nonnull Config config)
			throws TS3ServerQueryException {
		synchronized (syncObject) {
			toAdd = new HashSet<>();
			toDel = new HashSet<>();
			this.query = query;
			this.log = log;
			this.config = config;
			restHelper = new RestHelper(config);

			final List<HashMap<String, String>> mainGroupClients = query
					.getList(JTS3ServerQuery.LISTMODE_SERVERGROUPCLIENTLIST, "sgid=" + config.getMainGroup());
			currentGroupMembers = Collections.unmodifiableSet(mainGroupClients.stream()
					.map(map -> Long.parseLong(map.get("cldbid"))).collect(Collectors.toSet()));
			restMembers = Collections.unmodifiableSet(new HashSet<>(restHelper.getAllAccountIds()));
		}
	}

	public void syncAll() throws TS3ServerQueryException {
		synchronized (syncObject) {
			final Set<Long> newMembers = new HashSet<>(restMembers);
			newMembers.removeAll(currentGroupMembers);
			final Set<Long> noLongerMembers = new HashSet<>(currentGroupMembers);
			noLongerMembers.removeAll(restMembers);

			toAdd.addAll(newMembers);
			toDel.addAll(noLongerMembers);

			log.info("FullSync: Adding users " + Util.join(toAdd, ",") + " and removing " + Util.join(toDel, ","));

			doChanges();
		}
	}

	public boolean syncSingle(final long accountId) throws TS3ServerQueryException {
		synchronized (syncObject) {
			if (currentGroupMembers.contains(accountId) || !restMembers.contains(accountId)) {
				return false;
			}

			toAdd.add(accountId);
			toDel.clear();

			log.info("SingleSync: Adding users " + Util.join(toAdd, ",") + " and removing " + Util.join(toDel, ","));

			doChanges();

			return true;
		}
	}

	private void doChanges() throws TS3ServerQueryException {
		for (long userId : toAdd) {
			doChange(userId, false);
		}
		for (long userId : toDel) {
			doChange(userId, true);
		}
	}

	private void doChange(final @Nonnull long userId, final @Nonnull boolean userLeft) throws TS3ServerQueryException {
		doUserGroupChange(config.getMainGroup(), userId, userLeft ? true : false);

		final Set<Long> addToGroups = new HashSet<>(userLeft ? config.getOnLeaveAddTo() : config.getOnJoinAddTo());
		final Set<Long> removeFromGroups = new HashSet<>(
				userLeft ? config.getOnLeaveRemoveFrom() : config.getOnJoinRemoveFrom());
		final Set<Long> currentGroups = query
				.getList(JTS3ServerQuery.LISTMODE_SERVERGROUPSBYCLIENTID, "cldbid=" + userId).stream()
				.map(entry -> Long.parseLong(entry.get("sgid"))).collect(Collectors.toSet());

		addToGroups.removeAll(currentGroups); // Dont add to groups, the user is already member of
		if (removeFromGroups.contains(-1L)) {
			// User should be removed from all groups
			removeFromGroups.clear();
			removeFromGroups.addAll(currentGroups);
		} else {
			// Only remove from groups, the user is member of
			removeFromGroups.retainAll(currentGroups);
		}

		for (long groupId : removeFromGroups) {
			doUserGroupChange(groupId, userId, true);
		}
		for (long groupId : addToGroups) {
			doUserGroupChange(groupId, userId, false);
		}
	}

	private void doUserGroupChange(final @Nonnull long groupId, final @Nonnull long userId,
			final @Nonnull boolean remove) throws TS3ServerQueryException {
		final StringBuilder command = new StringBuilder();
		if (remove) {
			command.append("servergroupdelclient");
		} else {
			command.append("servergroupaddclient");
		}
		command.append(" sgid=").append(groupId).append(" cldbid=").append(userId);

		final Map<String, String> result = query.doCommand(command.toString());
		if (!result.get("id").equals("0")) {
			log.error("Cant change user group for " + userId + " from group " + groupId + " remove "
					+ Boolean.toString(remove) + " because " + String.valueOf(result.get("id")) + " "
					+ String.valueOf(result.get("msg")) + " ; " + String.valueOf(result.get("extra_msg")) + " ; "
					+ String.valueOf(result.get("failed_permid")));
			throw new TS3ServerQueryException(
					"doUserGroupChange(" + groupId + ", " + userId + ", " + Boolean.toString(remove), result.get("id"),
					result.get("msg"), result.get("extra_msg"), result.get("failed_permid"));
		}
	}
}
