module shell;

import arsd.jni;
import std.array : appender;
import std.conv;

enum PACKAGE_NAME = "net.mcwarlords.wlplugin.shell";

// some java bindings

interface CommandSender : JavaInterface!("org.bukkit.command", CommandSender) {
  @Import void sendMessage(string message);
}

final class Player : JavaClass!("org.bukkit.entity", Player), CommandSender {
  @Import void sendMessage(string message);
}

final class Utils : JavaClass!("net.mcwarlords.wlplugin", Utils) {
  @Import private static string escapeText(string message);
}

// the lexer

enum TEOF = Token("", Token.Type.END);

struct Token {
  enum Type {
    STRING, INTEGER, FLOAT, VAR,
    ADD, SUB, MUL, DIV, MOD, CONCAT,
    LBRACK, RBRACK, LPAREN, RPAREN, LBRACE, RBRACE,
    SEMICOLON,
    END
  }
  string value;
  Type   type;
  size_t line;

  string toString() {
    return "("~line.to!string~", "~type.to!string~", "~value~")";
  }
}

// (Lexception?)
class LexException : Exception {
  size_t line;
  this(size_t line, string msg) {
    import std.conv : to;
    this.line = line;
    super(line.to!string~": "~msg);
  }
}

enum Numeric {
  INT, FLOAT, NONE
}

Numeric numeric(string s) {
  if(s == ".")
    return Numeric.NONE;
  bool dot = false;
  foreach(c; s) {
    if(c == '.' && !dot) {
      dot = true;
      continue;
    }
    if(c < '0' || c > '9')
      return Numeric.NONE;
  }
  return dot ? Numeric.FLOAT : Numeric.INT;
}

Token[] lex(string code) {
  Token[] tokens;
  size_t line = 1;
  auto ap = appender!string;
  enum State {
    COMMAND,
    EXPRESSION
  }
  State[] states = [State.COMMAND];
  State state() {
    return states[$-1];
  }
  void add() {
    string val = ap[];
    ap = ap.init; // clear ap
    if(val == "")
      return;
    Token.Type type;
    auto num = val.numeric;
    import std.string : startsWith;
    final switch(num) {
      case Numeric.INT:
        type = Token.Type.INTEGER;
        break;
      case Numeric.FLOAT:
        type = Token.Type.FLOAT;
        break;
      case Numeric.NONE:
        if(val.startsWith("$"))
          type = Token.Type.VAR;
    }
    tokens ~= Token(val, type, line);
  }
  void op(char c, Token.Type t) {
    add();
    import std.conv : to;
    tokens ~= Token(c.to!string, t, line);
  }
  noreturn err(string msg) {
    throw new LexException(line, msg);
  }
  string genSwitch() {
    string s = "";
    string[string] ops = [
      "+": "ADD",
      "-": "SUB",
      "*": "MUL",
      "/": "DIV",
      "%": "MOD",
      "~": "CONCAT"
    ];
    foreach(k, v; ops) {
      import std.format, std.string : replace;
      s ~= q{
        case '_':
          op(c, Token.Type.!!);
          break;
      }.replace("_", k).replace("!!", v);
    }
    return s;
  }
  for(size_t i = 0; i < code.length; i++) {
    char c = code[i];
    final switch(state) {
      case State.COMMAND:
        switch(c) {
          case ' ':
            add();
            break;
          case '[':
            states ~= State.COMMAND;
            op(c, Token.Type.LBRACK);
            break;
          case ']':
            op(c, Token.Type.RBRACK);
            if(states.length == 0)
              err("Unbalanced brackets");
            states = states[0..$-1];
            break;
          case '{':
            states ~= State.COMMAND;
            op(c, Token.Type.LBRACE);
            break;
          case '}':
            op(c, Token.Type.RBRACE);
            if(states.length == 0)
              err("Unbalanced brackets");
            states = states[0..$-1];
            break;
          case ')':
            err("Unexpected parenthesis");
          case '(':
            states ~= State.EXPRESSION;
            op(c, Token.Type.LPAREN);
            break;
          case ';':
            op(c, Token.Type.SEMICOLON);
            break;
          default:
            ap ~= c;
        }
        break;
      case State.EXPRESSION:
        switch(c) {
          case ' ':
            add();
            break;
          // operators
          mixin(genSwitch());
          case '[':
            states ~= State.COMMAND;
            op(c, Token.Type.LBRACK);
            break;
          case ']':
            err("Unexpected bracket");
          case ')':
            op(c, Token.Type.RPAREN);
            if(states.length == 0)
              err("Unexpected parenthesis");
            states = states[0..$-1];
            break;
          case '(':
            states ~= State.EXPRESSION;
            op(c, Token.Type.LPAREN);
            break;
          case '{':
            states ~= State.COMMAND;
            op(c, Token.Type.LBRACE);
            break;
          case '}':
            op(c, Token.Type.RBRACE);
            if(states.length == 0)
              err("Unbalanced brackets");
            states = states[0..$-1];
            break;
          default:
            ap ~= c;
        }
    }
  }
  op(' ', Token.Type.END);
  return tokens;
}

