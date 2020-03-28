# ForgeWrapper

Allow [MultiMC](https://github.com/MultiMC/MultiMC5) to launch Minecraft 1.13+ with Forge.

**ForgeWrapper has been adopted by MultiMC, you do not need to perform the following steps manually. (2020-03-29)**

## How to use (Outdated)

1. Download Forge installer for Minecraft 1.13+ [here](https://files.minecraftforge.net/).
2. Download ForgeWrapper jar file at the [release](https://github.com/ZekerZhayard/ForgeWrapper/releases) page.
3. Run the below command in terminal:
   ```
   java -jar <ForgeWrapper.jar> --installer=<forge-installer.jar> [--instance=<instance-path>]
   ```
   *Notice: If you don't specify a MultiMC instance path, ForgeWrapper will create the instance folder in current working space.*

4. If the instance folder which just created is not in `MultiMC/instances` folder, you just need to move to the `MultiMC/instances` folder.
5. Run MultiMC, and you will see a new instance named `forge-<mcVersion>-<forgeVersion>`.