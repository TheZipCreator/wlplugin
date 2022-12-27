package net.mcwarlords.wlplugin.chat;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import net.mcwarlords.wlplugin.*;

public class ChatCommand implements CommandExecutor {
  private void sendHelpMessage(Player p) {
    p.sendMessage(Utils.escapeText("&_s======[ &_eWLCHAT &_s]======"));
    p.sendMessage(Utils.escapeText("&_p/wlchat h | help &_s- &_dShows this help message."));
    p.sendMessage(Utils.escapeText("&_p/wlchat j | join &_e<channel> [pass] &_s- &_dJoins channel &_e<channel>&_d. &_e[pass] &_dis neccesary if the channel is locked."));
    p.sendMessage(Utils.escapeText("&_p/wlchat x | exit &_s- &_dExits your current channel and joins global."));
    p.sendMessage(Utils.escapeText("&_p/wlchat p | prefix &_e[prefix] &_s- &_dSets a prefix to appear before every message you send in chat (typically used for color codes). If no prefix is specified, it clears your current prefix."));
    p.sendMessage(Utils.escapeText("&_p/wlchat n | nick &_e[nick] &_s- &_dSets your nickname to &_e[nick] &_dif specified, otherwise clears it."));
    p.sendMessage(Utils.escapeText("&_p/wlchat r | realname &_e<nick> &_s- &_dTells you which online user has a given nick."));
    p.sendMessage(Utils.escapeText("&_p/wlchat l | lock &_e<password> &_s- &_dLocks a channel with a given password. You must be the only one in the channel to do this."));
    p.sendMessage(Utils.escapeText("&_p/wlchat hg | hideglobal &_s- &_dHides the global chat."));
    p.sendMessage(Utils.escapeText("&_p/wlchat sg | hideglobal &_s- &_dShows the global chat."));
    p.sendMessage(Utils.escapeText("&_p/wlchat i | ignore &_s- &_dIgnores a player. If they're already ignored, it unignores them."));
    p.sendMessage(Utils.escapeText("&_p/wlchat il | ignorelist &_s- &_dDisplays all ignored players."));
  }

  static final String invalidArguments = Utils.escapeText("&_p* &_eInvalid arguments.");

