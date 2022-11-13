package net.mcwarlords.wlplugin;
/** Represents a single module of wlplugin */
public interface Module {
  /** What to do when the module is enabled */
  public void onEnable();
  /** What to do when the module is disabled */
  public void onDisable();
}
