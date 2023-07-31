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

import net.mcwarlords.wlplugin.*;

// class for ASTs
sealed class Tree(val loc: Location) {
	class Do(l: Location, val children: List<Tree>) : Tree(l);
	class Event(l: Location, val name: kotlin.String, val children: List<Tree>) : Tree(l);
	class If(l: Location, val cond: Tree, val success: Tree, val failure: Tree? = null) : Tree(l);
	class Builtin(l: Location, val name: kotlin.String, val args: List<Tree>) : Tree(l);
	class String(l: Location, val string: kotlin.String) : Tree(l);
	class Variable(l: Location, val name: kotlin.String) : Tree(l);
	class Number(l: Location, val num: Double) : Tree(l);
	class Bool(l: Location, val bool: Boolean): Tree(l);

	override fun toString() = when(this) {
		is Event -> "Event '$name' { ${children.joinToString(" ")} }"
		is If -> "If { $cond $success ${if(failure == null) "" else "$failure "}}"
		is Builtin -> "Builtin '$name' { ${args.joinToString(" ")} }"
		is String -> "String '$string'"
		is Variable -> "Variable '$name'"
		is Number -> "Number '$num'"
		is Bool -> if(bool) "True" else "False"
		is Do -> "${children.joinToString(" ")}"
	}

	fun name() = when(this) {
		is Do -> "Do"
		is Event -> "Event"
		is If -> "If"
		is Builtin -> "Builtin"
		is String -> "String"
		is Variable -> "Variable"
		is Number -> "Number"
		is Bool -> "Bool"
	}
}


sealed class Token(val loc: Location) {
	class If(l: Location) : Token(l);
	class Lbrack(l: Location) : Token(l);
	class Rbrack(l: Location) : Token(l);
	class Event(l: Location, val name: kotlin.String) : Token(l);
	class Builtin(l: Location, val name: kotlin.String) : Token(l);
	class Variable(l: Location, val name: kotlin.String) : Token(l);
	class String(l: Location, val string: kotlin.String) : Token(l);
	class Number(l: Location, val num: Double) : Token(l);
	class Bool(l: Location, val bool: Boolean): Token(l);

	override fun toString(): kotlin.String = when(this) {
		is If -> "if"
		is Lbrack -> "left bracket"
		is Rbrack -> "right bracket"
		is Event -> "event '$name'"
		is Builtin -> "builtin '$name'"
		is Variable -> "variable '$name'"
		is String -> "string '$string'"
		is Number -> "number '$num'"
		is Bool -> if(bool) "true" else "false"
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

	private var nextToken: Token? = null; // token cached in case peek() is called

	private var lastLocation: Location? = null; // location of last block
	
	// returns null on EOF
	fun next(): Token? {
		if(nextToken != null) {
			var t = nextToken;
			nextToken = null;
			return t;
		}
		// skip past whitespace and comments
		while(true) {
			var _b = nextBlock();
			if(_b == null)
				return null;
			var b = _b!!; // the compiler says the `!!` is uneccessary but removing it causes an error
			fun readSign(): String {
				var lines = (b.getRelative(BlockFace.EAST).state as Sign).lines;
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
						for(e in CodeEvent.values()) {
							if(s == e.lispCase())
								return Token.Event(loc, s);
						}
						throw ParseException(loc, "Unknown event '$s'.");
					}
					Material.FURNACE -> return Token.Builtin(loc, readSign());
					Material.OAK_PLANKS -> return Token.If(loc);
					Material.OBSIDIAN -> return Token.Variable(loc, readSign());
					Material.WHITE_WOOL -> return Token.String(loc, buildString {
						while(true) {
							append(readSign());
							if(b.getRelative(BlockFace.NORTH).type == Material.WHITE_WOOL)
								b = nextBlock()!!;
							else
								break;
						}
					});
					Material.LIME_TERRACOTTA -> return Token.Bool(loc, true);
					Material.RED_TERRACOTTA -> return Token.Bool(loc, false);
					Material.TARGET -> {
						val n = readSign().toDoubleOrNull();
						if(n == null)
							throw ParseException(loc, "Invalid number.");
						return Token.Number(loc, n);
					}
					else -> throw ParseException(loc, "Unknown block type: ${b.type.toString()}")
				}
			}
		}
		
	}
	
	// peeks at the next token
	fun peek(): Token? {
		var tk = next();
		nextToken = tk;
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
		if(topLevel && (tk !is Token.Event))
			throw ParseException(tk.loc, "Unexpected $tk at top-level.");
		when(tk) {
			is Token.Lbrack, is Token.Rbrack -> {
				throw ParseException(tk.loc, "Unexpected $tk.");
			}
			is Token.If -> {
				val b = block();
				if(b.size < 2 || b.size > 3)
					throw ParseException(tk.loc, "Incorrect number of elements inside if block.");
				if(b.size == 2)
					return Tree.If(tk.loc, b[0], b[1]);
				else
					return Tree.If(tk.loc, b[0], b[1], b[2]);
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
			is Token.Variable -> return Tree.Variable(tk.loc, tk.name);
			is Token.String -> return Tree.String(tk.loc, tk.string);
			is Token.Number -> return Tree.Number(tk.loc, tk.num);
			is Token.Bool -> return Tree.Bool(tk.loc, tk.bool);
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
