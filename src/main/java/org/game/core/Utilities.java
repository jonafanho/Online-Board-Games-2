package org.game.core;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Utilities {

	static <T> T parseUuid(String uuidString, Function<UUID, T> onSuccess, @Nullable Supplier<T> onError) {
		try {
			return onSuccess.apply(UUID.fromString(uuidString));
		} catch (Exception ignored) {
			return onError == null ? null : onError.get();
		}
	}

	static <T> T parseUuid(String uuidString1, String uuidString2, BiFunction<UUID, UUID, T> onSuccess, @Nullable Supplier<T> onError) {
		return parseUuid(uuidString1, uuid1 -> parseUuid(uuidString2, uuid2 -> onSuccess.apply(uuid1, uuid2), onError), onError);
	}

	static <T> T getElement(List<T> collection, int index) {
		return getElement(collection, index, null);
	}

	static <T> T getElement(@Nullable List<T> collection, int index, @Nullable T defaultValue) {
		final T result;
		if (collection == null || index >= collection.size() || index < -collection.size()) {
			result = null;
		} else {
			result = collection.get((index < 0 ? collection.size() : 0) + index);
		}
		return result == null ? defaultValue : result;
	}
}
