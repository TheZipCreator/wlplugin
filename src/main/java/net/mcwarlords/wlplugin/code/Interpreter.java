package net.mcwarlords.wlplugin.code;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.BukkitRunnable;
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
    ItemStack[][] items;
    int line;
    Location origin;

    CodeLoc(Location origin, ItemStack[][] items) {
      this.items = items;
      this.origin = origin;
      line = 0;
    }

    void inc() {
      line++;
    }

    boolean atEnd() {
      return line >= items.length;
    }

    ItemStack[] getLine() {
      return items[line];
    }

    Location getLoc() {
      return new Location(origin.getWorld(), origin.getX()+line/3, origin.getY(), origin.getZ());
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
    LPAREN, RPAREN,
    COMMA, DOT
  }
  HashMap<String, Var> vars;
  Consumer<String> log;
  CodeLoc loc;
  int scope;
  private static ArrayList<Pair<Pattern, TokenType>> lexerRules;
  static HashMap<String, ExternalCodeFunction> builtinFunctions;

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
    lexerRules.add(Pair.with(Pattern.compile(","), TokenType.COMMA));
    lexerRules.add(Pair.with(Pattern.compile("."), TokenType.DOT));

    builtinFunctions = new HashMap<String, ExternalCodeFunction>();
    builtinFunctions.put("List", (Value[] values) -> {
      List<Value> l = new ArrayList<Value>();
      for(int i = 0; i < values.length; i++)
        l.add(values[i]);
      return new Value(l);
    });
  }

  public Interpreter(Location loc, Consumer<String> log) {
    List<ItemStack[]> contents = new ArrayList<ItemStack[]>();
    while(true) {
      Block b = loc.getBlock();
      if(b.getType() != Material.CHEST)
        break;
      Chest c = (Chest)b.getState();
      Inventory inv = c.getInventory();
      int rows = inv.getSize()/9;
      if(rows == 3)
        loc.setX(loc.getX()+1); // single chest
      else
        loc.setX(loc.getX()+2); // double chest
      for(int i = 0; i < rows; i++) {
        List<ItemStack> row = new ArrayList<ItemStack>();
        for(int j = 0; j < 9; j++) {
          ItemStack is = inv.getItem(i*9+j);
          if(is != null)
            row.add(is);
        }
        contents.add(row.toArray(new ItemStack[]{}));
      }
    }
    this.loc = new CodeLoc(loc, contents.toArray(new ItemStack[][]{}));
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
    Location l = loc.getLoc();
    log(Utils.escapeText("&_pError at &_e("+l.getX()+","+l.getY()+","+l.getZ()+"):"+loc.line+"&_p:&f "+msg));
  }

  private void log(String msg) {
    new BukkitRunnable() {
      public void run() {
        log.accept(msg);
      }
    }.runTask(WlPlugin.instance);
  }

  public Value getVar(String name) throws InterpreterException {
    if(vars.containsKey(name))
      return vars.get(name).val;
    if(builtinFunctions.containsKey(name))
      return new Value(builtinFunctions.get(name));
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
    boolean operatorPushed = false; // was the last thing pushed an operator?

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
        throw new InterpreterException("Incomplete expression.");
      }
    };
    for(int i = 0; i < tokens.size(); i++) {
      Token t = tokens.get(i);
      operatorPushed = false;
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
        case LPAREN: {
          if(operatorPushed || i == 0) {
            operators.push(t.type);
            operatorPushed = true;
            break;
          }
          // function call
          // find arguments
          List<Value> args = new ArrayList<Value>();
          int paren = 1;
          List<Token> sublist = new ArrayList<Token>();
          while(paren > 0) {
            i++;
            if(i >= tokens.size())
              throw new InterpreterException("Incomplete expression.");
            Token token = tokens.get(i);
            switch(token.type) {
              case LPAREN:
                paren++;
                break;
              case RPAREN:
                paren--;
                break;
            }
            if(token.type == TokenType.COMMA && paren == 1) {
              args.add(expression(sublist).clone());
              sublist.clear();
            } else
              sublist.add(token);
          }
          if(sublist.size() > 0)
            args.add(expression(sublist).clone());
          // call
          Value func = operands.pop();
          operands.push(func.call(args.toArray(new Value[]{})));
          break;
        }
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
          operatorPushed = true;
          break;
        case COMMA:
          throw new InterpreterException("Unexpected comma.");
      }
    }
    reduce.accept(-1, false);
    if(operators.size() != 0 || operands.size() != 1)
      throw new InterpreterException("Incomplete expression.");
    return operands.get(0);
  }

  void run() throws InterpreterException {
    while(!loc.atEnd()) {
      ItemStack[] row = loc.getLine();
      if(row.length > 0) {
        switch(row[0].getType()) {
          case OAK_LOG: { // log
            String res = "";
            for(int i = 1; i < row.length; i++)
              res += eval(row[i]).getString();
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
    log("Finished execution.");
  }

}
