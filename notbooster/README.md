# NotBooster

NotBooster is a standalone Paper plugin project that hooks into NotKillRank through the public `notkillrank-api` dependency.

This project intentionally is **not** part of the NotKillRank Maven reactor. Build it from this directory:

```bash
mvn -f notbooster/pom.xml package
```

Implemented phases:

- Standalone plugin bootstrap with NotKillRank API availability checks.
- `/notbooster <type> <personal/global> <power> <duration>` command plus `list`, `stop`, and `reload` helpers.
- Persistent YAML booster storage in `boosters.yml`.
- Elo boosters through `NKREloChangeEvent`.
- Vanilla EXP boosters through `PlayerExpChangeEvent`.
- CataMines item-gained boosters using the public `CataMineBlockBreakEvent` detected reflectively at runtime, then boosting Bukkit `BlockDropItemEvent` drops for confirmed CataMines breaks.
- Effect boosters using configurable potion-effect presets.


Code organization follows NotKillRank patterns: static file loaders under `file`, singleton runtime managers under `manager`, self-registering commands/listeners, and a slim `NotBooster` bootstrap that wires files, settings, managers, hooks, listeners, commands, and tasks.


## NotKillRank API dependency

The NotKillRank API is resolved from JitPack as `com.github.ihqqq.NotKillRank:notkillrank-api:${notkillrank.version}`. If you are developing without internet access or against a private/local NotKillRank build, install the API to your local Maven repository from the NotKillRank project first:

```bash
mvn -pl notkillrank-api -am install -DskipTests
```

Then override the dependency in `pom.xml` to your locally installed coordinates if needed.

Lombok is used for small immutable model boilerplate such as `Booster` getters/constructor; enable annotation processing in your IDE if needed.
