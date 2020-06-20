package eu._4fh.tsgroupguildsync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Util {
	private Util() {
	}

	public static String join(Collection<? extends Object> collection, String delimiter) {
		return collection.stream().map(String::valueOf).collect(Collectors.joining(delimiter));
	}

	public static <T> List<T> split(final String str, final String delimiter, final Function<String, T> converter) {
		final String[] parts = str.split(Pattern.quote(delimiter), 0);
		final List<T> result = new ArrayList<>(parts.length);
		for (String part : parts) {
			final String trimmedPart = part.trim();
			if (trimmedPart.isEmpty()) {
				continue;
			}
			result.add(converter.apply(trimmedPart));
		}
		return result;
	}
}