  void changeChannel(PlayerData pd, String newChannel) {
    if(Data.lockedChannels.containsKey(pd.channel)) {
      if(Utils.channelPlayerCount(pd.channel) == 1)
        Data.lockedChannels.remove(pd.channel);
    }
    pd.channel = newChannel;
  }

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if(!(sender instanceof Player)) {
      sender.sendMessage("wlchat can only be used by a player");
      return false;
    }
    Player p = (Player)sender;
    if(args.length == 0) {
      sendHelpMessage(p);
      return true;
    }
    PlayerData pd = Data.getPlayerData(p);
    switch(args[0]) {
      case "help":
      case "h":
        sendHelpMessage(p);
        break;
      case "join": 
      case "j": {
        if(args.length != 2 && args.length != 3) {
          p.sendMessage(invalidArguments);
          return true;
        }
        String channel = args[1];
        if(Data.lockedChannels.containsKey(channel)) {
          if(args.length != 3) {
            p.sendMessage(Utils.escapeText("&_p* Channel &_e"+channel+" is locked. Please enter a password like so: &_p/wlchat join "+channel+" <password>"));
            return true;
          }
          if(!args[2].equals(Data.lockedChannels.get(channel))) {
            p.sendMessage(Utils.escapeText("&_p* &_eIncorrect password."));
            return true;
          }
        }
        p.sendMessage(Utils.escapeText("&_p* &_dJoined channel &_e"+channel));
        changeChannel(pd, channel);
      }
        break;
      case "exit":
      case "x":
        if(args.length != 1) {
          p.sendMessage(invalidArguments);
          return true;
        }
        p.sendMessage(Utils.escapeText("&_p* &_dLeft channel &_e"+pd.channel+"."));
        changeChannel(pd, "global");
        break;
      case "prefix":
      case "p":
        if(args.length == 2) {
          p.sendMessage(Utils.escapeText("&_p* &_dPrefix set to ")+args[1]+".");
          pd.prefix = args[1];
          break;
        }
        if(args.length != 1) {
          p.sendMessage(invalidArguments);
          return true;
        }
        p.sendMessage(Utils.escapeText("&_p* &_dPrefix cleared."));
        pd.prefix = "";
        break;
      case "nick":
      case "n":
        if(args.length == 2) {
          p.sendMessage(Utils.escapeText("&_p* &_dChanged nickname to "+args[1]+"&_d."));
          p.setDisplayName(Utils.escapeText(args[1]));
          pd.nick = args[1];
          break;
        }
        if(args.length != 1) {
          p.sendMessage(invalidArguments);
          return true;
        }
        p.sendMessage(Utils.escapeText("&_p* &_dCleared nickname."));
        pd.nick = null;
        break;
      case "realname": 
      case "r": {
        if(args.length != 2) {
          p.sendMessage(invalidArguments);
          return true;
        }
        String nick = args[1];
        ArrayList<String> users = new ArrayList<String>();
        for(Player plyr : Bukkit.getOnlinePlayers()) {
          if(Data.getPlayerData(plyr).nick.equals(nick))
            users.add(plyr.getName());
        }
        if(users.size() == 0) {
          p.sendMessage(Utils.escapeText("&_p* &_eCould not find any players with nickname "+nick+"&_e."));
        } else {
          p.sendMessage(Utils.escapeText("&_p* &_d"+String.join(", ", users)+"&_d is ~"+nick+"&_d."));
        }
        break;
      }
      case "lock":
      case "l":
        if(args.length != 2) {
          p.sendMessage(invalidArguments);
          return true;
        }
        if(Utils.channelPlayerCount(pd.channel) > 1) {
          p.sendMessage(Utils.escapeText("&_p* &_eCannot lock a channel while other people are in it."));
          return true;
        }
        if(pd.channel.equals("global")) {
          p.sendMessage(Utils.escapeText("&_p* &_eCannot lock channel "+pd.channel));
          return true;
        }
        p.sendMessage(Utils.escapeText("&_p* &_dSuccessfully locked channel &_e"+pd.channel+" &_dwith password &_e"+args[1]+"&_d."));
        Data.lockedChannels.put(pd.channel, args[1]);
        break;
      case "hideglobal":
      case "hg":
        if(args.length != 1) {
          p.sendMessage(invalidArguments);
          return true;
        }
        p.sendMessage(Utils.escapeText("&_p* &_dGlobal chat is now hidden."));
        pd.hideGlobal = true;
        break;
      case "showglobal":
      case "sg":
        if(args.length != 1) {
          p.sendMessage(invalidArguments);
          return true;
        }
        p.sendMessage(Utils.escapeText("&_p* &_dGlobal chat is now shown."));
        pd.hideGlobal = false;
        break;
      case "ignore":
      case "i": {
        if(args.length != 2) {
          p.sendMessage(invalidArguments);
          return true;
        }
        String name = args[1];
        if(!Data.playerExists(name)) {
          p.sendMessage(Utils.escapeText("&_p* &_eUnknown player "+name+"."));
          return true;
        }
        String uuid = Data.uuidOf(args[1]);
        if(pd.ignored.contains(uuid)) {
          pd.ignored.remove(uuid);
          p.sendMessage(Utils.escapeText("&_p* &_dUnignored player "+name+"."));
          break;
        }
        pd.ignored.add(uuid);
        p.sendMessage(Utils.escapeText("&_p* &_dIgnored player "+name+"."));
        break;
      }
      case "ignorelist":
      case "il": {
        if(args.length != 1) {
          p.sendMessage(invalidArguments);
          return true;
        }
        String ignored = "";
        for(String uuid : pd.ignored)
          ignored += Data.nameOf(uuid)+" ";
        p.sendMessage(Utils.escapeText("&_p* &_dList of all ignored players:\n&_d"+ignored));
        break;
      }
      default:
        p.sendMessage(Utils.escapeText("&_p* &_eUnknown subcommand: "+args[0]));
        break;
    }
    return true;
  }
}
