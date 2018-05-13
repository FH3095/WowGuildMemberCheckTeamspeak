package eu._4fh.tsgroupguildsync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import javax.annotation.Nonnull;

import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TS3ServerQueryException;

public class OfficersCache {

	private final class RefreshOfficersTimerTask extends TimerTask {
		@Override
		public void run() {
			try {
				List<Integer> newList = new ArrayList<>();

				List<HashMap<String, String>> groupMembers = plugin.getQuery().getList(
						JTS3ServerQuery.LISTMODE_SERVERGROUPCLIENTLIST, "sgid=" + plugin.getConfig().getOfficerGroup());
				for (Map<String, String> groupMember : groupMembers) {
					newList.add(Integer.parseInt(groupMember.get("cldbid")));
				}

				officers = Collections.unmodifiableList(newList);
			} catch (TS3ServerQueryException e) {
				plugin.getLog().error("Cant get officer server group members", e);
			}
		}
	}

	private final @Nonnull SyncPlugin plugin;
	private volatile @Nonnull List<Integer> officers;

	public OfficersCache(final @Nonnull SyncPlugin plugin) {
		officers = new ArrayList<>();
		this.plugin = plugin;
	}

	public void start() {
		plugin.getMod().addBotTimer(new RefreshOfficersTimerTask(), 2 * 1000, 5 * 60 * 1000);
	}

	public List<Integer> getOfficersDbId() {
		return Collections.unmodifiableList(officers);
	}
}
