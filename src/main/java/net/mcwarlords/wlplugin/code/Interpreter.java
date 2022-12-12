package net.mcwarlords.wlplugin.code;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.inventory.*;
import org.javatuples.Pair;

import net.mcwarlords.wlplugin.Utils;
import net.mcwarlords.wlplugin.WlPlugin;

public class Interpreter {
  class Var {
    int scope;
    Value val;

    Var(int scope, Value val) {
      this.scope = scope;
      this.val = val;
    }
  }
  class CodeLoc {
    Location chest;
    int line;

    CodeLoc(Location chest, int line) {
      this.chest = chest;
      this.line = line;
    }

    void inc() {
      line++;
      if(line > 5) {
        chest.setX(chest.getX()+2);
        line = 0;
      }
    }
  }
  /** Expression tokens */
  class Token {
    TokenType type;
    String value;

    Token(TokenType type, String value) {
      this.type = type;
      this.value = value;
    }
  }
  enum TokenType {
    IDENTIFIER, STRING, INT, FLOAT,
    ADD, SUB, MUL, DIV, MOD, CAT,
    LPAREN, RPAREN
  }
  HashMap<String, Var> vars;
  Consumer<String> log;
  CodeLoc loc;
  int scope;
  private static ArrayList<Pair<Pattern, TokenType>> lexerRules;
  static HashMap<String, CodeFunction> builtinFunctions;

