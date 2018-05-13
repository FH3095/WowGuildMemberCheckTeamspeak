package eu._4fh.tsgroupguildsync;

import java.util.Collection;
import java.util.stream.Collectors;

public class Util {
	private Util() {
	}

	public static String join(Collection<? extends Object> collection, String delimiter) {
		return collection.stream().map(Object::toString).collect(Collectors.joining(delimiter));
	}
}
