package net.mcwarlords.wlplugin.util;

import net.mcwarlords.wlplugin.Utils;

public class BoundingBox {
  public Point tl; /** Top left */
  public Point br; /** Bottom right */

  public BoundingBox(Point tl, Point br) {
    this.tl = tl;
    this.br = br;
  }

  public Point center() {
    return new Point(tl.x+(br.x-tl.x)/2, tl.y+(br.y-tl.y)/2);
  }

  public Point randomWithin() {
    return new Point(Utils.randInt(tl.x, br.x), Utils.randInt(tl.y, br.y));
  }
}