import std.variant;

/// Represents a value in D
struct Value {
  enum Type {
    NULL, STRING, INT, FLOAT, LIST, BLOCK
  }
  Type type;
  Variant val;

  this(string v) {
    type = Type.STRING;
    val = v;
  }
  this(int v) {
    type = Type.INT;
    val = v;
  }
  this(float v) {
    type = Type.FLOAT;
    val = v;
  }
  // this(Block v) {
  //   type = Type.BLOCK;
  //   val = v;
  // }
  this(Value v) {
    type = v.type;
    val = v.val;
  }
  this(T)(T[] v) {
    static if(is(T == Value)) {
      type = Type.LIST;
      val = v;
    } else {
      type = Type.LIST;
      Value[] arr;
      foreach(w; v)
        arr ~= Value(w);
      val = arr;
    }
  }

  T get(T)() {
    return val.get!T;
  }
  T coerce(T)() {
    noreturn err() {
      throw new InterpreterException("Cannot coerce "~toString~" to "~T.stringof);
    }
    static if(is(T == string))
      return toString;
    else static if(is(T == int)) {
      final switch(type) {
        case Type.NULL:
          err;
        case Type.STRING:
          try
            return toString.to!int;
          catch(ConvException)
            err;
        case Type.INT:
          return get!int;
        case Type.FLOAT:
          return get!float.to!int;
        case Type.LIST:
        case Type.BLOCK:
          err;
      }
    } else static if(is(T == float)) {
      final switch(type) {
        case Type.NULL:
          err;
        case Type.STRING:
          try
            return toString.to!float;
          catch(ConvException)
            err;
        case Type.INT:
          return get!int.to!float;
        case Type.FLOAT:
          return get!float;
        case Type.LIST:
        case Type.BLOCK:
          return err;
      }
    } else static if(is(T == Block)) {
      switch(type) {
        default:
          err;
        case Type.BLOCK:
          return get!Block;
      }
    }
    else
      static assert(0, "Cannot coerce to "~T.stringof);
  }

  string toString() {
    final switch(type) {
      case Type.NULL:
        return "null";
      case Type.STRING:
        return get!string;
      case Type.INT:
        return get!int.to!string;
      case Type.FLOAT:
        return get!float.to!string;
      case Type.LIST: {
        auto ap = appender!string;
        ap ~= "[list ";
        foreach(i, v; val.get!(Value[])) {
          if(i != 0)
            ap ~= " ";
          ap ~= v.toString;
        }
        ap ~= "]";
        return ap[];
      }
      case Type.BLOCK: {
        auto ap = appender!string;
        ap ~= "{";
        // foreach(i, a; (val.get!(Block)).commands) {
        //   if(i != 0)
        //     ap ~= "; ";
        //   foreach(j, t; a) {
        //     if(j != 0)
        //       ap ~= " ";
        //     ap ~= t.value;
        //   }
        // }
        ap ~= "}";
        return ap[];
      }
    }
  }

