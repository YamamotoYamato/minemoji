package dev.tet.minemoji.slack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StandardEmojiDatasetTest {
	@Test
	void includesSlackStandardNames() {
		StandardEmojiDataset dataset = StandardEmojiDataset.getInstance();

		assertEquals("\uD83D\uDE47", dataset.unicodeForSlackName("bow"));
		assertEquals("\uD83D\uDE47\u200D\u2642\uFE0F", dataset.unicodeForSlackName("man-bowing"));
		assertTrue(dataset.entries().size() > 1_000);
	}
}
