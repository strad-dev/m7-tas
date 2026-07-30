package plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft default-font pixel metrics for measuring and centering chat lines. Widths match the vanilla
 * GUI font: each glyph's width plus 1px of inter-character spacing, with bold adding 1px per glyph (except
 * the space). A standard chat box renders {@value #MAX_WIDTH}px wide.
 */
public final class ChatFont {
	private ChatFont() {}

	/** Rendered width of a standard (unscaled) chat line. */
	public static final int MAX_WIDTH = 320;

	/** Wrap threshold for packing lines — a 10px buffer under {@link #MAX_WIDTH} so the client never wraps a line itself. */
	public static final int WRAP_WIDTH = 310;

	/** The legacy section sign (§) that prefixes colour/format codes. */
	private static final char SECTION_CHAR = '§';

	/** Rendered pixel width of {@code text} — skips §-colour/format codes and accounts for bold (§l, reset by §r/colours). */
	public static int width(String text) {
		int px = 0;
		boolean afterCode = false;
		boolean bold = false;
		for(char c : text.toCharArray()) {
			if(c == SECTION_CHAR) {
				afterCode = true;
				continue;
			}
			if(afterCode) {
				afterCode = false;
				char l = Character.toLowerCase(c);
				if(l == 'l') bold = true;
				else if(l == 'r' || (l >= '0' && l <= '9') || (l >= 'a' && l <= 'f')) bold = false;
				// k/m/n/o don't affect width
				continue;
			}
			int advance = glyphWidth(c) + 1; // glyph + 1px spacing
			if(bold && c != ' ') advance += 1;
			px += advance;
		}
		return px;
	}

	/**
	 * {@code text} word-wrapped at {@link #WRAP_WIDTH} and each resulting line {@link #centerPad}ed — so a line
	 * whose rendered width isn't known up front (a player name, a live number) still sits centered instead of
	 * carrying hardcoded padding. Text short enough for one line comes back as a single entry.
	 * <p>Measured with {@link #width}, so pass plain text or a legacy §-string; MiniMessage tags would be counted
	 * as literal characters. A single word wider than {@link #WRAP_WIDTH} gets its own line and overflows it.
	 */
	public static List<String> centerLines(String text) {
		List<String> lines = new ArrayList<>();
		if(text == null || text.isBlank()) return lines;
		StringBuilder line = new StringBuilder();
		for(String word : text.trim().split("\\s+")) {
			if(line.isEmpty()) {
				line.append(word);
			} else if(width(line + " " + word) <= WRAP_WIDTH) {
				line.append(' ').append(word);
			} else {
				lines.add(centerPad(line.toString()));
				line.setLength(0);
				line.append(word);
			}
		}
		if(!line.isEmpty()) lines.add(centerPad(line.toString()));
		return lines;
	}

	/** Leading spaces that center {@code text} within {@link #MAX_WIDTH}, then the text. */
	public static String centerPad(String text) {
		int toCompensate = (MAX_WIDTH / 2) - (width(text) / 2);
		int spaceAdvance = glyphWidth(' ') + 1; // 4px
		StringBuilder pad = new StringBuilder();
		for(int compensated = 0; compensated < toCompensate; compensated += spaceAdvance) {
			pad.append(' ');
		}
		return pad + text;
	}

	/** Base glyph width (without the trailing spacing pixel) of a character in the vanilla font. */
	private static int glyphWidth(char c) {
		return switch(c) {
			case 'i', '!', '.', ',', ':', ';', '|', '\'' -> 1;
			case 'l', '`' -> 2;
			case ' ', 'I', 't', '[', ']', '"' -> 3;
			case 'f', 'k', '(', ')', '{', '}', '<', '>' -> 4;
			case '@', '~' -> 6;
			default -> 5;
		};
	}
}
