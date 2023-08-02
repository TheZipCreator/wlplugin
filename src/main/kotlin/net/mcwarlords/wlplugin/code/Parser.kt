package net.mcwarlords.wlplugin.code;

import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.block.*;
import org.bukkit.block.data.type.*;
import org.bukkit.block.data.*;
import org.bukkit.block.sign.*;
import org.bukkit.block.Sign;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.block.Barrel;

import net.mcwarlords.wlplugin.*;

internal typealias KList<T> = kotlin.collections.List<T>;
internal typealias KString = kotlin.String;

// class for ASTs
sealed class Tree(val loc: Location) {
	class Do(l: Location, val children: KList<Tree>) : Tree(l);
	class Event(l: Location, val name: String, val children: KList<Tree>) : Tree(l);
	class If(l: Location, val conds: KList<Tree>, val actions: KList<Tree>, val failure: Tree?) : Tree(l);
	class For(l: Location, val varName: String, val list: Tree, val body: KList<Tree>) : Tree(l);
	class Builtin(l: Location, val name: String, val args: KList<Tree>) : Tree(l);
	class Token(l: Location, val token: net.mcwarlords.wlplugin.code.Token) : Tree(l);
	class List(l: Location, val list: KList<Tree>) : Tree(l);
	class Declare(l: Location, val name: String, val value: Tree) : Tree(l);
	class Set(l: Location, val name: String, val value: Tree) : Tree(l);
	class Map(l: Location, val keys: KList<Tree>, val values: KList<Tree>) : Tree(l);
	class Return(l: Location, val value: Tree) : Tree(l);
	class While(l: Location, val cond: Tree, val body: KList<Tree>) : Tree(l);
	
	// this isn't used anywhere, so I've decided to remove for now (so I don't have to bother updating it again)
	// override fun toString() = when(this) {
	// 	is Event -> "event '$name' { ${children.joinToString(" ")} }"
	// 	is If -> "if { ${buildString { 
	// 		for(i in conds.indices) { 
	// 			append(conds[i]); 
	// 			append(" ");
	// 			append(actions[i]);
	// 		}
	// 		if(failure != null) {
	// 			append(" ");
	// 			append(failure);
	// 		}
	// 	}} } "
	// 	is Do -> "${children.joinToString(" ")}"
	// 	is Builtin -> "builtin '$name' { ${args.joinToString(" ")} }"
	// 	is Token -> token.toString();
	// 	is Declare -> "declare { $name $value }"
	// 	is Set -> "set { $name $value }"
	// 	is List -> "list { ${list.joinToString(" ")} }"
	// 	is For -> "for($varName in $list) { ${body.joinToString(" ")} }"
	// 	is Map -> "map { ${buildString { 
	// 		for(i in keys.indices) { 
	// 			append(keys[i]); 
	// 			append(" ");
	// 			append(values[i]);
	// 		}
	// 	}} } "
	// 	is Return -> "return $value"
	// }

	fun name() = when(this) {
		is Do -> "do"
		is Event -> "event"
		is If -> "if"
		is Builtin -> "builtin"
		is Token -> token.name()
		is Declare -> "declare"
		is Set -> "set"
		is List -> "list"
		is For -> "for"
		is Map -> "map"
		is Return -> "return"
		is While -> "while"
	}
}


sealed class Token(val loc: Location) {
	class If(l: Location) : Token(l);
	class Lbrack(l: Location) : Token(l);
	class Rbrack(l: Location) : Token(l);
	class Event(l: Location, val name: KString) : Token(l);
	class Builtin(l: Location, val name: KString) : Token(l);
	class Do(l: Location) : Token(l);
	class For(l: Location) : Token(l);
	class Variable(l: Location, val name: KString) : Token(l);
	class String(l: Location, val string: KString) : Token(l);
	class Number(l: Location, val num: Double) : Token(l);
	class Bool(l: Location, val bool: Boolean): Token(l);
	class Parameter(l: Location, val name: KString): Token(l);
	class Item(l: Location, val item: ItemStack): Token(l)
	class List(l: Location): Token(l);
	class Map(l: Location) : Token(l);
	class Declare(l: Location, val name: KString): Token(l);
	class Set(l: Location, val name: KString): Token(l);
	class Unit(l: Location) : Token(l)
	class Return(l: Location) : Token(l)
	class Loc(l: Location, val location: Location) : Token(l);
	class While(l: Location) : Token(l);
	class Break(l: Location) : Token(l);
	class Continue(l: Location) : Token(l);

	override fun toString(): kotlin.String = when(this) {
		is If -> "if"
		is Do -> "do"
		is Lbrack -> "left bracket"
		is Rbrack -> "right bracket"
		is Event -> "event '$name'"
		is Builtin -> "builtin '$name'"
		is Variable -> "variable '$name'"
		is Parameter -> "parameter '$name'"
		is String -> "string '$string'"
		is Number -> "number '$num'"
		is Bool -> if(bool) "true" else "false"
		is Item -> "item '$item'"
		is Declare -> "declare '$name'"
		is Set -> "set '$name'"
		is List -> "list"
		is For -> "for"
		is Map -> "map"
		is Unit -> "unit"
		is Return -> "return"
		is Loc -> "loc"
		is While -> "while"
		is Break -> "break"
		is Continue -> "continue"
	}

