package customskull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Base64;
import java.util.Scanner;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

public class CustomSkull extends JavaPlugin {
	
	private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";
	private static final String TEXTURE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		reloadConfig();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("customskull")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("[CustomSkull] Este comando so pode ser usado em jogo.");
				return false;
			}
			Player p = (Player) sender;
			if (!(p.hasPermission("customskull.use"))) {
				p.sendMessage("\u00a7cVoce nao possui permissao.");
				return false;
			}
			if (args.length == 0) {
				p.sendMessage("\u00a7c/customskull save \u00a7f - Salva uma textura\n"
							+ "\u00a7c/customskull get \u00a7f- Obtem uma custom skull");
				return false;
			}
			if (args[0].equalsIgnoreCase("save")) {
				if (args.length < 3) {
					p.sendMessage("\u00a7cUse: /customskull save <Nick> <Id>");
					return false;
				}
				String player = args[1];
				String id = args[2].toLowerCase();
				if (!(player.matches("[A-Za-z0-9_]{3,16}"))) {
					p.sendMessage("\u00a7cNome de jogador invalido.");
					return false;
				}
				if (getConfig().isSet(id)) {
					p.sendMessage("\u00a7cJa existe uma textura salva com este id.");
					return false;
				}
				String uuid = getUUID(player);
				if (uuid == null) {
					p.sendMessage("\u00a7cFalha ao obter o UUID. Tente novamente mais tarde.");
					return false;
				}
				String texture = getTexture(uuid);
				if (texture == null) {
					p.sendMessage("\u00a7cFalha ao obter textura. Tente novamente mais tarde.");
					return false;
				}
				getConfig().set(id, texture);
				saveConfig();
				p.sendMessage("\u00a7aTextura do jogador \u00a7f" + player + " \u00a7asalva com id \u00a7f" + id);
				
			} else if (args[0].equalsIgnoreCase("get")) {
				if (args.length < 2) {
					p.sendMessage("\u00a7cUse: /customskull get <Id>");
					return false;
				}
				String id = args[1].toLowerCase();
				if (!(getConfig().isSet(id))) {
					p.sendMessage("\u00a7cNao existe uma textura salva com este id.");
					return false;
				}
				String url = getConfig().getString(id);
				
				ItemStack skull = getSkull(url);
				p.getInventory().addItem(skull);
				
				p.sendMessage("\u00a7aObtida com sucesso skull \u00a7f" + id);
			}
		}
		return true;
	}
	
	private ItemStack getSkull(String url) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        
        if (url == null || url.isEmpty())
            return skull;
        
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
        profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
        
        Field profileField = null;
        try {
            profileField = skullMeta.getClass().getDeclaredField("profile");
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
        
        profileField.setAccessible(true);
        
        try {
            profileField.set(skullMeta, profile);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        
        skull.setItemMeta(skullMeta);
        
        return skull;
    }
	
	private String getUUID(String nick) {
		try {
			InputStream stream = new URL(UUID_URL + nick).openStream();
			
			Scanner scanner = new Scanner(stream);
			String response = scanner.next();
			scanner.close();
			
			JsonElement json = new JsonParser().parse(response);
			
			return json.getAsJsonObject().get("id").getAsString();
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private String getTexture(String uuid) {
		try {
			InputStream stream = new URL(TEXTURE_URL + uuid).openStream();
			
			Scanner scanner = new Scanner(stream);
			String response = "";
			while (scanner.hasNext())
				response += scanner.next();
			scanner.close();
			
			JsonParser parser = new JsonParser();
			
			JsonElement json = parser.parse(response);
			
			JsonArray properties = json.getAsJsonObject().get("properties").getAsJsonArray();
			String encoded = properties.get(0).getAsJsonObject().get("value").getAsString();
			
			String decoded = new String(Base64.getDecoder().decode(encoded));
			json = parser.parse(decoded);
			
			JsonObject textures = json.getAsJsonObject().get("textures").getAsJsonObject();
			
			return textures.get("SKIN").getAsJsonObject().get("url").getAsString();
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
