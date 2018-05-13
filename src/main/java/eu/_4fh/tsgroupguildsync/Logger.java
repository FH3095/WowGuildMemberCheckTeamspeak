package eu._4fh.tsgroupguildsync;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

public class Logger {
	public static enum LogLevel {
		DEBUG(JTS3ServerMod_Interface.ERROR_LEVEL_DEBUG), INFO(JTS3ServerMod_Interface.ERROR_LEVEL_INFO), WARN(
				JTS3ServerMod_Interface.ERROR_LEVEL_WARNING), ERROR(
						JTS3ServerMod_Interface.ERROR_LEVEL_ERROR), CRIT(JTS3ServerMod_Interface.ERROR_LEVEL_CRITICAL);
		private final @NonNull byte level;

		private LogLevel(final @NonNull byte level) {
			this.level = level;
		}
	}

	private @CheckForNull JTS3ServerMod_Interface mod = null;
	private @NonNull String prefix = "yetUnknown";

	public void init(final @NonNull JTS3ServerMod_Interface mod, final @CheckForNull String prefix) {
		this.mod = mod;
		this.prefix = String.valueOf(prefix); // "null" for null-prefix
	}

	public void debug(final @NonNull String msg) {
		log(LogLevel.DEBUG, msg);
	}

	public void info(final @NonNull String msg) {
		log(LogLevel.INFO, msg);
	}

	public void warn(final @NonNull String msg) {
		log(LogLevel.WARN, msg);
	}

	public void error(final @NonNull String msg) {
		log(LogLevel.ERROR, msg);
	}

	public void crit(final @NonNull String msg) {
		log(LogLevel.CRIT, msg);
	}

	public void debug(final @NonNull String msg, final @NonNull Throwable e) {
		log(LogLevel.DEBUG, msg, e);
	}

	public void info(final @NonNull String msg, final @NonNull Throwable e) {
		log(LogLevel.INFO, msg, e);
	}

	public void warn(final @NonNull String msg, final @NonNull Throwable e) {
		log(LogLevel.WARN, msg, e);
	}

	public void error(final @NonNull String msg, final @NonNull Throwable e) {
		log(LogLevel.ERROR, msg, e);
	}

	public void crit(final @NonNull String msg, final @NonNull Throwable e) {
		log(LogLevel.CRIT, msg, e);
	}

	public void log(final @NonNull LogLevel level, final @NonNull String msg) {
		if (mod == null) {
			System.out.println(level.toString() + ": " + msg);
			return;
		}

		boolean toSysout = false;
		if (LogLevel.CRIT.equals(level) || LogLevel.ERROR.equals(level)) {
			toSysout = true;
		}
		mod.addLogEntry(prefix, level.level, msg, toSysout);
	}

	public void log(final @NonNull LogLevel level, final @NonNull String msg, final @NonNull Throwable e) {
		try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
			pw.println(msg);
			e.printStackTrace(pw);
			pw.flush();
			log(level, sw.toString());
		} catch (IOException e1) {
			crit(e1.getMessage());
		}
	}
}