	fun name(): kotlin.String = when(this) {
		is If -> "if"
		is Do -> "do"
		is Lbrack -> "left bracket"
		is Rbrack -> "right bracket"
		is Event -> "event"
		is Builtin -> "builtin"
		is Variable -> "variable"
		is Parameter -> "parameter"
		is String -> "string"
		is Number -> "number"
		is Bool -> if(bool) "true" else "false"
		is Item -> "item"
		is Declare -> "declare"
		is Set -> "set"
		is List -> "list"
		is For -> "for"
		is Map -> "map"
		is Unit -> "unit"
		is Return -> "return"
		is Loc -> "loc"
		is While -> "while"
		is Break -> "break"
		is Continue -> "continue"
	}
}

open class CodeException(val loc: Location?, msg: String) : Exception(msg) {
	// returns a string that should be Utils.escapeText()'d
	fun toChatString() = "&_p[${loc?.x}, ${loc?.y}, ${loc?.z}] &_e$message";
}

class ParseException(loc: Location?, msg: String) : CodeException(loc, msg);

class Parser(var loc: Location) {
	val startZ = loc.z;

	companion object {
		fun parse(l: Location): Tree {
			return Parser(l).parse();
		}
	}

	private var eof = false;
	
	// returns null if EOF has been reached
	private fun nextBlock(): Block? {
		if(eof)
			return null;
		var b = loc.block;
		loc.z -= 1;
		if(startZ-loc.z >= 32) {
			loc.z = startZ;
			loc.y -= 1;
			if(loc.y < 64) {
				eof = true;
			}
		}
		return b;
	}

	private var nextTokens: MutableList<Token> = mutableListOf(); // cached tokens

	private var lastLocation: Location? = null; // location of last block
	
	// returns null on EOF
	fun next(): Token? {
		if(nextTokens.size > 0)
			return nextTokens.removeAt(0)
		// skip past whitespace and comments
		while(true) {
			var b = nextBlock();
			if(b == null)
				return null;
			fun readSign(): String {
				var lines = (b!!.getRelative(BlockFace.EAST).state as Sign).lines;
				return lines.slice(1..lines.size-1).joinToString("");
			}
			if(b.type != Material.REDSTONE_LAMP && b.type != Material.BLACK_STAINED_GLASS) {
				val loc = b.location;
				lastLocation = loc;
				when(b.type) {
					Material.PISTON -> {
						if((b.blockData as Directional).facing == BlockFace.NORTH)
							return Token.Lbrack(loc);
						else
							return Token.Rbrack(loc);
					}
					Material.DIAMOND_BLOCK -> {
						var s = readSign();
						if(!validEvents.contains(s))
							throw ParseException(loc, "Invalid event '$s'.");
						return Token.Event(loc, s);
					}
					Material.FURNACE -> return Token.Builtin(loc, readSign());
					Material.OAK_PLANKS -> return Token.If(loc);
					Material.MANGROVE_PLANKS -> return Token.Do(loc);
					Material.MAGMA_BLOCK -> return Token.For(loc);
					Material.NETHER_WART_BLOCK -> return Token.While(loc);
					Material.COAL_BLOCK -> return Token.Return(loc);
					Material.CHISELED_STONE_BRICKS -> return Token.Break(loc);
					Material.CHISELED_QUARTZ_BLOCK -> return Token.Continue(loc);
					Material.OBSIDIAN -> return Token.Variable(loc, readSign());
					Material.WHITE_WOOL -> return Token.String(loc, buildString {
						while(true) {
							append(readSign());
							if(b!!.getRelative(BlockFace.NORTH).type == Material.WHITE_WOOL)
								b = nextBlock()!!;
							else
								break;
						}
					});
					Material.LIME_TERRACOTTA -> return Token.Bool(loc, true);
					Material.RED_TERRACOTTA -> return Token.Bool(loc, false);
					Material.WAXED_COPPER_BLOCK -> {
						val n = readSign().toDoubleOrNull();
						if(n == null)
							throw ParseException(loc, "Invalid number.");
						return Token.Number(loc, n);
					}
					Material.PRISMARINE_BRICKS -> return Token.Parameter(loc, readSign());
					Material.BARREL -> {
						val inv = (b!!.state as Barrel).inventory;
						for(i in 0..inv.size-1) {
							val item = inv.getItem(i);
							if(item == null)
								continue;
							nextTokens.add(Token.Item(loc, item));
						}
						return next();
					}
					Material.TARGET -> {
						try {
							val s = readSign().split(" ").map { it.toDouble() };
							if(s.size == 3)
								return Token.Loc(loc, Location(loc.world, s[0], s[1], s[2]));
							if(s.size == 5)
								return Token.Loc(loc, Location(loc.world, s[0], s[1], s[2], s[3].toFloat(), s[4].toFloat()));
							throw ExecutionException(loc, "Invalid location.");
						} catch(e: NumberFormatException) {
							throw ExecutionException(loc, "Invalid location.");
						}
					}
					Material.END_STONE_BRICKS -> return Token.List(loc)
					Material.MUD_BRICKS -> return Token.Map(loc)
					Material.NETHERITE_BLOCK -> return Token.Unit(loc)
					Material.CRIMSON_HYPHAE -> return Token.Declare(loc, readSign())
					Material.WARPED_HYPHAE -> return Token.Set(loc, readSign())
					else -> throw ParseException(loc, "Unknown block type: ${b!!.type.toString()}")
				}
			}
		}
		
	}
	
