package net.mcwarlords.wlplugin.schema;

import java.nio.file.*;

import net.mcwarlords.wlplugin.*;

public class SchemaModule implements Module {
  @Override public void onEnable() {
    WlPlugin.info("wlschema enabled");
    if(!Files.exists(Paths.get("plugins/FastAsyncWorldEdit"))) {
      WlPlugin.warn("FAWE is not installed. In order for wlschema to work, please install FAWE.");
      return;
    }
    WlPlugin.addCommand("wlschema", new SchemaCommand());
  }

  @Override public void onDisable() {
    WlPlugin.info("wlschema disabled");
  }
}
