package net.mcwarlords.wlplugin;
/** Represents a single module of wlplugin */
interface Module {
	/** What to do when the module is enabled */
	fun onEnable();
	/** What to do when the module is disabled */
	fun onDisable();
}
