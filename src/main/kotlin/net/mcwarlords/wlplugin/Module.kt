package net.mcwarlords.wlplugin;

import org.json.simple.*;

import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.*;

import kotlin.annotation.*;
import kotlin.reflect.*;
import kotlin.reflect.full.*;

/** Represents a single module of wlplugin */
// NOTE: don't use anymore. (not gonna deprecate it quite yet)
interface Module {
	/** What to do when the module is enabled */
	fun onEnable();
	/** What to do when the module is disabled */
	fun onDisable();
}

/** Newer interface for modules */
interface SimpleModule : Module {
	val name: String;
	override fun onEnable() {
		enabled();
		WlPlugin.info("$name enabled.");
	}
	override fun onDisable() {
		disabled();
		WlPlugin.info("$name disabled.");
	}
	
	/** Called on enabled */
	fun enabled(); 	
	/** Called on disabled */
	fun disabled(); 
	/** Fields should be registered here. */
	fun registerFields(); 

}

@Target(AnnotationTarget.FUNCTION) annotation class SubCommand(val names: Array<String>, val description: String)
fun SubCommand.permission(name: String) = "wlplugin.$name.${names.maxBy { it.length }}";
@Target(AnnotationTarget.VALUE_PARAMETER) annotation class CommandPlayer;
@Target(AnnotationTarget.VALUE_PARAMETER) annotation class CommandName(val name: String);

interface ModuleCommand : CommandExecutor {
	val name: String;
	/** Must be set to the current class */
	val clazz: KClass<*>; 
	
	private inline fun forEachSubcommand(cb: (SubCommand, KFunction<*>) -> Unit) {
		// one liner!
		// remove non-subcommand functions and sort alphabetically
		val funcs = clazz.declaredFunctions.filter { it.findAnnotation<SubCommand>() != null }.sortedBy { it.findAnnotation<SubCommand>()!!.names.maxBy { it.length } };
		for(func in funcs) {
			val ann = func.findAnnotation<SubCommand>();
			if(ann == null)
				continue;
			cb(ann, func);
		}
	}

	fun sendHelpMessage(p: Player) {
    p.sendEscaped("&_s======[ &_e${name.uppercase()} &_s]======");
    p.sendEscaped("&_p/wlcode h | help &_s- &_dDisplays this help information.");
		forEachSubcommand { ann, func ->
			val msg = buildString {
				append("&_p/$name ${ann.names.joinToString(" | ")}");
				for(param in func.parameters) {
					if(param.kind == KParameter.Kind.INSTANCE)
						continue;
					if(param.findAnnotation<CommandPlayer>() != null)
						continue;
					val cn = param.findAnnotation<CommandName>();
					val name = if(cn == null) param.name else cn.name;
					val klass = param.type.classifier;
					if(klass != null && klass is KClass<*> && klass.isSubclassOf(Enum::class)) {
						append(if(param.isOptional) " [" else " <");
						append(klass.java.enumConstants.map { (it as Enum<*>).name.lowercase() }.joinToString(" | "));
						append(if(param.isOptional) "]" else ">");
					}
					else if(param.isOptional)
						append(" [$name]");
					else if(param.isVararg)
						append(" [$name...]");
					else
						append(" <$name>");
				}
				append("&_s - &_d${ann.description}");
			};
			p.sendEscaped(msg);
		}
	}

	fun complete(args: Array<String>): MutableList<String> {
		return mutableListOf();
	}

	fun invalidArguments(p: Player) {
		p.sendEscaped("&_p* &_dInvalid arguments.");
	}

  override fun onCommand(p: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
		if(p !is Player) {
      p.sendMessage("$name can only be used by a player.");
      return false;
		}
		if(args.size == 0 || args[0] == "h" || args[0] == "help") {
			sendHelpMessage(p);
			return true;
		}
		forEachSubcommand { ann, func ->
			if(ann.names.firstOrNull { it == args[0] } == null)
				return@forEachSubcommand;
			if(!p.hasPermission(ann.permission(name))) {
				p.sendEscaped("&_p* &_eYou do not have the required permission to run this command!");
				return false;
			}
			val map = mutableMapOf<KParameter, Any?>();
			var i = 1;
			fun takeArgument(type: KType): Any? {
				val a = args[i];
				i++;
				when(type.classifier) {
					String::class -> return a;
					Int::class -> {
						try {
							return a.toInt();
						} catch(e: NumberFormatException) {
							p.sendEscaped("&_p* &_dInvalid integer.");
							return null;
						}
					}
					Float::class -> {
						try {
							return a.toFloat();
						} catch(e: NumberFormatException) {
							p.sendEscaped("&_p* &_dInvalid float.");
							return null;
						}
					}
					else -> { 
						val klass = type.classifier;
						if(klass != null && klass is KClass<*> && klass.isSubclassOf(Enum::class)) {
							for(const in klass.java.enumConstants) {
								if((const as Enum<*>).name.equals(a, ignoreCase = true))
									return const;
							}
							p.sendEscaped("&_p* &_dInvalid option '$a'.");
						}
						throw Exception("Invalid type '$type' for argument of subcommand '${func.name}'")
					}
				}
			}
			for(param in func.parameters) {
				if(param.kind == KParameter.Kind.INSTANCE) {
					map[param] = clazz.cast(this);
					continue;
				}
				if(param.findAnnotation<CommandPlayer>() != null) {
					map[param] = p;
					continue;
				}
				if(i >= args.size) {
					if(param.isOptional)
						break;
					invalidArguments(p);
					return true;
				}
				if(param.isVararg) {
					val list = mutableListOf<Any>();
					val subtype = param.type.arguments[0].type!!;
					while(i < args.size) {
						val a = takeArgument(subtype);
						if(a == null)
							return true;
						list.add(a);
					}
					// reflection can be so ugly sometimes
					val arr = java.lang.reflect.Array.newInstance((subtype.classifier as KClass<*>).java, list.size);
					list.forEachIndexed { j, v -> java.lang.reflect.Array.set(arr, j, v) };
					map[param] = arr;
					continue;
				}
				val a = takeArgument(param.type);
				if(a == null)
					return true;
				map[param] = a;
			}
			if(i != args.size) {
				invalidArguments(p);
				return true;
			}
			func.callBy(map);
			return true;
		};
		p.sendEscaped("&_p* &_eInvalid subcommand.");
		return true;
	}

	fun register() {
		val argList = mutableListOf("h", "help");
		forEachSubcommand { ann, _ ->
			argList.addAll(ann.names);
		};
		WlPlugin.addCommand(name, this, object : TabCompleter {
			override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<String>): MutableList<String> {
				if(args.size == 1)
					return argList;
				forEachSubcommand { ann, func ->
					if(ann.names.contains(args[0])) {
						if(args.size >= func.parameters.size)
							return complete(args);
						val klass = func.parameters[args.size].type.classifier;
						if(klass != null && klass is KClass<*> && klass.isSubclassOf(Enum::class))
							return klass.java.enumConstants.map { (it as Enum<*>).name.lowercase() }.toMutableList();
						return complete(args);
					}
				}
				return complete(args);
			}
		});
	}
}

