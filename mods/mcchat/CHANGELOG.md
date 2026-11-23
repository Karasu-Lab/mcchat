# Changelog

## [1.4.0](https://github.com/Karasu-Lab/mcchat/compare/mcchat-v1.3.0...mcchat-v1.4.0) (2026-11-23)


### Features

* add a Mod Menu config screen to toggle the TCP servers ([7f082d2](https://github.com/Karasu-Lab/mcchat/commit/7f082d2bed75f62208f6c261694513360693fc29))
* add reply and mention chat text components with @ completion ([dcf6373](https://github.com/Karasu-Lab/mcchat/commit/dcf637302fc67591fa477e74d71ff9f153361bd2))
* disallow replies to messages from a different world ([beead15](https://github.com/Karasu-Lab/mcchat/commit/beead153a31dd1b76db73c520921a074682d62f8))
* draw chat bubbles, avatars and icons with a core shader ([b5cac3f](https://github.com/Karasu-Lab/mcchat/commit/b5cac3fe23638ee0880a38e80013d6b5f090f92a))
* port the screen state, world icon and event class design to main ([7e8f666](https://github.com/Karasu-Lab/mcchat/commit/7e8f66623ddf4a8a60efe1f714778355bba53f7c))
* replace the Minecraft chat list with the app-style UI ([85f6cdc](https://github.com/Karasu-Lab/mcchat/commit/85f6cdcf42d4649bbfa6744ae3533ca4f9f13ebe))
* report the current screen and world icon to the app ([8c261b6](https://github.com/Karasu-Lab/mcchat/commit/8c261b61562afa6006bfde0f0ee22d66ec5877fd))


### Bug Fixes

* clear the chat log with the vanilla history and start TCP on load ([055f0b9](https://github.com/Karasu-Lab/mcchat/commit/055f0b92ec3133693d389100aaad8f26e19e69e6))
* draw the typing indicator from an SVG and stop capturing idle input ([edf484c](https://github.com/Karasu-Lab/mcchat/commit/edf484cb8d2cdf3bfd631056ff10c00e3bcad0b2))
* highlight mentions from the structured chat text ([8c8caec](https://github.com/Karasu-Lab/mcchat/commit/8c8caeca1c795e35cacab1079d175c2c2f2ec66a))
* keep reply data and hover tooltips working on Minecraft 26.3 ([1c67fb7](https://github.com/Karasu-Lab/mcchat/commit/1c67fb7805d2b5bbab547cf0fa23955cf28fefd6))
* keep the reply row above the input as a plain rectangle ([fc5a175](https://github.com/Karasu-Lab/mcchat/commit/fc5a175bf4647b80fdf633d9ef4361a14283e37d))
* keep the styled text component sent by the server ([37a4ba6](https://github.com/Karasu-Lab/mcchat/commit/37a4ba67ab5420a40a603884f5557cb23e653488))
* show the vanilla system tooltip and scroll over the whole screen ([19e4997](https://github.com/Karasu-Lab/mcchat/commit/19e4997da170f71db38b7f44e0716455f137ad11))
* use the mentions sent by the server as they are ([23ae250](https://github.com/Karasu-Lab/mcchat/commit/23ae2502d7dde9e4fe94ef1e81bc00643cef9ae2))

## [1.3.0](https://github.com/Karasu-Lab/mcchat/compare/mcchat-v1.2.0...mcchat-v1.3.0) (2026-11-23)


### Features

* add common protocol library and connect the app to the mod ([9b7b6a8](https://github.com/Karasu-Lab/mcchat/commit/9b7b6a8d214907de86e71a57162a49149c6f9365))

## [1.2.0](https://github.com/Karasu-Lab/mcchat/compare/mcchat-v1.1.0...mcchat-v1.2.0) (2026-11-23)


### Features

* capture chat and commands and sync them over TCP ([e0143b8](https://github.com/Karasu-Lab/mcchat/commit/e0143b8a2755b4c3512f596cc311c6753d247924))

## [1.1.0](https://github.com/Karasu-Lab/mcchat/compare/mcchat-v1.0.0...mcchat-v1.1.0) (2026-11-23)


### Features

* capture chat and commands and sync them over TCP ([e0143b8](https://github.com/Karasu-Lab/mcchat/commit/e0143b8a2755b4c3512f596cc311c6753d247924))
