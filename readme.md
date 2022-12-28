# wlplugin
This is a plugin for the Warlords minecraft server, a private server for me and my online friends. It contains a few modules which do various helpful things that we'd previously use other plugins for.

# licenses
`LICENSE` (GNU GPLv3) is for the main code

`LICENSE-lite2edit` (MIT) is for anything under src/main/java/goldendelicios

# changelog

## 1.0.1
- Fixed a bug where whenever the player that owns a plot is online, going to that plot causes the "Current Plot" action bar message to disappear for all players.
## 1.1.0
- Added `/wlchat showglobal` and `/wlchat hideglobal`
- Added `/players` to the discord bot
- Fixed a bug where not having the discord key will cause errors after launch.
- Set wlbot's activity to display the number of players online.
## 1.2.0
- Changed join messages to use `&e`
- Changed the discord activity message from "Playing X players online" to "Playing: X/100"
- Added discord command `/panda`
- Added `/wlchat ignore`, which allows you to ignore/unignore players
- Allowed an extra argument for `/wlplot wild`, which allows you to specify a player who's plots to go to a random position to.
- Added `/wlplot center <id>` which lets you teleport to the center of a plot with an id.
- Added wlmisc module, with `/wlmisc roll`
## 1.2.1
- Fixed a bug where the bot would repeat messages sent in #chat-mirror
## 1.3.0
- Added a limit to how many faces & amount of dice you can roll
- Fixed a bug where some color codes created buggy symbols in #chat-mirror
- Fixed a bug where if you put a backslash at the end of a line it would add an extra NUL character
- Added `/wlchat discordignore` and `/wlchat discordignorelist`