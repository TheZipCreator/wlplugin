package net.mcwarlords.wlplugin.code;

public class Value {
  enum Type {
    NULL, INT, FLOAT, STRING
  }
  Type type;
  Object value;

  Value() {
    type = Type.NULL;
  }

  Value(int v) {
    type = Type.INT;
    value = v;
  }

  Value(float v) {
    type = Type.FLOAT;
    value = v;
  }

  Value(String v) {
    type = Type.STRING;
    value = v;
  }

  public Value clone() {
    switch(type) {
      case INT:
        return new Value(((Integer)value).intValue());
      case FLOAT:
        return new Value(((Float)value).floatValue());
      case STRING:
        return new Value((String)value);
      default:
        return new Value();
    }
  }

  public int getInt() {
    switch(type) {
      case INT:
        return (Integer)value;
      case FLOAT:
        return Math.round(((Float)value).floatValue());
      case STRING:
        return 0; // TODO
      default: // why do I need this? a switch on an enum always covers all possible states
        return 0;
    }
  }

  public float getFloat() {
    switch(type) {
      case INT:
        return (float)((Integer)value);
      case FLOAT:
        return ((Float)value).floatValue();
      case STRING:
        return 0; // TODO
      default:
        return 0;
    }
  }

  public String toString() {
    switch(type) {
      case INT:
        return ((Integer)value).toString();
      case FLOAT:
        return ((Float)value).toString();
      case STRING:
        return (String)value;
      default:
        return "";
    }
  }

  public static Value fromStringLiteral(String s) {
    s = s.substring(1, s.length()-1) // remove quotes
         .replace("\\\"", "\"")
         .replace("\\n", "\n");
    return new Value(s);
  }

  /*
    Java does not have good metaprogramming capabilities, so I can't automatically change the operation
    in this to a different one. Instead I'll give a few templates which can be turned into actual functions
    by manually finding an replacing things
  */

  /* 
  Arithmetic operations:
  <oper> The literal operation (e.g. + or *)
  <name> name of the function (e.g. add or multiply)

  public static Value <name>(Value a, Value b) throws InterpreterException {
    if(a.type == Type.INT && b.type == Type.FLOAT) {
      a = new Value(a.getFloat());
    }
    else if(a.type == Type.FLOAT && b.type == Type.INT) {
      b = new Value(b.getFloat());
    }
    if(a.type != b.type)
      throw new InterpreterException("Cannot <name> "+a.type+" and "+b.type);
    switch(a.type) {
      case INT:
        return new Value(a.getInt()<oper>b.getInt());
      case FLOAT:
        return new Value(a.getFloat()<oper>b.getFloat());
      default:
        throw new InterpreterException("Cannot <name> "+a.type);
    }
  }
  */

  public static Value add(Value a, Value b) throws InterpreterException {
    if(a.type == Type.INT && b.type == Type.FLOAT) {
      a = new Value(a.getFloat());
    }
    else if(a.type == Type.FLOAT && b.type == Type.INT) {
      b = new Value(b.getFloat());
    }
    if(a.type != b.type)
      throw new InterpreterException("Cannot add "+a.type+" and "+b.type);
    switch(a.type) {
      case INT:
        return new Value(a.getInt()+b.getInt());
      case FLOAT:
        return new Value(a.getFloat()+b.getFloat());
      default:
        throw new InterpreterException("Cannot add "+a.type);
    }
  }

  public static Value subtract(Value a, Value b) throws InterpreterException {
    if(a.type == Type.INT && b.type == Type.FLOAT) {
      a = new Value(a.getFloat());
    }
    else if(a.type == Type.FLOAT && b.type == Type.INT) {
      b = new Value(b.getFloat());
    }
    if(a.type != b.type)
      throw new InterpreterException("Cannot subtract "+a.type+" and "+b.type);
    switch(a.type) {
      case INT:
        return new Value(a.getInt()-b.getInt());
      case FLOAT:
        return new Value(a.getFloat()-b.getFloat());
      default:
        throw new InterpreterException("Cannot subtract "+a.type);
    }
  }

  public static Value multiply(Value a, Value b) throws InterpreterException {
    if(a.type == Type.INT && b.type == Type.FLOAT) {
      a = new Value(a.getFloat());
    }
    else if(a.type == Type.FLOAT && b.type == Type.INT) {
      b = new Value(b.getFloat());
    }
    if(a.type != b.type)
      throw new InterpreterException("Cannot multiply "+a.type+" and "+b.type);
    switch(a.type) {
      case INT:
        return new Value(a.getInt()*b.getInt());
      case FLOAT:
        return new Value(a.getFloat()*b.getFloat());
      default:
        throw new InterpreterException("Cannot multiply "+a.type);
    }
  }

  public static Value divide(Value a, Value b) throws InterpreterException {
    if(a.type == Type.INT && b.type == Type.FLOAT) {
      a = new Value(a.getFloat());
    }
    else if(a.type == Type.FLOAT && b.type == Type.INT) {
      b = new Value(b.getFloat());
    }
    if(a.type != b.type)
      throw new InterpreterException("Cannot divide "+a.type+" and "+b.type);
    switch(a.type) {
      case INT:
        return new Value(a.getInt()*b.getInt());
      case FLOAT:
        return new Value(a.getFloat()*b.getFloat());
      default:
        throw new InterpreterException("Cannot divide "+a.type);
    }
  }

  public static Value modulo(Value a, Value b) throws InterpreterException {
    if(a.type == Type.INT && b.type == Type.FLOAT) {
      a = new Value(a.getFloat());
    }
    else if(a.type == Type.FLOAT && b.type == Type.INT) {
      b = new Value(b.getFloat());
    }
    if(a.type != b.type)
      throw new InterpreterException("Cannot modulo "+a.type+" and "+b.type);
    switch(a.type) {
      case INT:
        return new Value(a.getInt()%b.getInt());
      case FLOAT:
        return new Value(a.getFloat()%b.getFloat());
      default:
        throw new InterpreterException("Cannot modulo "+a.type);
    }
  }

  public static Value cat(Value a, Value b) throws InterpreterException {
    throw new InterpreterException("Unsupported operation");
  }
}
