package net.mcwarlords.wlplugin.code;

@FunctionalInterface
public interface ExternalCodeFunction extends CodeFunction {
  public Value apply(Value[] args) throws InterpreterException;
}