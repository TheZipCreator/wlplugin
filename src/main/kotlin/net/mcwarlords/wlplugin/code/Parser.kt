package net.mcwarlords.wlplugin.code;

import org.bukkit.*;

// generic class for ASTs
abstract class CodeTree(val location: Location);

class CodeEvent(l: Location, val name: String, val children: List<CodeTree>) : CodeTree(l);
class CodeIf(l: Location, val cond: CodeTree, val success: CodeTree, val failure: CodeTree? = null) : CodeTree(l);
class CodeBuiltin(l: Location, val name: String, val args: List<CodeTree>) : CodeTree(l);
