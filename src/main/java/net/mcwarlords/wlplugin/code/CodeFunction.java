package net.mcwarlords.wlplugin.code;

public interface CodeFunction {
  public Value apply(Value[] args) throws InterpreterException;
}
