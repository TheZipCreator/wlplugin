package net.mcwarlords.wlplugin;
/** Represents a single module of wlplugin */
// NOTE: don't use anymore.
interface Module {
	/** What to do when the module is enabled */
	fun onEnable();
	/** What to do when the module is disabled */
	fun onDisable();
}

interface SimpleModule {
	fun onEnable(); // called on enabled
	fun onDisable(); // called on disabled
}
