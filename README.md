# ForgeWrapper

Allow [MultiMC](https://github.com/MultiMC/MultiMC5) to launch Minecraft 1.13+ with Forge.

**ForgeWrapper has been adopted by MultiMC, you do not need to perform the following steps manually. (2020-03-29)**

## For other launchers
1. ForgeWrapper provides some java properties since 1.4.2:
   - `forgewrapper.librariesDir` : a path to libraries folder (e.g. -Dforgewrapper.librariesDir=/home/xxx/.minecraft/libraries)
   - `forgewrapper.installer` : a path to forge installer (e.g. -Dforgewrapper.installer=/home/xxx/forge-1.14.4-28.2.0-installer.jar)
   - `forgewrapper.minecraft` : a path to the vanilla minecraft jar (e.g. -Dforgewrapper.minecraft=/home/xxx/.minecraft/versions/1.14.4/1.14.4.jar)

2. ForgeWrapper also provides an interface [`IFileDetector`](https://github.com/ZekerZhayard/ForgeWrapper/blob/master/src/main/java/io/github/zekerzhayard/forgewrapper/installer/detector/IFileDetector.java), you can implement it and custom your own detecting rules. To load it, you should make another jar which contains `META-INF/services/io.github.zekerzhayard.forgewrapper.installer.detector.IFileDetector` within the full implementation class name and add the jar to class path.

## How to use (Outdated)

1. Download Forge installer for Minecraft 1.13+ [here](https://files.minecraftforge.net/).
2. Download ForgeWrapper jar file at the [release](https://github.com/ZekerZhayard/ForgeWrapper/releases) page.
3. Since ForgeWrapper 1.5.1, it no longer includes the json converter, so you need to build it by yourself:
   - [Download](https://github.com/ZekerZhayard/ForgeWrapper/archive/refs/heads/master.zip) ForgeWrapper sources.
   - Extract the zip and open terminal in the extracted folder.
   - Run `./gradlew build` command in terminal and get the jar from `./converter/build/libs`
3. Run the below command in terminal:
   ```
   java -jar <ForgeWrapper.jar> --installer=<forge-installer.jar> [--instance=<instance-path>]
   ```
   *Notice: If you don't specify a MultiMC instance path, ForgeWrapper will create the instance folder in current working space.*

4. If the instance folder which just created is not in `MultiMC/instances` folder, you just need to move to the `MultiMC/instances` folder.
5. Run MultiMC, and you will see a new instance named `forge-<mcVersion>-<forgeVersion>`.