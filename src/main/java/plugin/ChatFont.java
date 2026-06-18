package plugin;

import org.bukkit.ChatColor;

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

	/** Rendered pixel width of {@code text} — skips §-colour/format codes and accounts for bold (§l, reset by §r/colours). */
	public static int width(String text) {
		int px = 0;
		boolean afterCode = false;
		boolean bold = false;
		for(char c : text.toCharArray()) {
			if(c == ChatColor.COLOR_CHAR) {
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