  private static pure string genMathOp(string name, string op) {
    import std.string : replace;
    return q{
      static Value {name}(Value a, Value b) {
        if(a.type == Type.FLOAT && b.type == Type.INT)
          b = Value(b.coerce!float);
        else if(a.type == Type.INT && b.type == Type.FLOAT)
          a = Value(a.coerce!float);
        switch(a.type) {
          case Type.INT:
            return Value(a.get!int{op}b.get!int);
          case Type.FLOAT:
            return Value(a.get!float{op}b.get!float);
          default:
            throw new InterpreterException("Cannot perform op {op} on "~a.toString~" and "~b.toString);
        }
      }
    }.replace("{op}", op).replace("{name}", name);
  }

  mixin(genMathOp("add", "+"));
  mixin(genMathOp("sub", "-"));
  mixin(genMathOp("mul", "*"));
  mixin(genMathOp("div", "/"));
  mixin(genMathOp("mod", "%"));

  bool truthy() {
    final switch(type) {
      case Type.NULL:
        return false;
      case Type.STRING:
        return get!string.length > 0;
      case Type.INT:
        return get!int != 0;
      case Type.FLOAT:
        return get!float != 0;
      case Type.LIST:
        return get!(Value[]).length > 0;
      case Type.BLOCK:
        return true;
    }
  }
}

class InterpreterException : Exception {
  size_t line;
  string msg;

  this(size_t line, string msg) {
    super(msg);
    this.line = line;
  }

  this(string msg) {
    super(msg);
  }
}

/// Java wrapper for Value
@JavaName("Value")
final class JavaValue : JavaClass!(PACKAGE_NAME, JavaValue) {
  // generate constants
  static foreach(type; __traits(allMembers, Value.Type)) {
    import std.string : replace;
    mixin(q{
      @Export int _0() {
        return Value.Type._0;
      }
    }.replace("_0", type.stringof[1..$-1]));
  }
  Value val;
  this(Value val) {
    this.val = val;
  }
  @Export void ctor() {
    val = Value();
  }
  @Export void ctor(string v) {
    val = Value(v);
  }
  @Export void ctor(int v) {
    val = Value(v);
  }
  @Export void ctor(float v) {
    val = Value(v);
  }
  @Export void ctor(JavaValue[] v) {
    Value[] vals;
    foreach(jv; v)
      vals ~= jv.val;
    val = Value(vals);
  }
  @Export string getString() {
    return val.get!string;
  }
  @Export int getInt() {
    return val.get!int;
  }
  @Export float getFloat() {
    return val.get!float;
  }
  @Export JavaValue[] getList() {
    JavaValue[] ret;
    foreach(v; val.get!(Value[]))
      ret ~= new JavaValue(v);
    return ret;
  }
  // not gonna bother with getting blocks, since Token can not be passed to the java side

  // TODO: coercion

  static private pure string genMathOp(string name) {
    import std.string : replace;
    return q{
      @Export static JavaValue _0(JavaValue a, JavaValue b) {
        return new JavaValue(Value._0(a.val, b.val));
      }
    }.replace("_0", name);
  }

  mixin(genMathOp("add"));
  mixin(genMathOp("sub"));
  mixin(genMathOp("mul"));
  mixin(genMathOp("div"));
  mixin(genMathOp("mod"));

  @Export bool truthy() {
    return val.truthy();
  }
}

/// Interface for all commands
interface Command {
  Value run(Value[] vals);
}

/// Command implemented in java
final class JavaCommand : JavaClass!(PACKAGE_NAME, JavaCommand), Command {
  @Import JavaValue run(JavaValue[] vals);
  @Import this(JavaCommand jc);
  Value run(Value[] vals) {
    JavaValue[] jvals;
    foreach(val; vals)
      jvals ~= new JavaValue(val);
    return run(jvals).val;
  }
}

