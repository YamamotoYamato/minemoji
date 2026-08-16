package dev.tet.minemoji.slack;

import java.util.Locale;

public record SlackEmoji(String name, String aliasOf, String value) {
	private static final String UNICODE_PREFIX = "unicode:";

	public SlackEmoji {
		aliasOf = aliasOf == null ? "" : aliasOf;
	}

	public boolean isAlias() {
		return !this.aliasOf.isBlank();
	}

	public boolean isUnicode() {
		return this.value.startsWith(UNICODE_PREFIX);
	}

	public String unicodeValue() {
		if (!this.isUnicode()) {
			return "";
		}
		return this.value.substring(UNICODE_PREFIX.length());
	}

	public String normalizedName() {
		return this.name.toLowerCase(Locale.ROOT);
	}

	public String displayText() {
		if (this.isAlias()) {
			return ":" + this.name + ": -> :" + this.aliasOf + ":";
		}
		return ":" + this.name + ":";
	}

	public static SlackEmoji unicode(String name, String unicodeValue) {
		return new SlackEmoji(name, "", UNICODE_PREFIX + unicodeValue);
	}
}
