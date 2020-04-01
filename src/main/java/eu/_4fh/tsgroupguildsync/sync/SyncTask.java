package eu._4fh.tsgroupguildsync.sync;

import java.util.TimerTask;

import javax.annotation.Nonnull;
import javax.ws.rs.WebApplicationException;

import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import eu._4fh.tsgroupguildsync.SyncPlugin;

public class SyncTask {
	private static final Object syncObject = new Object();

	private final @Nonnull SyncPlugin plugin;

	public SyncTask(final @Nonnull SyncPlugin plugin) {
		this.plugin = plugin;
	}

	public void start() {
		final TimerTask task = new TimerTask() {
			@Override
			public void run() {
				runSync();
			}
		};
		plugin.getMod().addBotTimer(task, plugin.getConfig().getCheckInterval() * 60L * 1000L,
				plugin.getConfig().getCheckInterval() * 60L * 1000L);
	}

	public String runSync() {
		synchronized (syncObject) {
			try {
				new RestSync(plugin.getQuery(), plugin.getLog(), plugin.getConfig()).sync();
			} catch (TS3ServerQueryException | WebApplicationException e) {
				plugin.getLog().error("Cant sync server group with battle net", e);
				return "Cant sync: " + e.getMessage();
			}
		}
		return null;
	}
}