	// peeks at the next token
	fun peek(): Token? {
		var tk = next();
		if(tk != null)
			nextTokens.add(tk)
		return tk;
	}

	private fun throwEOF(): Nothing {
		throw ParseException(lastLocation!!, "Unexpected end of unit.");
	}
	
	fun block(): List<Tree> {
		var list = mutableListOf<Tree>();
		run {
			var tk = next();
			if(tk == null)
				throwEOF();
			if(tk !is Token.Lbrack)
				throw ParseException(tk.loc, "Expected left brace, got $tk.");
		};
		while(true) {
			if(peek() is Token.Rbrack) {
				next(); // consume rbrace
				return list;
			}
			var tree = nextTree();
			if(tree == null)
				throwEOF();
			list.add(tree);
		}
	}

	fun nextTree(topLevel: Boolean = false): Tree? {
		var tk = next();
		if(tk == null) {
			if(topLevel)
				return null;
			// this should be safe - if we're not at top level another block should've been parsed
			throwEOF();
		}
		if(topLevel && tk !is Token.Event && tk !is Token.Declare)
			throw ParseException(tk.loc, "Unexpected $tk at top-level.");
		when(tk) {
			is Token.Lbrack, is Token.Rbrack -> {
				throw ParseException(tk.loc, "Unexpected $tk.");
			}
			is Token.If -> {
				val b = block();
				if(b.size < 2)
					throw ParseException(tk.loc, "An if must have at least 2 children.");
				var conds = mutableListOf<Tree>();
				var actions = mutableListOf<Tree>();
				for(i in 0..(b.size/2-1)) {
					conds.add(b[i*2]);
					actions.add(b[i*2+1]);
				}
				var failure: Tree? = null;
				if(b.size%2 != 0)
					failure = b[b.size-1];
				return Tree.If(loc, conds, actions, failure);
			}
			is Token.Event -> {
				if(!topLevel)
					throw ParseException(tk.loc, "Event can only be used at top-level.");
				val b = block();
				return Tree.Event(tk.loc, tk.name, b);
			}
			is Token.Builtin -> {
				val b = block();
				return Tree.Builtin(tk.loc, tk.name, b);
			}
			is Token.Do -> return Tree.Do(tk.loc, block())
			is Token.For -> {
				var idx = next();
				if(idx !is Token.Variable)
					throw ParseException(tk.loc, "A for loop is of the form: for <variable> <list> [ ... ]");
				var l = nextTree();
				if(l == null)
					throwEOF();
				return Tree.For(tk.loc, idx.name, l, block());
			}
			is Token.While -> {
				var cond = nextTree();
				if(cond == null)
					throwEOF();
				return Tree.While(tk.loc, cond, block());
			}
			is Token.Return -> {
				var n = nextTree();
				if(n == null)
					throwEOF();
				return Tree.Return(tk.loc, n);
			}
			is Token.List -> return Tree.List(tk.loc, block())
			is Token.Map -> {
				val b = block();
				var keys = mutableListOf<Tree>();
				var values = mutableListOf<Tree>();
				if(b.size == 0)
					return Tree.Map(loc, keys, values);
				if(b.size%2 != 0)
					throw ParseException(tk.loc, "Maps take an even number of elements to initialize.");
				for(i in 0..(b.size/2-1)) {
					keys.add(b[i*2]);
					values.add(b[i*2+1]);
				}
				return Tree.Map(loc, keys, values);
			}
			is Token.Declare -> {
				var n = nextTree();
				if(n == null)
					throwEOF();
				return Tree.Declare(tk.loc, tk.name, n);
			}
			is Token.Set -> {
				var n = nextTree();
				if(n == null)
					throwEOF();
				return Tree.Set(tk.loc, tk.name, n);
			}
			else -> return Tree.Token(tk.loc, tk);
		}
	}

	fun parse(): Tree {
		val list = mutableListOf<Tree>();
		while(true) {
			var tree = nextTree(true);
			if(tree == null)
				break;
			list.add(tree);
		}
		if(list.size == 0)
			throw ParseException(null, "Empty program.");
		return Tree.Do(list[0].loc, list);
	}
}
