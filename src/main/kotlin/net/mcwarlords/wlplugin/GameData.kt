package net.mcwarlords.wlplugin;

import org.bukkit.Location;
import org.json.simple.*;

public class GameData(@JvmField val id: String) {
	@JvmField var name: String = "";
	@JvmField var author: String = ""; /** UUID */
	@JvmField var description: String = "No description set.";
	@JvmField var dateCreated: String = "unknown";
	@JvmField var location: Location = Location(null, 0.0, 0.0, 0.0);

	init {
		val games = Data.jsonServerData.get("games") as JSONObject;
		if(games.containsKey(id)) {
			val game = games.get(id) as JSONObject;
			if(game.containsKey("name"))
				name = game.get("name")!! as String;
			if(game.containsKey("author"))
				author = game.get("author")!! as String;
			if(game.containsKey("description"))
				description = game.get("description")!! as String;
			if(game.containsKey("location"))
				location = Utils.deserializeLocation(game.get("location"));
			if(game.containsKey("dateCreated"))
				dateCreated = game.get("dateCreated")!! as String;
		}
		else {
			name = id;
		}
	}

	fun toJSON(): JSONObject {
		val obj = JSONObject();
		obj.put("name", name);
		obj.put("author", author);
		obj.put("description", description);
		obj.put("dateCreated", dateCreated);
		obj.put("location", Utils.serializeLocation(location));
		return obj;
	}
}
