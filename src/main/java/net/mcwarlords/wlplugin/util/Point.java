package net.mcwarlords.wlplugin.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class Point {
  public int x;
  public int y;

  public Point(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public Location toLocation() {
    return new Location(Bukkit.getWorlds().get(0), x, 70, y);
  }
}