  static {
    lexerRules = new ArrayList<Pair<Pattern, TokenType>>();
    lexerRules.add(Pair.with(Pattern.compile(" +"), null));
    lexerRules.add(Pair.with(Pattern.compile("§(#......|.)"), null));
    lexerRules.add(Pair.with(Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*"), TokenType.IDENTIFIER));
    lexerRules.add(Pair.with(Pattern.compile("[0-9]+(\\.[0-9]*)"), TokenType.FLOAT));
    lexerRules.add(Pair.with(Pattern.compile("[0-9]+"), TokenType.INT));
    // I wish java had raw strings
    lexerRules.add(Pair.with(Pattern.compile("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\""), TokenType.STRING));
    lexerRules.add(Pair.with(Pattern.compile("\\+"), TokenType.ADD));
    lexerRules.add(Pair.with(Pattern.compile("-"), TokenType.SUB));
    lexerRules.add(Pair.with(Pattern.compile("\\*"), TokenType.MUL));
    lexerRules.add(Pair.with(Pattern.compile("/"), TokenType.DIV));
    lexerRules.add(Pair.with(Pattern.compile("%"), TokenType.MOD));
    lexerRules.add(Pair.with(Pattern.compile("~"), TokenType.CAT));
    lexerRules.add(Pair.with(Pattern.compile("\\("), TokenType.LPAREN));
    lexerRules.add(Pair.with(Pattern.compile("\\)"), TokenType.RPAREN));

    builtinFunctions = new HashMap<String, CodeFunction>();
  }

  public Interpreter(Location loc, Consumer<String> log) {
    this.loc = new CodeLoc(loc, 0);
    vars = new HashMap<String, Var>();
    this.log = log;
  }

  Interpreter(HashMap<String, Var> vars, Consumer<String> log, CodeLoc loc, int scope) {
    this.vars = vars;
    this.log = log;
    this.loc = loc;
    this.scope = scope;
  }

  public Interpreter sub() {
    return new Interpreter(vars, log, loc, scope+1);
  }

  public Interpreter clone() {
    return new Interpreter(vars, log, loc, scope);
  }

  public void err(String msg) {
    Location l = loc.chest;
    log.accept(Utils.escapeText("&_pError at &_e("+l.getX()+","+l.getY()+","+l.getZ()+"):"+loc.line+"&_p:&f "+msg));
  }

  private void log(String msg) {
    log.accept(msg);
  }

  public Value getVar(String name) throws InterpreterException {
    if(vars.containsKey(name))
      return vars.get(name).val;
    throw new InterpreterException("Unknown variable "+name+".");
  }

  ArrayList<Token> lex(String expr) throws InterpreterException {
    ArrayList<Token> tokens = new ArrayList<Token>();
    int i = 0;
    outer:
    while(true) {
      if(i >= expr.length())
        break;
      String s = expr.substring(i);
      for(int j = 0; j < lexerRules.size(); j++) {
        Pair<Pattern, TokenType> rule = lexerRules.get(j);
        Pattern pattern = rule.getValue0();
        TokenType type = rule.getValue1();
        Matcher m = pattern.matcher(s);
        if(m.lookingAt()) {
          i += m.end();
          if(type != null) {
            tokens.add(new Token(type, s.substring(0, m.end())));
          }
          continue outer;
        }
      }
      throw new InterpreterException("Incorrect expression syntax.");
    }
    return tokens;
  }

  Value eval(ItemStack is) throws InterpreterException {
    String name = is.getItemMeta().getDisplayName();
    switch(is.getType()) {
      case GOLD_INGOT: // expresion
        return expression(lex(name));
      default:
        throw new InterpreterException("Invalid argument type.");
    }
  }

  private int precedence(TokenType t) {
    switch(t) {
      case MUL:
      case DIV:
        return 2;
      case ADD:
      case SUB:
        return 1;
      default:
        return 0;
    }
  }

  // why the fuck do I need this
  @FunctionalInterface
  private interface Consumer2<T, U> {
    void accept(T t, U u) throws InterpreterException;
  }

  Value expression(List<Token> tokens) throws InterpreterException {
    Stack<TokenType> operators = new Stack<TokenType>();
    Stack<Value> operands = new Stack<Value>();

    // closest you can get to a local function in java
    Consumer2<Integer, Boolean> reduce = (Integer prec, Boolean lparen) -> {
      try {
        while(operators.size() != 0) {
          if(lparen) {
            if(operators.peek() == TokenType.LPAREN) {
              operators.pop();
              break;
            }
          } else {
            if(precedence(operators.peek()) < prec)
              break;
          }
          TokenType op = operators.pop();
          Value a = operands.pop();
          Value b = operands.pop();
          Value ret = new Value(); // I shouldn't need to initialize this but whatever
          switch(op) {
            case ADD:
              ret = Value.add(a, b);
              break;
            case SUB:
              ret = Value.subtract(a, b);
              break;
            case MUL:
              ret = Value.multiply(a, b);
              break; 
            case DIV:
              ret = Value.divide(a, b);
              break;
            case MOD:
              ret = Value.modulo(a, b);
              break;
          }
          operands.push(ret);
        }
      } catch(EmptyStackException e) {
        throw new InterpreterException("Incomplete expression");
      }
    };
    for(int i = 0; i < tokens.size(); i++) {
      Token t = tokens.get(i);
      switch(t.type) {
        case IDENTIFIER:
          operands.push(getVar(t.value));
          break;
        case STRING:
          operands.push(Value.fromStringLiteral(t.value));
          break;
        case INT:
          operands.push(new Value(Integer.parseInt(t.value)));
          break;
        case FLOAT:
          operands.push(new Value(Float.parseFloat(t.value)));
          break;
        case LPAREN:
          operators.push(t.type);
          break;
        case RPAREN:
          reduce.accept(0, true);
          break;
        case ADD:
        case SUB:
        case MUL:
        case DIV:
        case MOD:
        case CAT:
          reduce.accept(precedence(t.type), false);
          operators.push(t.type);
          break;
      }
    }
    reduce.accept(-1, false);
    if(operators.size() != 0 || operands.size() != 1)
      throw new InterpreterException("Incomplete expression");
    return operands.get(0);
  }

  void run() throws InterpreterException {
    while(true) {
      Block b = loc.chest.getBlock();
      if(b.getType() != Material.CHEST) {
        log("Finished execution.");
        return;
      }
      Chest chest = (Chest)(b.getState());
      Inventory inv = chest.getInventory();
      if(inv.getSize() != 54)
        throw new InterpreterException("Incorrect inventory size. Make sure you're using double chests and that they're facing in the +X direction");
      List<ItemStack> row = new ArrayList<ItemStack>();
      for(int i = loc.line*9; i < loc.line*9+9; i++) {
        ItemStack is = inv.getItem(i);
        if(is != null)
          row.add(is);
      }
      if(row.size() > 0) {
        switch(row.get(0).getType()) {
          case OAK_LOG: { // log
            String res = "";
            for(int i = 1; i < row.size(); i++)
              res += eval(row.get(i)).toString();
            log(res);
            break;
          }
          case ENDER_CHEST: { // set
            // TODO
          }
          default:
            throw new InterpreterException("Invalid command type.");
        }
      }
      loc.inc();
    }
  }

}
