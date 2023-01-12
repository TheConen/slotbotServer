package de.webalf.slotbot.util.bot;

import jakarta.validation.constraints.NotBlank;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;

/**
 * @author Alf
 * @since 17.01.2021
 */
@UtilityClass
public final class DiscordUserUtils {
	/**
	 * Returns the private channel for the given user
	 *
	 * @return the matching private channel or null if it doesn't exist
	 */
	public static PrivateChannel getPrivateChannel(User user) {
		if (user != null && user.hasPrivateChannel()) {
			return user.openPrivateChannel().complete();
		}
		return null;
	}

	public static String getAvatarUrl(@NotBlank String id, String avatar, @NotBlank String discriminator) {
		if (avatar == null) {
			return getDefaultAvatarUrl(Short.parseShort(discriminator));
		}
		return "https://cdn.discordapp.com/avatars/" + id + "/" + avatar + (avatar.startsWith("a_") ? ".gif" : ".png");
	}

	private static String getDefaultAvatarUrl(short discriminator) {
		return "https://cdn.discordapp.com/embed/avatars/" + discriminator % 5 + ".png";
	}
}