/// Command implemented in D (these aren't visible to java)
final class DCommand : Command {
  Value delegate(Value[]) fn;

  this(Value delegate(Value[]) fn) {
    this.fn = fn;
  }

  Value run(Value[] vals) {
    return fn(vals);
  }
}

class TokenStream {
  Token[] tokens;
  size_t pos;

  this(Token[] tokens) {
    this.tokens = tokens;
    pos = 0;
  }

  Token next() {
    if(pos >= tokens.length)
      return Token("", Token.Type.END, tokens[$-1].line);
    return tokens[pos++];
  }

  Token peek() {
    if(pos >= tokens.length)
      return Token("", Token.Type.END, tokens[$-1].line);
    return tokens[pos];
  }
}

/// The shell class. This is mainly how the shell interfaces with java
final class Shell : JavaClass!(PACKAGE_NAME, Shell) {
  @Import @property CommandSender host();
  @Import @property void host(CommandSender);
  @Import @property int id();
  @Import @property void id(int);

  /// Exists so we can store D data between java objects
  struct ShellData {
    Command[string] commands;
    Value[string] vars;
  }

  static ShellData[int] data;
  // generate setters and getters
  static foreach(e; __traits(allMembers, ShellData)) {
    import std.string : replace;
    pragma(msg, e);
    pragma(msg, typeof(e));
    // mixin(q{
    //   _type _name() {
    //     return data[id]._name;
    //   }
    //   void _name(_type t) {
    //     data[id]._name = t;
    //   }
    // }.replace("_type", __traits(getMember, ShellData, e).typeof.stringof).replace("_name", e));
  }

  Command[string] commands;
  Value[string] vars;

  @Export void ctor() {
    // default commands
    commands = [
      "echo": new DCommand((Value[] vals) {
        string res = "";
        foreach(v; vals)
          res ~= v.toString;
        host.sendMessage(res);
        return Value();
      })
    ];
  }

  /// used from the java side to add JavaCommands
  @Export void addCommand(string name, JavaCommand jc) {
    commands[name] = new JavaCommand(jc);
  }

  /// get a var as a JavaValue. NOTE: modifying the returned value is pointless, since it basically creates a clone
  @Export JavaValue getVar(string name) {
    if(name in vars)
      return new JavaValue(vars[name]);
    return new JavaValue(Value());
  }

  // set a variable to a JavaValue
  @Export void setVar(string name, JavaValue val) {
    vars[name] = val.val;
  }

  @Export void run(string code) {
    auto ts = new TokenStream(code.lex);
    command(ts);
  }

  private enum string sharedCases = q{
    case Token.Type.LBRACK:
      values ~= command(ts);
      break;
    // case Token.Type.LPAREN:
    //   values ~= expression(ts);
    //   break;
    case Token.Type.STRING:
      values ~= Value(next.value);
      break;
    case Token.Type.INTEGER:
      values ~= Value(next.value.to!int);
      break;
    case Token.Type.FLOAT:
      values ~= Value(next.value.to!float);
      break;
    case Token.Type.VAR:
      values ~= vars[next.value];
      break;
    // case Token.Type.LBRACE:
    //   values ~= block(state);
    //   break;
  };

  /// Interprets a command
  Value command(TokenStream ts) {
    Value[] values;
    try {
      while(true) {
        auto next = ts.next;
        switch(next.type) {
          default:
            break;
          mixin(sharedCases);
          case Token.Type.RBRACK:
          case Token.Type.END:
            if(values.length > 0) {
              string cmd = values[0].toString;
              auto args = values[1..$];
              if(cmd in commands)
                return commands[cmd].run(args);
              throw new InterpreterException("Unknown command: "~cmd);
            }
            return Value();
        }
      }
    } catch(InterpreterException e) {
      e.line = ts.peek.line;
      throw e;
    }
  }
}

version(Windows) {
	import core.sys.windows.dll;
	mixin SimpleDllMain;
}